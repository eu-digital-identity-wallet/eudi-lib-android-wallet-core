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
package eu.europa.ec.eudi.wallet.trust

import eu.europa.ec.eudi.etsi119602.consultation.VerifyJwtSignature
import eu.europa.ec.eudi.etsi119602.datamodel.Uri
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationClassifications
import eu.europa.ec.eudi.etsi1196x2.consultation.SupportedLists
import io.mockk.mockk
import java.security.cert.X509Certificate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EtsiTrustConfigBuilderTest {

    @Test
    fun `build fails without LoTE signer trust anchors`() {
        assertFailsWith<IllegalArgumentException> {
            builder().build()
        }
    }

    @Test
    fun `build passes the LoTE signer trust anchors to the config`() {
        val certificate = mockk<X509Certificate>()

        val config = builder().apply { loteSignerTrustAnchors(certificate) }.build()

        assertEquals(listOf(certificate), config.loteSignerTrustAnchors)
    }

    @Test
    fun `build does not need LoTE signer trust anchors with a custom JWT signature verifier`() {
        val verifier = mockk<VerifyJwtSignature>()

        val config = builder().apply { jwtSignatureVerifier(verifier) }.build()

        assertEquals(verifier, config.customJwtSignatureVerifier)
    }

    private fun builder() = EtsiTrustConfigBuilder().apply {
        loteLocations(SupportedLists(pidProviders = Uri("https://trust.example.eu/pid-providers.jwt")))
        classifications(AttestationClassifications())
    }
}
