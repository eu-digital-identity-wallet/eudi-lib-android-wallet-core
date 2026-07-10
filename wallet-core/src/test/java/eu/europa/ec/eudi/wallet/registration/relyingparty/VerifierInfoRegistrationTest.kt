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
package eu.europa.ec.eudi.wallet.registration.relyingparty

import eu.europa.ec.eudi.openid4vp.VerifierInfo
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQueryIds
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64
import kotlin.test.assertContentEquals

class VerifierInfoRegistrationTest {

    @Test
    fun `a base64url-encoded registration certificate is decoded (RO-16)`() {
        val certificate = byteArrayOf(1, 2, 3, 4, 5)
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(certificate)

        val result = verifierInfo(FORMAT_REGISTRATION_CERT, encoded).extractRegistrationCertificate()

        assertContentEquals(certificate, result)
    }

    @Test
    fun `a compact JWS placed directly is taken as-is`() {
        // a compact JWS value is distinguished by its dot-separated segments and used as-is
        val compactJws = "eyJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJPUkctMSJ9.c2lnbmF0dXJl"

        val result = verifierInfo(FORMAT_REGISTRATION_CERT, compactJws).extractRegistrationCertificate()

        assertContentEquals(compactJws.toByteArray(Charsets.US_ASCII), result)
    }

    @Test
    fun `an element that is not a registration certificate is ignored`() {
        val result = verifierInfo("jwt", "some-verifier-attestation").extractRegistrationCertificate()

        assertNull(result)
    }

    @Test
    fun `no verifier info yields null`() {
        assertNull((null as VerifierInfo?).extractRegistrationCertificate())
    }

    @Test
    fun `a registration certificate element carrying credential_ids is ignored (RO-14)`() {
        val verifierInfo = VerifierInfo(
            listOf(
                VerifierInfo.Attestation(
                    format = VerifierInfo.Attestation.Format(FORMAT_REGISTRATION_CERT),
                    data = VerifierInfo.Attestation.Data(JsonPrimitive("AQIDBAU")),
                    credentialIds = CredentialQueryIds(listOf(QueryId("cred-1"))),
                ),
            ),
        )

        assertNull(verifierInfo.extractRegistrationCertificate())
    }

    @Test
    fun `more than one registration certificate element is ambiguous and ignored (RO-13)`() {
        val verifierInfo = VerifierInfo(
            listOf(
                VerifierInfo.Attestation(
                    format = VerifierInfo.Attestation.Format(FORMAT_REGISTRATION_CERT),
                    data = VerifierInfo.Attestation.Data(JsonPrimitive("AQIDBAU")),
                ),
                VerifierInfo.Attestation(
                    format = VerifierInfo.Attestation.Format(FORMAT_REGISTRATION_CERT),
                    data = VerifierInfo.Attestation.Data(JsonPrimitive("BQYHCAk")),
                ),
            ),
        )

        assertNull(verifierInfo.extractRegistrationCertificate())
    }
}

private fun verifierInfo(format: String, data: String): VerifierInfo =
    VerifierInfo(
        listOf(
            VerifierInfo.Attestation(
                format = VerifierInfo.Attestation.Format(format),
                data = VerifierInfo.Attestation.Data(JsonPrimitive(data)),
            ),
        ),
    )
