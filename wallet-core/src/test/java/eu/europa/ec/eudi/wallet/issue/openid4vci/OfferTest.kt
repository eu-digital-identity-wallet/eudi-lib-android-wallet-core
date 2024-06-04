/*
 *  Copyright (c) 2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.openid4vci.*
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class OfferTest {

    @Test
    fun `issuerName returns host of credentialIssuerIdentifier`() {
        val mockCredentialIssuerMetadata = mockk<CredentialIssuerMetadata> {
            every { credentialIssuerIdentifier.value.value.host } returns "test.host"
        }
        val mockCredentialOffer = mockk<CredentialOffer> {
            every { credentialIssuerMetadata } returns mockCredentialIssuerMetadata
        }
        val offer: Offer = DefaultOffer(mockCredentialOffer)

        assertEquals("test.host", offer.issuerName)
    }

    @Test
    fun `offeredDocuments returns filtered and mapped credentialConfigurationsSupported`() {
        val mockCredentialConfiguration = mockk<MsoMdocCredential>(relaxed = true) {
            every { proofTypesSupported } returns ProofTypesSupported(
                setOf(
                    ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES256, JWSAlgorithm.ES384))
                )
            )
            every { name } returns "testName"
            every { docType } returns "testDocType"
        }
        val mockCredentialConfigurationIdentifier = mockk<CredentialConfigurationIdentifier>(relaxed = true)
        val mockCredentialIssuerMetadata = mockk<CredentialIssuerMetadata> {
            every { credentialConfigurationsSupported } returns mapOf(mockCredentialConfigurationIdentifier to mockCredentialConfiguration)
        }
        val mockCredentialOffer = mockk<CredentialOffer>(relaxed = true) {
            every { credentialIssuerMetadata } returns mockCredentialIssuerMetadata
            every { credentialConfigurationIdentifiers } returns listOf(mockCredentialConfigurationIdentifier)
        }
        val offer: Offer = DefaultOffer(mockCredentialOffer)

        val expectedOfferedDocument = Offer.OfferedDocument(
            "testName",
            "testDocType",
            mockCredentialConfigurationIdentifier,
            mockCredentialConfiguration
        )

        assertEquals(listOf(expectedOfferedDocument), offer.offeredDocuments)
    }
}