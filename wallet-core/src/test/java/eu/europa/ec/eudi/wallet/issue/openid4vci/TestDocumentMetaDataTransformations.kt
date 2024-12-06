package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.Claim
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.MsoMdocPolicy
import eu.europa.ec.eudi.openid4vci.ProofTypesSupported
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.wallet.document.metadata.DocumentMetaData
import eu.europa.ec.eudi.wallet.issue.openid4vci.transformations.extractDocumentMetaData
import org.junit.Test
import java.net.URI
import java.util.Locale
import kotlin.test.assertEquals

class TestDocumentMetaDataTransformations {

    @Test
    fun `extractDocumentMetaData for MsoMdocCredential`() {
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
            claims = mapOf(
                "namespace1" to mapOf(
                    "claim1" to Claim(
                        mandatory = true,
                        valueType = "string",
                        display = listOf(
                            Claim.Display(
                                name = "Claim 1 Display",
                                locale = Locale.forLanguageTag("en")
                            )
                        )
                    )
                )
            ),
            order = listOf("claim1")
        )

        val expectedMetaData = DocumentMetaData(
            display = listOf(
                DocumentMetaData.Display(
                    name = "Example Display",
                    locale = Locale.forLanguageTag("en"),
                    logo = DocumentMetaData.Display.Logo(
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
                    name = DocumentMetaData.Claim.Name.MsoMdoc(
                        name = "claim1",
                        nameSpace = "namespace1"
                    ),
                    mandatory = true,
                    valueType = "string",
                    display = listOf(
                        DocumentMetaData.Claim.Display(
                            name = "Claim 1 Display",
                            locale = Locale.forLanguageTag("en")
                        )
                    )
                )
            )
        )

        // When
        val actualMetaData = inputCredential.extractDocumentMetaData()

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
            claims = mapOf(
                "claim2" to Claim(
                    mandatory = false,
                    valueType = "integer",
                    display = listOf(
                        Claim.Display(
                            name = "Claim 2 Display",
                            locale = Locale.forLanguageTag("fr")
                        )
                    )
                )
            )
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
                    name = DocumentMetaData.Claim.Name.SdJwtVc(
                        name = "claim2"
                    ),
                    mandatory = false,
                    valueType = "integer",
                    display = listOf(
                        DocumentMetaData.Claim.Display(
                            name = "Claim 2 Display",
                            locale = Locale.forLanguageTag("fr")
                        )
                    )
                )
            )
        )

        // When
        val actualMetaData = inputCredential.extractDocumentMetaData()

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
            claims = null
        )

        val expectedMetaData = DocumentMetaData(
            display = emptyList(),
            claims = null
        )

        // When
        val actualMetaData = credentialWithNoClaims.extractDocumentMetaData()

        // Then
        assertEquals(expectedMetaData, actualMetaData)
    }
}
