/*
 * Copyright (c) 2024-2025 European Commission
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
import eu.europa.ec.eudi.openid4vci.BatchCredentialIssuance
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.Grants
import eu.europa.ec.eudi.openid4vci.KeyAttestationRequirement
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.MsoMdocPolicy
import eu.europa.ec.eudi.openid4vci.ProofTypeMeta
import eu.europa.ec.eudi.openid4vci.ProofTypesSupported
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.openid4vci.TxCode
import eu.europa.ec.eudi.openid4vci.TxCodeInputMode
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import io.mockk.every
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OfferTest {

    private lateinit var mockCredentialOffer: CredentialOffer
    private val msoMdocCredentialId = CredentialConfigurationIdentifier("mso-mdoc")
    private val sdJwtVcCredentialId = CredentialConfigurationIdentifier("sd-jwt-vc")
    private lateinit var mockCredentialConfigurationIdentifiers: List<CredentialConfigurationIdentifier>
    private lateinit var mockCredentialConfigurations: List<CredentialConfiguration>
    private lateinit var mockMsoMdocCredential: MsoMdocCredential
    private lateinit var mockSdJwtVcCredential: SdJwtVcCredential
    private lateinit var mockCredentialIssuerMetadata: CredentialIssuerMetadata
    private lateinit var txCode: TxCode

    @BeforeTest
    fun setup() {

        mockCredentialConfigurationIdentifiers = listOf(msoMdocCredentialId, sdJwtVcCredentialId)

        mockMsoMdocCredential = mockk<MsoMdocCredential>(relaxed = true) {
            every { proofTypesSupported } returns ProofTypesSupported(
                setOf(
                    ProofTypeMeta.Jwt(
                        listOf(JWSAlgorithm.ES256, JWSAlgorithm.ES384),
                        KeyAttestationRequirement.NotRequired
                    )
                )
            )
            every { docType } returns "testDocType"
            every { isoPolicy } returns MsoMdocPolicy(true, 2)
        }

        mockSdJwtVcCredential = mockk<SdJwtVcCredential>(relaxed = true) {
            every { type } returns "testType"
        }

        mockCredentialConfigurations = listOf(mockMsoMdocCredential, mockSdJwtVcCredential)

        mockCredentialIssuerMetadata = mockk<CredentialIssuerMetadata> {
            every { credentialIssuerIdentifier } returns CredentialIssuerId("https://test.host").getOrThrow()
            every { credentialConfigurationsSupported } returns mapOf(
                msoMdocCredentialId to mockMsoMdocCredential,
                sdJwtVcCredentialId to mockSdJwtVcCredential
            )
            every { batchCredentialIssuance } returns BatchCredentialIssuance.Supported(3)
        }

        txCode = TxCode(
            inputMode = TxCodeInputMode.TEXT,
            length = 4,
            description = null
        )

        mockCredentialOffer = mockk<CredentialOffer>(relaxed = true) {
            every { credentialIssuerMetadata } returns mockCredentialIssuerMetadata
            every { credentialConfigurationIdentifiers } returns mockCredentialConfigurationIdentifiers
            every { grants } returns Grants.PreAuthorizedCode(
                preAuthorizedCode = "code",
                txCode = txCode,
                authorizationServer = null
            )
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
        val offer = Offer(mockCredentialOffer)

        val offeredDocuments = offer.offeredDocuments
        assertEquals(2, offeredDocuments.size)

        val msoMdocOfferedDocument =
            offeredDocuments.find { it.configurationIdentifier == msoMdocCredentialId }
        assertEquals(msoMdocCredentialId, msoMdocOfferedDocument?.configurationIdentifier)
        assertEquals(mockMsoMdocCredential, msoMdocOfferedDocument?.configuration)

        val sdJwtVcOfferedDocument =
            offeredDocuments.find { it.configurationIdentifier == sdJwtVcCredentialId }
        assertEquals(sdJwtVcCredentialId, sdJwtVcOfferedDocument?.configurationIdentifier)
        assertEquals(mockSdJwtVcCredential, sdJwtVcOfferedDocument?.configuration)
    }

    @Test
    fun `txCodeSpec returns txCode from grants preAuthorizedCode`() {
        val offer = Offer(mockCredentialOffer)

        assertEquals(txCode, offer.txCodeSpec)
    }

    @Test
    fun `documentFormat returns correct format for MsoMdocCredential`() {
        val offer = Offer(mockCredentialOffer)
        val offeredDocument =
            offer.offeredDocuments.find { it.configurationIdentifier == msoMdocCredentialId }!!

        val documentFormat = offeredDocument.documentFormat
        require(documentFormat is MsoMdocFormat)
        assertEquals("testDocType", documentFormat.docType)
    }

    @Test
    fun `documentFormat returns correct format for SdJwtVcCredential`() {
        val offer = Offer(mockCredentialOffer)
        val offeredDocument =
            offer.offeredDocuments.find { it.configurationIdentifier == sdJwtVcCredentialId }!!

        val documentFormat = offeredDocument.documentFormat
        require(documentFormat is SdJwtVcFormat)
        assertEquals("testType", documentFormat.vct)
    }

    @Test
    fun `batchCredentialIssuanceSize returns supported batch size from issuer metadata`() {
        val offer = Offer(mockCredentialOffer)
        val offeredDocument =
            offer.offeredDocuments.find { it.configurationIdentifier == msoMdocCredentialId }!!

        assertEquals(3, offeredDocument.batchCredentialIssuanceSize)
    }

    @Test
    fun `batchCredentialIssuanceSize returns 1 when batch credential issuance is not supported`() {
        // Create a new relaxed mock for this specific test
        val localMetadata = mockk<CredentialIssuerMetadata>(relaxed = true)
        every { localMetadata.credentialIssuerIdentifier } returns CredentialIssuerId("https://test.host").getOrThrow()
        every { localMetadata.credentialConfigurationsSupported } returns mapOf(
            msoMdocCredentialId to mockMsoMdocCredential
        )
        every { localMetadata.batchCredentialIssuance } returns BatchCredentialIssuance.NotSupported

        val localCredentialOffer = mockk<CredentialOffer>(relaxed = true)
        every { localCredentialOffer.credentialIssuerMetadata } returns localMetadata
        every { localCredentialOffer.credentialConfigurationIdentifiers } returns listOf(
            msoMdocCredentialId
        )
        every { localCredentialOffer.grants } returns Grants.PreAuthorizedCode(
            preAuthorizedCode = "code",
            txCode = txCode,
            authorizationServer = null
        )

        val offer = Offer(localCredentialOffer)
        val offeredDocument = offer.offeredDocuments.first()

        assertEquals(1, offeredDocument.batchCredentialIssuanceSize)
    }
}

