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
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.AsymmetricJWK
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.iso18013.transfer.SessionTranscriptBytes
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
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
import eu.europa.ec.eudi.openid4vp.VerifierId
import eu.europa.ec.eudi.openid4vp.VpFormatsSupported
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.present
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.serialize
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.serializeWithKeyBinding
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.NimbusSdJwtOps
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import eu.europa.ec.eudi.sdjwt.vc.ClaimPathElement
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.credential.CredentialIssuedData
import eu.europa.ec.eudi.wallet.document.credential.getIssuedData
import eu.europa.ec.eudi.wallet.issue.openid4vci.toJoseEncoded
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionAlgorithm
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionMethod
import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderTrust
import eu.europa.ec.eudi.wallet.transfer.openId4vp.SdJwtVcItem
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.UnlockReason
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Date

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
 * Generates a random nonce for mdoc using a secure random generator.
 *
 * @return A URL-safe base64 encoded nonce string.
 */
internal fun generateMdocGeneratedNonce(): String {
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
 * Serializes an SD-JWT with key binding using the provided credential and signing information.
 *
 * @receiver The SD-JWT to serialize.
 * @param credential The credential used for signing.
 * @param keyUnlockData Data to unlock the signing key.
 * @param clientId The verifier's client ID.
 * @param nonce The nonce for the session.
 * @param signatureAlgorithm The algorithm to use for signing.
 * @param issueDate The date of issuance.
 * @return The serialized SD-JWT as a string.
 */
internal suspend fun SdJwt<JwtAndClaims>.serializeWithKeyBinding(
    credential: SecureAreaBoundCredential,
    keyUnlockData: KeyUnlockData?,
    clientId: VerifierId,
    nonce: String,
    signatureAlgorithm: Algorithm,
    issueDate: Date,
): String {
    val algorithm = JWSAlgorithm.parse((signatureAlgorithm).joseAlgorithmIdentifier)
    val publicKey = credential.secureArea.getKeyInfo(credential.alias).publicKey
    val provider = keyUnlockData.asProvider()
    val buildKbJwt = NimbusSdJwtOps.kbJwtIssuer(
        signer = object : JWSSigner {
            override fun getJCAContext(): JCAContext = JCAContext()
            override fun supportedJWSAlgorithms(): Set<JWSAlgorithm> = setOf(algorithm)
            override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
                val signature = runBlocking {
                    withContext(provider) {
                        credential.secureArea.sign(
                            alias = credential.alias,
                            dataToSign = signingInput,
                            unlockReason = UnlockReason.Unspecified
                        )
                    }
                }
                return Base64URL.encode(signature.toJoseEncoded(algorithm))
            }
        },
        signAlgorithm = algorithm,
        publicKey = JWK.parseFromPEMEncodedObjects(publicKey.toPem()) as AsymmetricJWK
    ) {
        audience(clientId.clientId)
        claim("nonce", nonce)
        issueTime(issueDate)
    }
    return serializeWithKeyBinding(buildKbJwt).getOrThrow()
}

/**
 * Constructs a verifiable presentation for an SD-JWT VC credential.
 *
 * @param resolvedRequestObject The resolved OpenID4VP authorization request.
 * @param document The issued document containing the credential.
 * @param disclosedDocument The document with disclosed claims.
 * @param signatureAlgorithm The algorithm to use for signing.
 * @return The constructed [VerifiablePresentation.Generic].
 * @throws IllegalArgumentException if no claims are disclosed or presentation creation fails.
 */
internal suspend fun verifiablePresentationForSdJwtVc(
    resolvedRequestObject: ResolvedRequestObject,
    document: IssuedDocument,
    disclosedDocument: DisclosedDocument,
    signatureAlgorithm: Algorithm,
): VerifiablePresentation.Generic {
    return document.consumingCredential {
        val credentialIssuedData =
            getIssuedData<CredentialIssuedData.SdJwtVc>()
        val issuedSdJwt = credentialIssuedData.getOrThrow().issuedSdJwt

        val query = disclosedDocument.disclosedItems
            .filterIsInstance<SdJwtVcItem>()
            .map { item ->
                val elements = item.path.map { ClaimPathElement.Claim(it) }

                ClaimPath(elements.first(), *elements.drop(1).toTypedArray())
            }.toSet()

        // Check that at least one claim is disclosed, otherwise throw an error
        require(!(query.isEmpty())) { "No claims to disclose" }

        val presentation = issuedSdJwt.present(query)
            ?: throw IllegalArgumentException("Failed to create SD JWT VC presentation")

        val containsCnf = issuedSdJwt.jwt.second["cnf"] != null

        // If the SD-JWT contains a 'cnf' claim, serialize with key binding
        val serialized = if (containsCnf) {
            presentation.serializeWithKeyBinding(
                credential = this, //credential
                keyUnlockData = disclosedDocument.keyUnlockData,
                clientId = resolvedRequestObject.client.id,
                nonce = resolvedRequestObject.nonce,
                signatureAlgorithm = signatureAlgorithm,
                issueDate = Date()
            )
        } else {
            presentation.serialize()
        }

        VerifiablePresentation.Generic(serialized)
    }.getOrThrow()
}

/**
 * Constructs a verifiable presentation for an MSO mdoc credential.
 *
 * This function creates a verifiable presentation according to the ISO 18013-5 standard by:
 * 1. Filtering the requested documents to include only the specified document
 * 2. Generating a device response with the session transcript for cryptographic binding
 * 3. Converting the binary CBOR-encoded device response to a base64url-encoded string
 *
 * The session transcript is critical as it binds the presentation to the specific request session.
 *
 * @param documentManager The document manager for retrieving the full document content and credentials
 * @param disclosedDocument The document with specific claims that the user has consented to disclose
 * @param requestedDocuments The complete set of documents requested by the verifier
 * @param sessionTranscript The session transcript bytes that cryptographically bind the response to the request
 * @param signatureAlgorithm The algorithm to use for digitally signing the presentation
 * @return The constructed [VerifiablePresentation.Generic] containing the base64url-encoded device response
 * @throws RuntimeException if the response generation fails
 */
internal fun verifiablePresentationForMsoMdoc(
    documentManager: DocumentManager,
    disclosedDocument: DisclosedDocument,
    requestedDocuments: RequestedDocuments,
    sessionTranscript: ByteArray,
    signatureAlgorithm: Algorithm,
): VerifiablePresentation.Generic {
    // Create a new RequestedDocuments instance containing only the document that was selected for disclosure
    // This filters out any other documents that might have been in the original request
    val deviceResponse = ProcessedDeviceRequest(
        documentManager = documentManager,
        sessionTranscript = sessionTranscript,  // Bind the presentation to this specific session
        requestedDocuments = RequestedDocuments(requestedDocuments.filter { it.documentId == disclosedDocument.documentId })
    ).generateResponse(
        // Create a response containing only the selected document with its disclosed claims
        disclosedDocuments = DisclosedDocuments(disclosedDocument),
        signatureAlgorithm = signatureAlgorithm  // Use the specified algorithm to sign the response
    ).getOrThrow() as DeviceResponse  // Unwrap the Result and cast to DeviceResponse type

    // Convert the binary CBOR-encoded device response to a base64url-encoded string format
    // suitable for transmission in JWT or JSON payload without padding characters
    return VerifiablePresentation.Generic(
        value = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(deviceResponse.deviceResponseBytes)
    )
}