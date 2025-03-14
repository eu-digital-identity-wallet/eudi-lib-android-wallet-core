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

import com.nimbusds.jose.JWSAlgorithm
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.openid4vp.Client
import eu.europa.ec.eudi.openid4vp.PresentationQuery
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.openid4vp.VpFormat
import eu.europa.ec.eudi.openid4vp.VpFormats
import io.mockk.mockk
import org.bouncycastle.util.encoders.Hex
import java.net.URL
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals


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
    fun testGetSessionTranscriptBytes() {
        val expected = CBORObject.NewArray()
            .Add(CBORObject.Null)
            .Add(CBORObject.Null)
            .Add(
                CBORObject.NewArray()
                    .Add(MessageDigest.getInstance("SHA-256").digest(
                        CBORObject.NewArray()
                            .Add(clientId)
                            .Add(mdocGeneratedNonce)
                            .EncodeToBytes()
                    ))
                    .Add(MessageDigest.getInstance("SHA-256").digest(
                        CBORObject.NewArray()
                            .Add(responseUri)
                            .Add(mdocGeneratedNonce)
                            .EncodeToBytes()
                    ))
                    .Add(nonce)
            )
            .EncodeToBytes()
        val auth = ResolvedRequestObject.OpenId4VPAuthorization(
            client = Client.Preregistered(clientId, legalName = clientId),
            responseMode = ResponseMode.DirectPostJwt(URL(responseUri)),
            nonce = nonce,
            state = "state",
            vpFormats = VpFormats(
                msoMdoc = VpFormat.MsoMdoc(listOf(JWSAlgorithm.ES256))
            ),
            jarmRequirement = mockk(),
            presentationQuery = PresentationQuery.ByPresentationDefinition(mockk()),
            transactionData = null
        )
        val mdocGeneratedNonce = mdocGeneratedNonce
        val sessionTranscriptBytes = auth.getSessionTranscriptBytes(mdocGeneratedNonce)
        assertContentEquals(expected, sessionTranscriptBytes)
    }

    @Test
    fun testGetSessionTranscriptBytesWithOtherThanDirectPostJwtLeadsToEmtpyResponseUri() {
        val expected = CBORObject.NewArray()
            .Add(CBORObject.Null)
            .Add(CBORObject.Null)
            .Add(
                CBORObject.NewArray()
                    .Add(MessageDigest.getInstance("SHA-256").digest(
                        CBORObject.NewArray()
                            .Add(clientId)
                            .Add(mdocGeneratedNonce)
                            .EncodeToBytes()
                    ))
                    .Add(MessageDigest.getInstance("SHA-256").digest(
                        CBORObject.NewArray()
                            .Add("")
                            .Add(mdocGeneratedNonce)
                            .EncodeToBytes()
                    ))
                    .Add(nonce)
            )
            .EncodeToBytes()
        val auth = ResolvedRequestObject.OpenId4VPAuthorization(
            client = Client.Preregistered(clientId, legalName = clientId),
            responseMode = ResponseMode.DirectPost(URL(responseUri)),
            nonce = nonce,
            state = "state",
            vpFormats = VpFormats(
                msoMdoc = VpFormat.MsoMdoc(listOf(JWSAlgorithm.ES256))
            ),
            jarmRequirement = mockk(),
            presentationQuery = PresentationQuery.ByPresentationDefinition(mockk()),
            transactionData = null
        )
        val mdocGeneratedNonce = mdocGeneratedNonce
        val sessionTranscriptBytes = auth.getSessionTranscriptBytes(mdocGeneratedNonce)
        assertContentEquals(expected, sessionTranscriptBytes)
    }
}
