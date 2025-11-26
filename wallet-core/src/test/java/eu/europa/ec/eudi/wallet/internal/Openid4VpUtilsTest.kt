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

//import com.nimbusds.jose.JWEAlgorithm
//import com.nimbusds.jose.JWSAlgorithm
//import com.upokecenter.cbor.CBORObject
//import eu.europa.ec.eudi.openid4vp.Client
//import eu.europa.ec.eudi.openid4vp.JwkSetSource
//import eu.europa.ec.eudi.openid4vp.PresentationQuery
//import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
//import eu.europa.ec.eudi.openid4vp.ResponseMode
//import eu.europa.ec.eudi.openid4vp.SupportedClientIdScheme
//import eu.europa.ec.eudi.openid4vp.VpFormat
//import eu.europa.ec.eudi.openid4vp.VpFormats
//import eu.europa.ec.eudi.openid4vp.encryptionConfig
//import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme.Preregistered
//import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionAlgorithm
//import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionMethod
//import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
//import eu.europa.ec.eudi.wallet.transfer.openId4vp.JwsAlgorithm
//import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
//import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderTrust
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderTrustImpl
import org.bouncycastle.util.encoders.Hex
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals


/**
 *    Examples has been taken from:
 *    https://github.com/awoie/annex-b-examples/pull/2
 *    https://github.com/awoie/annex-b-examples/blob/main/examples/annex-b-examples.txt
 */

const val ANNEX_B_OPENID4VP_HANDOVER =
    "835820DA25C527E5FB75BC2DD31267C02237C4462BA0C1BF37071F692E7DD93B10AD0B5820F6ED8E3220D3C59A5F17EB45F48AB70AEECF9EE21744B1014982350BD96AC0C572616263646566676831323334353637383930"
const val ANNEX_B_SESSION_TRANSCRIPT =
    "83F6F6835820DA25C527E5FB75BC2DD31267C02237C4462BA0C1BF37071F692E7DD93B10AD0B5820F6ED8E3220D3C59A5F17EB45F48AB70AEECF9EE21744B1014982350BD96AC0C572616263646566676831323334353637383930"

const val clientId = "example.com"
const val responseUri = "https://example.com/12345/response"
const val nonce = "abcdefgh1234567890"
const val mdocGeneratedNonce = "1234567890abcdefgh"

class Openid4VpUtilsTest {

    @Test
    fun testGenerateOpenId4VpHandover() {
        val openid4VpHandover = generateOpenId4VpHandover(
            clientId,
            responseUri,
            nonce,
            mdocGeneratedNonce
        ).EncodeToBytes()
        assertEquals(ANNEX_B_OPENID4VP_HANDOVER, Hex.toHexString(openid4VpHandover).uppercase())
    }

    @Test
    fun testGenerateSessionTranscript() {
        val sessionTranscript = generateSessionTranscript(
            clientId,
            responseUri,
            nonce,
            mdocGeneratedNonce
        )
        assertEquals(ANNEX_B_SESSION_TRANSCRIPT, Hex.toHexString(sessionTranscript).uppercase())
    }

    @Test
    fun testToOpenId4VPConfig() {
        val trust = OpenId4VpReaderTrustImpl(null) // Assuming a default constructor or mock

//        val openId4VpConfig = OpenId4VpConfig.Builder()
//            .withEncryptionAlgorithms(listOf(EncryptionAlgorithm.ECDH_ES))
//            .withEncryptionMethods(listOf(EncryptionMethod.A256GCM))
//            .withClientIdSchemes(
//                listOf(
//                    Preregistered(
//                        preregisteredVerifiers = listOf(
//                            PreregisteredVerifier(
//                                clientId = "client1",
//                                legalName = "Client 1",
//                                jwsAlgorithm = JwsAlgorithm.RS256,
//                                verifierApi = "https://example.com"
//                            )
//                        )
//                    )
//                )
//            ).withFormats(listOf(Format.MsoMdoc))
//            .build()
//
//        val siopOpenId4VPConfig = openId4VpConfig.toSiopOpenId4VPConfig(trust)
//
//        assertEquals(
//            1,
//            siopOpenId4VPConfig.jarmConfiguration.encryptionConfig()?.supportedAlgorithms?.size
//        )
//        val supportedAlgorithms =
//            siopOpenId4VPConfig.jarmConfiguration.encryptionConfig()!!.supportedAlgorithms
//        assertEquals(JWEAlgorithm.ECDH_ES, supportedAlgorithms[0])
//
//        assertEquals(
//            1,
//            siopOpenId4VPConfig.jarmConfiguration.encryptionConfig()?.supportedMethods?.size
//        )
//        val supportedMethods =
//            siopOpenId4VPConfig.jarmConfiguration.encryptionConfig()!!.supportedMethods
//        assertEquals(com.nimbusds.jose.EncryptionMethod.A256GCM, supportedMethods[0])
//
//        assertEquals(1, siopOpenId4VPConfig.supportedClientIdSchemes.size)
//        val supportedClientIdScheme = siopOpenId4VPConfig.supportedClientIdSchemes[0]
//        assertIs<SupportedClientIdScheme.Preregistered>(supportedClientIdScheme)
//
//        val preregisteredClient = supportedClientIdScheme.clients.values.first()
//        assertEquals("client1", preregisteredClient.clientId)
//        assertEquals("Client 1", preregisteredClient.legalName)
//        assertEquals(JWSAlgorithm.RS256, preregisteredClient.jarConfig?.first)
//        val jwkSetSource = preregisteredClient.jarConfig?.second
//        assertIs<JwkSetSource.ByReference>(jwkSetSource)
//        assertEquals("https://example.com/wallet/public-keys.json", jwkSetSource.jwksUri.toString())
    }

    @Test
    fun testGetSessionTranscriptBytes() {
//        val expected = CBORObject.NewArray()
//            .Add(CBORObject.Null)
//            .Add(CBORObject.Null)
//            .Add(
//                CBORObject.NewArray()
//                    .Add(MessageDigest.getInstance("SHA-256").digest(
//                        CBORObject.NewArray()
//                            .Add(clientId)
//                            .Add(mdocGeneratedNonce)
//                            .EncodeToBytes()
//                    ))
//                    .Add(MessageDigest.getInstance("SHA-256").digest(
//                        CBORObject.NewArray()
//                            .Add(responseUri)
//                            .Add(mdocGeneratedNonce)
//                            .EncodeToBytes()
//                    ))
//                    .Add(nonce)
//            )
//            .EncodeToBytes()
//        val auth = ResolvedRequestObject.OpenId4VPAuthorization(
//            client = Client.Preregistered(clientId, legalName = clientId),
//            responseMode = ResponseMode.DirectPostJwt(URL(responseUri)),
//            nonce = nonce,
//            state = "state",
//            vpFormats = VpFormats(
//                msoMdoc = VpFormat.MsoMdoc(listOf(JWSAlgorithm.ES256))
//            ),
//            jarmRequirement = mockk(),
//            presentationQuery = PresentationQuery.ByPresentationDefinition(mockk()),
//            transactionData = null
//        )
//        val mdocGeneratedNonce = mdocGeneratedNonce
//        val sessionTranscriptBytes = auth.getSessionTranscriptBytes(mdocGeneratedNonce)
//        assertContentEquals(expected, sessionTranscriptBytes)
    }

    @Test
    fun testGetSessionTranscriptBytesWithOtherThanDirectPostJwtLeadsToEmtpyResponseUri() {
//        val expected = CBORObject.NewArray()
//            .Add(CBORObject.Null)
//            .Add(CBORObject.Null)
//            .Add(
//                CBORObject.NewArray()
//                    .Add(MessageDigest.getInstance("SHA-256").digest(
//                        CBORObject.NewArray()
//                            .Add(clientId)
//                            .Add(mdocGeneratedNonce)
//                            .EncodeToBytes()
//                    ))
//                    .Add(MessageDigest.getInstance("SHA-256").digest(
//                        CBORObject.NewArray()
//                            .Add("")
//                            .Add(mdocGeneratedNonce)
//                            .EncodeToBytes()
//                    ))
//                    .Add(nonce)
//            )
//            .EncodeToBytes()
//        val auth = ResolvedRequestObject.OpenId4VPAuthorization(
//            client = Client.Preregistered(clientId, legalName = clientId),
//            responseMode = ResponseMode.DirectPost(URL(responseUri)),
//            nonce = nonce,
//            state = "state",
//            vpFormats = VpFormats(
//                msoMdoc = VpFormat.MsoMdoc(listOf(JWSAlgorithm.ES256))
//            ),
//            jarmRequirement = mockk(),
//            presentationQuery = PresentationQuery.ByPresentationDefinition(mockk()),
//            transactionData = null
//        )
//        val mdocGeneratedNonce = mdocGeneratedNonce
//        val sessionTranscriptBytes = auth.getSessionTranscriptBytes(mdocGeneratedNonce)
//        assertContentEquals(expected, sessionTranscriptBytes)
    }

    /**
     * Tests for transaction data hash computation.
     * Per OpenID4VP spec, transaction data hashes use SHA-256 and are base64url-encoded without padding.
     */
    @Test
    fun testComputeTransactionDataHash_producesCorrectHash() {
        // Test with a known input - a simple base64url-encoded JSON object
        val transactionDataValue = "eyJ0eXBlIjoiZXUuZXVyb3BhLmVjLmV1ZGkuZmFtaWx5LW5hbWUtcHJlc2VudGF0aW9uIiwiY3JlZGVudGlhbF9pZHMiOlsicGlkX3NkX2p3dCJdfQ"

        // Compute the hash using our function
        val hash = computeTransactionDataHash(transactionDataValue)

        // Manually compute expected hash for verification
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(transactionDataValue.encodeToByteArray())
        val expectedHash = Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest())

        assertEquals(expectedHash, hash)
    }

    @Test
    fun testComputeTransactionDataHash_differentInputsProduceDifferentHashes() {
        val transactionDataValue1 = "eyJ0eXBlIjoiZXhhbXBsZTEifQ"
        val transactionDataValue2 = "eyJ0eXBlIjoiZXhhbXBsZTIifQ"

        val hash1 = computeTransactionDataHash(transactionDataValue1)
        val hash2 = computeTransactionDataHash(transactionDataValue2)

        assertNotEquals(hash1, hash2, "Different inputs should produce different hashes")
    }

    @Test
    fun testComputeTransactionDataHash_sameInputProducesSameHash() {
        val transactionDataValue = "eyJ0eXBlIjoiZXhhbXBsZSJ9"

        val hash1 = computeTransactionDataHash(transactionDataValue)
        val hash2 = computeTransactionDataHash(transactionDataValue)

        assertEquals(hash1, hash2, "Same input should always produce the same hash")
    }

    @Test
    fun testComputeTransactionDataHash_outputIsBase64UrlWithoutPadding() {
        val transactionDataValue = "eyJ0eXBlIjoiZXhhbXBsZSJ9"

        val hash = computeTransactionDataHash(transactionDataValue)

        // Verify it doesn't contain standard base64 characters that differ in base64url
        assert(!hash.contains('+')) { "Hash should not contain '+' (base64url uses '-' instead)" }
        assert(!hash.contains('/')) { "Hash should not contain '/' (base64url uses '_' instead)" }
        assert(!hash.contains('=')) { "Hash should not contain '=' padding" }
    }
}
