/*
 * Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.ProofTypeMeta
import eu.europa.ec.eudi.openid4vci.ProofTypesSupported
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import io.mockk.every
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OfferTest {

    private lateinit var mockCredentialOffer: CredentialOffer
    private lateinit var mockCredentialConfigurationIdentifiers: List<CredentialConfigurationIdentifier>
    private lateinit var mockCredentialConfigurations: List<CredentialConfiguration>

    @BeforeTest
    fun setup() {
        val msoMdocCredentialId = mockk<CredentialConfigurationIdentifier>(relaxed = true)
        val sdJwtVcCredentialId = mockk<CredentialConfigurationIdentifier>(relaxed = true)

        mockCredentialConfigurationIdentifiers = listOf(msoMdocCredentialId, sdJwtVcCredentialId)

        val msoMdocCredential = mockk<MsoMdocCredential>(relaxed = true) {
            every { proofTypesSupported } returns ProofTypesSupported(
                setOf(
                    ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES256, JWSAlgorithm.ES384))
                )
            )
            every { docType } returns "testDocType"
        }

        val sdJwtVcCredential = mockk<SdJwtVcCredential>(relaxed = true)

        mockCredentialConfigurations = listOf(msoMdocCredential, sdJwtVcCredential)

        val mockCredentialIssuerMetadata = mockk<CredentialIssuerMetadata> {
            every { credentialIssuerIdentifier.value.value.host } returns "test.host"
            every { credentialConfigurationsSupported } returns mapOf(msoMdocCredentialId to msoMdocCredential)
        }

        mockCredentialOffer = mockk<CredentialOffer>(relaxed = true) {
            every { credentialIssuerMetadata } returns mockCredentialIssuerMetadata
            every { credentialConfigurationIdentifiers } returns mockCredentialConfigurationIdentifiers
        }
    }

    @Test
    fun `issuerMetadata returns credentialConfigruation issuer Metadata`() {
        val offer = Offer(
            mockCredentialOffer
        )

        assertEquals(mockCredentialOffer.credentialIssuerMetadata, offer.issuerMetadata)
    }

    @Test
    fun `offeredDocuments returns filtered and mapped credentialConfigurationsSupported`() {

        val offer: Offer = Offer(
            mockCredentialOffer
        )

        assertEquals(1, offer.offeredDocuments.size)
    }
}