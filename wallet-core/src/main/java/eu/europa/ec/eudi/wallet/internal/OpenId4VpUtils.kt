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
import eu.europa.ec.eudi.openid4vp.SupportedClientIdScheme.X509SanDns
import eu.europa.ec.eudi.openid4vp.SupportedClientIdScheme.X509SanUri
import eu.europa.ec.eudi.openid4vp.VPConfiguration
import eu.europa.ec.eudi.openid4vp.VpFormat
import eu.europa.ec.eudi.openid4vp.VpFormats
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import java.net.URI
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
                            verifier.clientId,
                            verifier.legalName,
                            JWSAlgorithm.RS256 to ByReference(
                                URI("${verifier.verifierApi}/wallet/public-keys.json")
                            )
                        )
                    }
                )

                ClientIdScheme.X509SanDns -> X509SanDns(trust)

                ClientIdScheme.X509SanUri -> X509SanUri(trust)
            }
        },
        vpConfiguration = VPConfiguration(
            vpFormats = VpFormats(
                formats.map { format ->
                    when (format) {
                        is Format.MsoMdoc -> {
                            VpFormat.MsoMdoc
                        }

                        is Format.SdJwtVc -> {
                            VpFormat.SdJwtVc(
                                format.sdJwtAlgorithms.map { alg ->
                                    JWSAlgorithm.parse(alg.jwseAlgorithmIdentifier)
                                },
                                format.kbJwtAlgorithms.map { alg ->
                                    JWSAlgorithm.parse(alg.jwseAlgorithmIdentifier)
                                }
                            )
                        }
                    }
                }.toList()
            )
        )
    )
}

internal fun ResolvedRequestObject.OpenId4VPAuthorization.getSessionTranscriptBytes(
    mdocGeneratedNonce: String,
): SessionTranscriptBytes {
    val clientId = this.client.id
    val responseUri =
        (this.responseMode as ResponseMode.DirectPostJwt?)?.responseURI?.toString()
            ?: ""
    val nonce = this.nonce

    val sessionTranscriptBytes = generateSessionTranscript(
        clientId,
        responseUri,
        nonce,
        mdocGeneratedNonce
    )
    return sessionTranscriptBytes
}
