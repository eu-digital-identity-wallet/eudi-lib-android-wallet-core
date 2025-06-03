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

package eu.europa.ec.eudi.wallet.internal

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWSAlgorithm
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.iso18013.transfer.SessionTranscriptBytes
import eu.europa.ec.eudi.openid4vp.JarmConfiguration
import eu.europa.ec.eudi.openid4vp.JwkSetSource.ByReference
import eu.europa.ec.eudi.openid4vp.PreregisteredClient
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.openid4vp.SiopOpenId4VPConfig
import eu.europa.ec.eudi.openid4vp.SupportedClientIdScheme.Preregistered
import eu.europa.ec.eudi.openid4vp.SupportedClientIdScheme.RedirectUri
import eu.europa.ec.eudi.openid4vp.SupportedClientIdScheme.X509SanDns
import eu.europa.ec.eudi.openid4vp.SupportedClientIdScheme.X509SanUri
import eu.europa.ec.eudi.openid4vp.VPConfiguration
import eu.europa.ec.eudi.openid4vp.VpFormat
import eu.europa.ec.eudi.openid4vp.VpFormats
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
import eu.europa.ec.eudi.wallet.transfer.openId4vp.JwsAlgorithm
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import org.multipaz.crypto.Algorithm
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

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
 *  OID4VPHandover = [
 *    clientIdHash
 *    responseUriHash
 *    nonce
 *  ]
 *
 *  clientIdHash = bstr
 *  responseUriHash = bstr
 *
 *  where clientIdHash is the SHA-256 hash of clientIdToHash and responseUriHash is the SHA-256 hash of the responseUriToHash.
 *
 *
 *  clientIdToHash = [clientId, mdocGeneratedNonce]
 *  responseUriToHash = [responseUri, mdocGeneratedNonce]
 *
 *
 *  mdocGeneratedNonce = tstr
 *  clientId = tstr
 *  responseUri = tstr
 *  nonce = tstr
 *
 */
internal fun generateSessionTranscript(
    clientId: String,
    responseUri: String,
    nonce: String,
    mdocGeneratedNonce: String,
): SessionTranscriptBytes {

    val openID4VPHandover =
        generateOpenId4VpHandover(clientId, responseUri, nonce, mdocGeneratedNonce)

    val sessionTranscriptBytes =
        CBORObject.NewArray().apply {
            Add(CBORObject.Null)
            Add(CBORObject.Null)
            Add(openID4VPHandover)
        }.EncodeToBytes()

    return sessionTranscriptBytes
}

internal fun generateOpenId4VpHandover(
    clientId: String,
    responseUri: String,
    nonce: String,
    mdocGeneratedNonce: String,
): CBORObject {
    val clientIdToHash = CBORObject.NewArray().apply {
        Add(clientId)
        Add(mdocGeneratedNonce)
    }.EncodeToBytes()

    val responseUriToHash = CBORObject.NewArray().apply {
        Add(responseUri)
        Add(mdocGeneratedNonce)
    }.EncodeToBytes()

    val clientIdHash = MessageDigest.getInstance("SHA-256").digest(clientIdToHash)
    val responseUriHash = MessageDigest.getInstance("SHA-256").digest(responseUriToHash)

    val openID4VPHandover = CBORObject.NewArray().apply {
        Add(clientIdHash)
        Add(responseUriHash)
        Add(nonce)
    }
    return openID4VPHandover
}

internal fun generateMdocGeneratedNonce(): String {
    val secureRandom = SecureRandom()
    val bytes = ByteArray(16)
    secureRandom.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

internal fun OpenId4VpConfig.toSiopOpenId4VPConfig(trust: Openid4VpX509CertificateTrust): SiopOpenId4VPConfig {
    return SiopOpenId4VPConfig(
        jarmConfiguration = JarmConfiguration.Encryption(
            supportedAlgorithms = encryptionAlgorithms.map {
                JWEAlgorithm.parse(it.name)
            },
            supportedMethods = encryptionMethods.map {
                EncryptionMethod.parse(it.name)
            },
        ),
        supportedClientIdSchemes = clientIdSchemes.map { clientIdScheme ->
            when (clientIdScheme) {
                is ClientIdScheme.Preregistered -> Preregistered(
                    clientIdScheme.preregisteredVerifiers.associate { verifier ->
                        verifier.clientId to PreregisteredClient(
                            clientId = verifier.clientId,
                            legalName = verifier.legalName,
                            jarConfig = verifier.jwsAlgorithm.nimbus to ByReference(verifier.jwkSetSource)
                        )
                    }
                )

                ClientIdScheme.X509SanDns -> X509SanDns(trust)

                ClientIdScheme.X509SanUri -> X509SanUri(trust)

                ClientIdScheme.RedirectUri -> RedirectUri
            }
        },
        vpConfiguration = VPConfiguration(
            vpFormats = formats.toVpFormats()
        )
    )
}

internal fun ResolvedRequestObject.OpenId4VPAuthorization.getSessionTranscriptBytes(
    mdocGeneratedNonce: String,
): SessionTranscriptBytes {
    val clientId = this.client.id.clientId
    val responseUri = when(val mode = this.responseMode) {
        is ResponseMode.DirectPostJwt -> mode.responseURI.toString()
        else -> ""
    }
    val nonce = this.nonce

    val sessionTranscriptBytes = generateSessionTranscript(
        clientId,
        responseUri,
        nonce,
        mdocGeneratedNonce
    )
    return sessionTranscriptBytes
}

internal fun List<Format>.toVpFormats(): VpFormats {

    val msoMdocVpFormat = firstOrNull { it == Format.MsoMdoc }
        ?.let { VpFormat.MsoMdoc.ES256 }

    val sdJwtVcVpFormat = filterIsInstance<Format.SdJwtVc>()
        .firstOrNull()
        ?.let {
            VpFormat.SdJwtVc(
                sdJwtAlgorithms = it.sdJwtAlgorithms.map { it.toJwsAlgorithm(JWSAlgorithm.ES256) },
                kbJwtAlgorithms = it.kbJwtAlgorithms.map { it.toJwsAlgorithm(JWSAlgorithm.ES256) }
            )
        }

    return VpFormats(
        sdJwtVc = sdJwtVcVpFormat,
        msoMdoc = msoMdocVpFormat
    )
}

internal fun Algorithm.toJwsAlgorithm(default: JWSAlgorithm): JWSAlgorithm = try {
    JWSAlgorithm.parse(this.joseAlgorithmIdentifier)
} catch (_: Throwable) {
    default
}

internal val JwsAlgorithm.nimbus: JWSAlgorithm
    get() = when (this) {
        JwsAlgorithm.ES256 -> JWSAlgorithm.ES256
        JwsAlgorithm.ES384 -> JWSAlgorithm.ES384
        JwsAlgorithm.ES512 -> JWSAlgorithm.ES512
        JwsAlgorithm.EdDSA -> JWSAlgorithm.EdDSA
        JwsAlgorithm.HS256 -> JWSAlgorithm.HS256
        JwsAlgorithm.HS384 -> JWSAlgorithm.HS384
        JwsAlgorithm.HS512 -> JWSAlgorithm.HS512
        JwsAlgorithm.PS256 -> JWSAlgorithm.PS256
        JwsAlgorithm.PS384 -> JWSAlgorithm.PS384
        JwsAlgorithm.PS512 -> JWSAlgorithm.PS512
        JwsAlgorithm.RS256 -> JWSAlgorithm.RS256
        JwsAlgorithm.RS384 -> JWSAlgorithm.RS384
        JwsAlgorithm.RS512 -> JWSAlgorithm.RS512
        JwsAlgorithm.ES256K -> JWSAlgorithm.ES256K
        JwsAlgorithm.Ed448 -> JWSAlgorithm.Ed448
        JwsAlgorithm.Ed25519 -> JWSAlgorithm.Ed25519
    }