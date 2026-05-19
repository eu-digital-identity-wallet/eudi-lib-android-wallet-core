/*
 * Copyright (c) 2023-2025 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Utility functions and helpers for OpenID4VP (OpenID for Verifiable Presentations) flows.
 *
 * This file provides methods for generating session transcripts, handling cryptographic operations,
 * converting between OpenID4VP and SIOP configurations, and constructing verifiable presentations
 * for both SD-JWT VC and MSO mdoc credential formats. It also includes helpers for algorithm
 * conversions and key binding JWT serialization.
 *
 * Functions in this file are intended for internal use within the wallet-core module.
 */

package eu.europa.ec.eudi.wallet.internal

import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWSAlgorithm
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.iso18013.transfer.SessionTranscriptBytes
import eu.europa.ec.eudi.iso18013.transfer.internal.DocumentResponseGenerator
import eu.europa.ec.eudi.openid4vp.CoseAlgorithm
import eu.europa.ec.eudi.openid4vp.JarConfiguration
import eu.europa.ec.eudi.openid4vp.JwkSetSource.ByReference
import eu.europa.ec.eudi.openid4vp.OpenId4VPConfig
import eu.europa.ec.eudi.openid4vp.PreregisteredClient
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseEncryptionConfiguration
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.openid4vp.SupportedClientIdPrefix
import eu.europa.ec.eudi.openid4vp.VPConfiguration
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VpFormatsSupported
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionAlgorithm
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionMethod
import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderTrust
import kotlinx.coroutines.withContext
import kotlinx.io.bytestring.decodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.mdoc.response.DeviceResponseGenerator
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.presentment.PresentmentUnlockReason
import org.multipaz.request.JsonRequestedClaim
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.sdjwt.SdJwt
import org.multipaz.sdjwt.credential.SdJwtVcCredential
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.util.Constants
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

private const val SHA_256_ALGORITHM = "SHA-256"

/**
 *  Utility to generate the session transcript for the OpenID4VP protocol.
 *
 *  SessionTranscript = [
 *    DeviceEngagementBytes,
 *    EReaderKeyBytes,
 *    Handover
 *  ]
 *
 *  DeviceEngagementBytes = null,
 *  EReaderKeyBytes = null
 *
 *  Handover = OID4VPHandover
 *
 *  OpenID4VPHandover = [
 *    "OpenID4VPHandover",      ; A fixed identifier for this handover type
 *    OpenID4VPHandoverInfoHash ; A cryptographic hash of OpenID4VPHandoverInfo
 *  ]
 *
 *  ; Contains the sha-256 hash of OpenID4VPHandoverInfoBytes
 *  OpenID4VPHandoverInfoHash = bstr
 *
 *  ; Contains the bytes of OpenID4VPHandoverInfo encoded as CBOR
 *  OpenID4VPHandoverInfoBytes = bstr .cbor OpenID4VPHandoverInfo
 *
 *  OpenID4VPHandoverInfo = [
 *    clientId,
 *    nonce,
 *    jwkThumbprint,
 *    responseUri
 *  ] ; Array containing handover parameters
 *
 *  clientId = tstr
 *  nonce = tstr
 *  jwkThumbprint = bstr
 *  responseUri = tstr
 */
internal fun generateSessionTranscript(
    clientId: String,
    nonce: String,
    jwkThumbprint: ByteArray?,
    responseOrRedirectUri: String
): SessionTranscriptBytes {

    val openID4VPHandover =
        generateOpenId4VpHandover(clientId, nonce, jwkThumbprint, responseOrRedirectUri)

    val sessionTranscriptBytes =
        CBORObject.NewArray().apply {
            Add(CBORObject.Null)
            Add(CBORObject.Null)
            Add(openID4VPHandover)
        }.EncodeToBytes()

    return sessionTranscriptBytes
}

/**
 * Generates the OpenID4VP handover CBOR object
 *
 * @param clientId The client identifier.
 * @param nonce The nonce value.
 * @param jwkThumbprint The JWK thumbprint as a byte array.
 * @param responseOrRedirectUri The response URI or redirect URI.
 * @return The CBOR object representing the OpenID4VP handover.
 */
internal fun generateOpenId4VpHandover(
    clientId: String,
    nonce: String,
    jwkThumbprint: ByteArray?,
    responseOrRedirectUri: String,
): CBORObject {

    val openID4VPHandoverInfoBytes = CBORObject.NewArray().apply {
        Add(clientId)
        Add(nonce)
        Add(jwkThumbprint ?: CBORObject.Null)
        Add(responseOrRedirectUri)
    }.EncodeToBytes()

    val openID4VPHandoverInfoHash = MessageDigest.getInstance(SHA_256_ALGORITHM)
        .digest(openID4VPHandoverInfoBytes)

    val openID4VPHandover = CBORObject.NewArray().apply {
        Add("OpenID4VPHandover")
        Add(openID4VPHandoverInfoHash)
    }

    return openID4VPHandover
}

/**
 * Generates a random nonce for a generic JARM (JWT Secured Authorization Response Mode) using a secure random generator.
 *
 * @return A URL-safe base64 encoded nonce string.
 */
internal fun generateJarmNonce(): String {
    val secureRandom = SecureRandom()
    val bytes = ByteArray(16)
    secureRandom.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

internal fun makeOpenId4VPConfig(
    config: OpenId4VpConfig,
    trust: OpenId4VpReaderTrust,
): OpenId4VPConfig {
    val supportedClientIdPrefixes = config.clientIdSchemes.map { clientIdScheme ->
        when (clientIdScheme) {
            is ClientIdScheme.Preregistered -> SupportedClientIdPrefix.Preregistered(
                clientIdScheme.preregisteredVerifiers.associate { verifier ->
                    verifier.clientId to PreregisteredClient(
                        clientId = verifier.clientId,
                        legalName = verifier.legalName,
                        jarConfig = JWSAlgorithm.parse(verifier.jwsAlgorithm.joseAlgorithmIdentifier) to ByReference(
                            verifier.jwkSetSource
                        )

                    )
                }
            )

            ClientIdScheme.RedirectUri -> SupportedClientIdPrefix.RedirectUri
            ClientIdScheme.X509SanDns -> SupportedClientIdPrefix.X509SanDns(trust = trust)
            ClientIdScheme.X509Hash -> SupportedClientIdPrefix.X509Hash(trust = trust)
        }
    }
    return OpenId4VPConfig(
        issuer = OpenId4VPConfig.SelfIssued,
        jarConfiguration = JarConfiguration.Default,
        responseEncryptionConfiguration = ResponseEncryptionConfiguration.Supported(
            supportedAlgorithms = config.encryptionAlgorithms.map { it.nimbus },
            supportedMethods = config.encryptionMethods.map { it.nimbus }
        ),
        vpConfiguration = VPConfiguration(
            vpFormatsSupported = config.formats.toVpFormats()
        ),
        supportedClientIdPrefixes = supportedClientIdPrefixes
    )
}

/**
 * Extension function to get the session transcript bytes from a resolved OpenID4VP authorization request.
 *
 * @return The session transcript as a byte array.
 */
internal fun ResolvedRequestObject.getSessionTranscriptBytes(): SessionTranscriptBytes {
    val clientId = client.id.clientId
    val nonce = nonce
    val jwkThumbprint = responseEncryptionSpecification?.recipientKey?.computeThumbprint()?.decode()
    val responseOrRedirectUri = when (val mode = this.responseMode) {
        is ResponseMode.DirectPostJwt -> mode.responseURI.toString()
        is ResponseMode.DirectPost -> mode.responseURI.toString()
        is ResponseMode.Fragment -> mode.redirectUri.toString()
        is ResponseMode.FragmentJwt -> mode.redirectUri.toString()
        is ResponseMode.Query -> mode.redirectUri.toString()
        is ResponseMode.QueryJwt -> mode.redirectUri.toString()
    }
    val sessionTranscriptBytes = generateSessionTranscript(
        clientId = clientId,
        nonce = nonce,
        jwkThumbprint = jwkThumbprint,
        responseOrRedirectUri = responseOrRedirectUri
    )
    return sessionTranscriptBytes
}

/**
 * Converts a list of [Format]s to [VpFormats] for use in VP configuration.
 *
 * @receiver List of credential formats.
 * @return The corresponding [VpFormats] object.
 */
internal fun List<Format>.toVpFormats(): VpFormatsSupported {

    val msoMdocVpFormat = filterIsInstance<Format.MsoMdoc>()
        .firstOrNull()
        ?.let { spec ->
            VpFormatsSupported.MsoMdoc(
                issuerAuthAlgorithms = spec.issuerAuthAlgorithms.map { CoseAlgorithm(it.coseAlgorithmIdentifier!!) },
                deviceAuthAlgorithms = spec.deviceAuthAlgorithms.map { CoseAlgorithm(it.coseAlgorithmIdentifier!!) }
            )
        }


    val sdJwtVcVpFormat = filterIsInstance<Format.SdJwtVc>()
        .firstOrNull()
        ?.let { spec ->
            VpFormatsSupported.SdJwtVc(
                sdJwtAlgorithms = spec.sdJwtAlgorithms.map { JWSAlgorithm.parse(it.joseAlgorithmIdentifier!!) },
                kbJwtAlgorithms = spec.kbJwtAlgorithms.map { JWSAlgorithm.parse(it.joseAlgorithmIdentifier!!) }
            )
        }

    return VpFormatsSupported(
        sdJwtVc = sdJwtVcVpFormat,
        msoMdoc = msoMdocVpFormat
    )
}

/**
 * Extension property to convert an [EncryptionAlgorithm] to Nimbus [JWEAlgorithm].
 */
internal val EncryptionAlgorithm.nimbus: JWEAlgorithm
    get() = when (this) {
        EncryptionAlgorithm.ECDH_ES -> JWEAlgorithm.ECDH_ES
        EncryptionAlgorithm.ECDH_ES_A128KW -> JWEAlgorithm.ECDH_ES_A128KW
        EncryptionAlgorithm.ECDH_ES_A192KW -> JWEAlgorithm.ECDH_ES_A192KW
        EncryptionAlgorithm.ECDH_ES_A256KW -> JWEAlgorithm.ECDH_ES_A256KW
    }

internal val EncryptionMethod.nimbus: com.nimbusds.jose.EncryptionMethod
    get() = when (this) {
        EncryptionMethod.A128CBC_HS256 -> com.nimbusds.jose.EncryptionMethod.A128CBC_HS256
        EncryptionMethod.A192CBC_HS384 -> com.nimbusds.jose.EncryptionMethod.A192CBC_HS384
        EncryptionMethod.A256CBC_HS512 -> com.nimbusds.jose.EncryptionMethod.A256CBC_HS512
        EncryptionMethod.A128GCM -> com.nimbusds.jose.EncryptionMethod.A128GCM
        EncryptionMethod.A192GCM -> com.nimbusds.jose.EncryptionMethod.A192GCM
        EncryptionMethod.A256GCM -> com.nimbusds.jose.EncryptionMethod.A256GCM
        EncryptionMethod.A128CBC_HS256_DEPRECATED -> com.nimbusds.jose.EncryptionMethod.A128CBC_HS256_DEPRECATED
        EncryptionMethod.A256CBC_HS512_DEPRECATED -> com.nimbusds.jose.EncryptionMethod.A256CBC_HS512_DEPRECATED
        EncryptionMethod.XC20P -> com.nimbusds.jose.EncryptionMethod.XC20P
    }

/**
 * Builds a verifiable presentation for an SD-JWT VC credential from a single match.
 *
 * Each request path from [JsonRequestedClaim.claimPath] is matched against the SD-JWT
 * disclosure paths through [pathMatches], honouring OpenID4VP §7.1 semantics for
 * string keys, integer array indices, and `null` wildcards. Parent disclosures
 * required to reconstruct a selected child are pulled in automatically.
 *
 * Key Binding JWT is emitted whenever the issuer-signed JWT carries a `cnf` claim,
 * regardless of the verifier's `require_cryptographic_holder_binding` flag.
 * Verifiers that did not request the proof can ignore it.
 * The flag is honoured at match time in [DcqlRequestProcessor], which
 * filters out credentials lacking `cnf` when the verifier requires holder binding —
 * so the bare-SD-JWT branch below runs only when the verifier explicitly accepts
 * presentations without holder binding.
 *
 * Empty `match.claims` corresponds to OpenID4VP §6.4.1 "claims=null" semantics
 * (mandatory disclosure only).
 */
internal suspend fun verifiablePresentationForSdJwtVc(
    resolvedRequestObject: ResolvedRequestObject,
    match: CredentialPresentmentSetOptionMemberMatch,
    documentManager: DocumentManager,
    keyUnlockData: KeyUnlockData?
): VerifiablePresentation.Generic {
    val document = match.credential.requireIssuedDocument(documentManager)
    return document.consumingCredential {
        val sdJwtVcCredential = this as? SdJwtVcCredential
            ?: throw IllegalStateException(
                "Credential ${this.identifier} is not an SD-JWT VC credential"
            )
        val sdJwt = SdJwt.fromCompactSerialization(
            sdJwtVcCredential.issuerProvidedData.decodeToString()
        )

        val requestPaths = match.claims.keys
            .filterIsInstance<JsonRequestedClaim>()
            .map { it.claimPath }

        val filteredSdJwt = sdJwt.filter { emittedPath, _ ->
            requestPaths.any { requestPath -> pathMatches(requestPath, emittedPath) }
        }

        val serialized = if (filteredSdJwt.kbKey != null) {
            val signingKey = AsymmetricKey.anonymous(
                secureArea = this.secureArea,
                alias = this.alias,
                unlockReason = PresentmentUnlockReason(this)
            )
            withContext(keyUnlockData.asProvider()) {
                filteredSdJwt.present(
                    signingKey = signingKey,
                    nonce = resolvedRequestObject.nonce,
                    audience = resolvedRequestObject.client.id.clientId
                )
            }.compactSerialization
        } else {
            filteredSdJwt.compactSerialization
        }

        VerifiablePresentation.Generic(serialized)
    }.getOrThrow()
}

/**
 * Builds a verifiable presentation for an MSO mdoc credential from a single
 * [CredentialPresentmentSetOptionMemberMatch].
 *
 * Produces a single-document `DeviceResponse` (status OK) bound to [sessionTranscript],
 * encoded as base64url for transport in OpenID4VP `vp_token`. Disclosed elements are
 * taken from the match's [MdocRequestedClaim] keys, grouped by namespace; an empty claim
 * set yields a minimal response.
 */
internal suspend fun verifiablePresentationForMsoMdoc(
    match: CredentialPresentmentSetOptionMemberMatch,
    documentManager: DocumentManager,
    sessionTranscript: ByteArray,
    keyUnlockData: KeyUnlockData?
): VerifiablePresentation.Generic {
    val document = match.credential.requireIssuedDocument(documentManager)

    val elements: Map<String, List<String>> = match.claims.keys
        .filterIsInstance<MdocRequestedClaim>()
        .groupBy { it.namespaceName }
        .mapValues { (_, claims) -> claims.map { it.dataElementName } }

    val encodedDocument = DocumentResponseGenerator.generate(
        document = document,
        transcript = sessionTranscript,
        elements = elements,
        keyUnlockData = keyUnlockData
    )
    val deviceResponseBytes = DeviceResponseGenerator(Constants.DEVICE_RESPONSE_STATUS_OK)
        .addDocument(encodedDocument)
        .generate()

    return VerifiablePresentation.Generic(
        value = Base64.getUrlEncoder().withoutPadding().encodeToString(deviceResponseBytes)
    )
}

/**
 * Matches a DCQL claim path against an SD-JWT disclosure path emitted during the
 * filter pass. Honours OpenID4VP §7.1:
 *
 *  - String key matches the same string key.
 *  - Integer index matches the same integer index.
 *  - [JsonNull] wildcard matches any integer index.
 *
 * Returns true iff every position the two paths share matches element-by-element.
 * Emitted paths are accepted in three positions relative to the request:
 *
 *  - Equal → exact match.
 *  - Longer than the request → descendant disclosure; releases every field of
 *    the targeted node (request `["addresses", 0]` also releases
 *    `["addresses", 0, "city"]`).
 *  - Shorter than the request → ancestor disclosure; required because SD-JWT
 *    array-element disclosures cannot be reconstructed by the verifier without
 *    their parent-array disclosure. Ancestor envelopes carry only child digests
 *    and no extra data beyond what the request already authorises.
 */
private fun pathMatches(request: JsonArray, emittedPath: JsonArray): Boolean {
    val requestElements = request.toList()
    val emittedElements = emittedPath.toList()
    val common = minOf(requestElements.size, emittedElements.size)
    for (i in 0 until common) {
        if (!elementMatches(requestElements[i], emittedElements[i])) return false
    }
    return true
}

private fun elementMatches(request: JsonElement, emitted: JsonElement): Boolean =
    when (request) {
        is JsonNull -> emitted is JsonPrimitive && !emitted.isString
        is JsonPrimitive if request.isString ->
            emitted is JsonPrimitive && emitted.isString && request.content == emitted.content

        is JsonPrimitive if !request.isString ->
            emitted is JsonPrimitive && !emitted.isString && request.content == emitted.content

        else -> false
    }