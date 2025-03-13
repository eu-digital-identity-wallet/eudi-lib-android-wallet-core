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

import eu.europa.ec.eudi.openid4vci.Claim
import eu.europa.ec.eudi.openid4vci.ClaimPath
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.MsoMdocPolicy
import eu.europa.ec.eudi.openid4vci.ProofTypesSupported
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.wallet.document.metadata.DocumentMetaData
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer.OfferedDocument
import eu.europa.ec.eudi.wallet.issue.openid4vci.transformations.extractDocumentMetaData
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.util.Locale
import kotlin.test.assertEquals

class TestDocumentMetaDataTransformations {

    private lateinit var mockedCredentialIssuerMetadata: CredentialIssuerMetadata

    private lateinit var mockCredentialOffer: CredentialOffer

    private lateinit var dummyOffer: Offer
    private var dummyIssuerUrl: String = "https://test.com"
    private var dummyDocumentId: String = "documentId"


    @Before
    fun setup() {
        mockedCredentialIssuerMetadata = mockk<CredentialIssuerMetadata>(relaxed = true) {
            every { display } returns emptyList()
        }

        val credentialIssuerIdentifier = CredentialIssuerId.invoke(dummyIssuerUrl).getOrThrow()

        mockCredentialOffer = CredentialOffer(
            credentialIssuerIdentifier = credentialIssuerIdentifier,
            credentialIssuerMetadata = mockedCredentialIssuerMetadata,
            authorizationServerMetadata = mockk(),
            credentialConfigurationIdentifiers = listOf(mockk<CredentialConfigurationIdentifier>())
        )

        dummyOffer = Offer(mockCredentialOffer)
    }

    @Test
    fun `extractDocumentMetaData from OfferedDocument`() {
        // Given
        val inputCredential = MsoMdocCredential(
            scope = "exampleScope",
            cryptographicBindingMethodsSupported = listOf(),
            credentialSigningAlgorithmsSupported = listOf("ES256"),
            isoCredentialSigningAlgorithmsSupported = listOf(),
            isoCredentialCurvesSupported = listOf(),
            isoPolicy = MsoMdocPolicy(oneTimeUse = true, batchSize = 10),
            proofTypesSupported = ProofTypesSupported.Empty,
            display = listOf(
                Display(
                    name = "Example Display",
                    locale = Locale.forLanguageTag("en"),
                    logo = Display.Logo(
                        uri = URI("https://example.com/logo.png"),
                        alternativeText = "Example Logo"
                    ),
                    description = "A description",
                    backgroundColor = "#FFFFFF",
                    textColor = "#000000"
                )
            ),
            docType = "exampleDocType",
            claims = listOf(
                Claim(
                    path = ClaimPath.claim("namespace1").claim("claim1"),
                    mandatory = true,
                    display = listOf(
                        Claim.Display(
                            name = "Claim 1 Display",
                            locale = Locale.forLanguageTag("en")
                        )
                    )
                )

            )
        )
        val offeredDocument = OfferedDocument(
            offer = dummyOffer,
            configurationIdentifier = CredentialConfigurationIdentifier(dummyDocumentId),
            configuration = inputCredential,
        )
        val expectedMetaData = DocumentMetaData(
            display = listOf(
                DocumentMetaData.Display(
                    name = "Example Display",
                    locale = Locale.forLanguageTag("en"),
                    logo = DocumentMetaData.Logo(
                        uri = URI("https://example.com/logo.png"),
                        alternativeText = "Example Logo"
                    ),
                    description = "A description",
                    backgroundColor = "#FFFFFF",
                    textColor = "#000000"
                )
            ),
            claims = listOf(
                DocumentMetaData.Claim(
                    path = listOf(
                        "namespace1",
                        "claim1",
                    ),
                    mandatory = true,
                    display = listOf(
                        DocumentMetaData.Claim.Display(
                            name = "Claim 1 Display",
                            locale = Locale.forLanguageTag("en")
                        )
                    )
                )
            ),
            issuerDisplay = emptyList(),
            credentialIssuerIdentifier = dummyIssuerUrl,
            documentConfigurationIdentifier = dummyDocumentId
        )

        // When
        val actualMetaData: DocumentMetaData = offeredDocument.extractDocumentMetaData()

        // Then
        assertEquals(expectedMetaData, actualMetaData)
    }

    @Test
    fun `extractDocumentMetaData for SdJwtVcCredential`() {
        // Given
        val inputCredential = SdJwtVcCredential(
            scope = "exampleScope",
            cryptographicBindingMethodsSupported = listOf(),
            credentialSigningAlgorithmsSupported = listOf("RS256"),
            proofTypesSupported = ProofTypesSupported.Empty,
            display = listOf(
                Display(
                    name = "Example SdJwt Display",
                    locale = Locale.forLanguageTag("fr"),
                    description = "French description",
                    backgroundColor = "#EEEEEE",
                    textColor = "#111111"
                )
            ),
            type = "exampleType",
            claims = listOf(
                Claim(
                    path = ClaimPath.claim("claim2"),
                    mandatory = false,
                    display = listOf(
                        Claim.Display(
                            name = "Claim 2 Display",
                            locale = Locale.forLanguageTag("fr")
                        )
                    )
                )
            )
        )

        val offeredDocument = OfferedDocument(
            offer = dummyOffer,
            configurationIdentifier = CredentialConfigurationIdentifier(dummyDocumentId),
            configuration = inputCredential,
        )
        val expectedMetaData = DocumentMetaData(
            display = listOf(
                DocumentMetaData.Display(
                    name = "Example SdJwt Display",
                    locale = Locale.forLanguageTag("fr"),
                    logo = null,
                    description = "French description",
                    backgroundColor = "#EEEEEE",
                    textColor = "#111111"
                )
            ),
            claims = listOf(
                DocumentMetaData.Claim(
                    path = listOf("claim2"),
                    mandatory = false,
                    display = listOf(
                        DocumentMetaData.Claim.Display(
                            name = "Claim 2 Display",
                            locale = Locale.forLanguageTag("fr")
                        )
                    )
                )
            ),
            issuerDisplay = emptyList(),
            credentialIssuerIdentifier = dummyIssuerUrl,
            documentConfigurationIdentifier = dummyDocumentId
        )

        // When
        val actualMetaData = offeredDocument.extractDocumentMetaData()

        // Then
        assertEquals(expectedMetaData, actualMetaData)
    }


    @Test
    fun `extractDocumentMetaData handles null or empty claims`() {
        // Given
        val credentialWithNoClaims = SdJwtVcCredential(
            scope = "exampleScope",
            cryptographicBindingMethodsSupported = listOf(),
            credentialSigningAlgorithmsSupported = listOf("RS256"),
            proofTypesSupported = ProofTypesSupported.Empty,
            display = emptyList(),
            type = "exampleType",
        )

        val expectedMetaData = DocumentMetaData(
            display = emptyList(),
            claims = emptyList(),
            issuerDisplay = emptyList(),
            credentialIssuerIdentifier = dummyIssuerUrl,
            documentConfigurationIdentifier = dummyDocumentId
        )

        val offeredDocument = OfferedDocument(
            offer = dummyOffer,
            configurationIdentifier = CredentialConfigurationIdentifier(dummyDocumentId),
            configuration = credentialWithNoClaims,
        )
        // When
        val actualMetaData = offeredDocument.extractDocumentMetaData()

        // Then
        assertEquals(expectedMetaData, actualMetaData)
    }
}
