/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import eu.europa.ec.eudi.openid4vp.OpenId4Vp
import eu.europa.ec.eudi.openid4vp.RequestValidationError
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.X509CertificateTrust
import eu.europa.ec.eudi.wallet.internal.makeOpenId4VPConfig
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ReaderTrustFixture.Companion.VERIFIER_DNS
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Test
import java.net.URLEncoder
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests the response encryption algorithm and method selected with the default [OpenId4VpConfig].
 *
 * The requests are resolved by the OpenID4VP library.
 */
class OpenId4VpResponseEncryptionTest {

    private val fixture = ReaderTrustFixture()
    private val config = OpenId4VpConfig.Builder()
        .withClientIdSchemes(ClientIdScheme.X509SanDns)
        .withFormats(Format.MsoMdoc.ES256)
        .build()

    @Test
    fun `A256GCM is used when the verifier supports A128GCM and A256GCM`() = runBlocking<Unit> {
        val resolution = resolve(encryptionMethods = listOf("A128GCM", "A256GCM"))

        val encryption = assertIs<Resolution.Success>(resolution).requestObject.responseEncryptionSpecification
        assertEquals(EncryptionMethod.A256GCM, encryption?.encryptionMethod)
        assertEquals(JWEAlgorithm.ECDH_ES, encryption?.encryptionAlgorithm)
    }

    @Test
    fun `a request is rejected when the verifier supports only A128CBC-HS256`() = runBlocking<Unit> {
        val resolution = resolve(encryptionMethods = listOf("A128CBC-HS256"))

        assertIs<RequestValidationError.UnsupportedClientMetaData>(assertIs<Resolution.Invalid>(resolution).error)
    }

    /** Resolves a signed request with `direct_post.jwt` and an `ECDH-ES` key of the verifier. */
    private suspend fun resolve(encryptionMethods: List<String>): Resolution {
        val encryptionKey = ECKeyGenerator(Curve.P_256)
            .keyID("verifier-encryption-key")
            .keyUse(KeyUse.ENCRYPTION)
            .algorithm(JWEAlgorithm.ECDH_ES)
            .generate()
            .toPublicJWK()
        val claims = buildJsonObject {
            put("client_id", fixture.x509SanDnsClientId)
            put("response_type", "vp_token")
            put("response_mode", "direct_post.jwt")
            put("response_uri", RESPONSE_URI)
            put("nonce", fixture.nonce())
            put("aud", "https://self-issued.me/v2")
            put("dcql_query", fixture.dcql)
            putJsonObject("client_metadata") {
                put("jwks", Json.parseToJsonElement(JWKSet(encryptionKey).toString()))
                putJsonArray("encrypted_response_enc_values_supported") { encryptionMethods.forEach { add(it) } }
                putJsonObject("vp_formats_supported") { putJsonObject("mso_mdoc") {} }
            }
        }
        val requestObject = fixture.requestObject(claims, fixture.verifierChain, fixture.verifierKey)
        val openId4Vp = OpenId4Vp.overRedirects(
            openId4VPConfig = makeOpenId4VPConfig(config, X509CertificateTrust { true }),
            httpClient = HttpClient(MockEngine { error("No HTTP request is expected") }),
        )
        return openId4Vp.resolveRequestUri(
            "openid4vp://?client_id=${encode(fixture.x509SanDnsClientId)}&request=$requestObject"
        )
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")

    private companion object {
        const val RESPONSE_URI = "https://$VERIFIER_DNS/response"
    }
}
