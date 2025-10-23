package eu.europa.ec.eudi.wallet.issue.openid4vci.transformations

import eu.europa.ec.eudi.openid4vci.Claim
import eu.europa.ec.eudi.openid4vci.ClaimPath
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.MsoMdocPolicy
import eu.europa.ec.eudi.openid4vci.ProofTypesSupported
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.util.Locale
import kotlin.test.assertEquals

class IssuerMetadataTransformationsTest {

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

        val credentialIssuerIdentifier = CredentialIssuerId.Companion.invoke(dummyIssuerUrl).getOrThrow()

        mockCredentialOffer = CredentialOffer(
            credentialIssuerIdentifier = credentialIssuerIdentifier,
            credentialIssuerMetadata = mockedCredentialIssuerMetadata,
            authorizationServerMetadata = mockk(),
            credentialConfigurationIdentifiers = listOf(mockk<CredentialConfigurationIdentifier>())
        )

        dummyOffer = Offer(mockCredentialOffer)
    }

    @Test
    fun `extractIssuerMetaData from OfferedDocument`() {
        // Given
        val inputCredential = MsoMdocCredential(
            scope = "exampleScope",
            cryptographicBindingMethodsSupported = listOf(),
            credentialSigningAlgorithmsSupported = listOf("ES256"),
            isoCredentialSigningAlgorithmsSupported = listOf(),
            isoCredentialCurvesSupported = listOf(),
            isoPolicy = MsoMdocPolicy(oneTimeUse = true, batchSize = 10),
            proofTypesSupported = ProofTypesSupported.Companion.Empty,
            docType = "exampleDocType",
            credentialMetadata = CredentialMetadata(
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
                claims = listOf(
                    Claim(
                        path = ClaimPath.Companion.claim("namespace1").claim("claim1"),
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
        )
        val offeredDocument = Offer.OfferedDocument(
            offer = dummyOffer,
            configurationIdentifier = CredentialConfigurationIdentifier(dummyDocumentId),
            configuration = inputCredential,
        )
        val expectedMetaData = IssuerMetadata(
            display = listOf(
                IssuerMetadata.Display(
                    name = "Example Display",
                    locale = Locale.forLanguageTag("en"),
                    logo = IssuerMetadata.Logo(
                        uri = URI("https://example.com/logo.png"),
                        alternativeText = "Example Logo"
                    ),
                    description = "A description",
                    backgroundColor = "#FFFFFF",
                    textColor = "#000000"
                )
            ),
            claims = listOf(
                IssuerMetadata.Claim(
                    path = listOf(
                        "namespace1",
                        "claim1",
                    ),
                    mandatory = true,
                    display = listOf(
                        IssuerMetadata.Claim.Display(
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
        val actualMetaData: IssuerMetadata = offeredDocument.extractIssuerMetadata()

        // Then
        assertEquals(expectedMetaData, actualMetaData)
    }

    @Test
    fun `extractIssuerMetaData for SdJwtVcCredential`() {
        // Given
        val inputCredential = SdJwtVcCredential(
            scope = "exampleScope",
            cryptographicBindingMethodsSupported = listOf(),
            credentialSigningAlgorithmsSupported = listOf("RS256"),
            proofTypesSupported = ProofTypesSupported.Companion.Empty,
            type = "exampleType",
            credentialMetadata = CredentialMetadata(
                display = listOf(
                    Display(
                        name = "Example SdJwt Display",
                        locale = Locale.forLanguageTag("fr"),
                        description = "French description",
                        backgroundColor = "#EEEEEE",
                        textColor = "#111111"
                    )
                ),

                claims = listOf(
                    Claim(
                        path = ClaimPath.Companion.claim("claim2"),
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
        )

        val offeredDocument = Offer.OfferedDocument(
            offer = dummyOffer,
            configurationIdentifier = CredentialConfigurationIdentifier(dummyDocumentId),
            configuration = inputCredential,
        )
        val expectedMetaData = IssuerMetadata(
            display = listOf(
                IssuerMetadata.Display(
                    name = "Example SdJwt Display",
                    locale = Locale.forLanguageTag("fr"),
                    logo = null,
                    description = "French description",
                    backgroundColor = "#EEEEEE",
                    textColor = "#111111"
                )
            ),
            claims = listOf(
                IssuerMetadata.Claim(
                    path = listOf("claim2"),
                    mandatory = false,
                    display = listOf(
                        IssuerMetadata.Claim.Display(
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
        val actualMetaData = offeredDocument.extractIssuerMetadata()

        // Then
        assertEquals(expectedMetaData, actualMetaData)
    }


    @Test
    fun `extractIssuerMetaData handles null or empty claims`() {
        // Given
        val credentialWithNoClaims = SdJwtVcCredential(
            scope = "exampleScope",
            cryptographicBindingMethodsSupported = listOf(),
            credentialSigningAlgorithmsSupported = listOf("RS256"),
            proofTypesSupported = ProofTypesSupported.Companion.Empty,
            type = "exampleType",
            credentialMetadata = CredentialMetadata(
                display = emptyList(),
            )
        )

        val expectedMetaData = IssuerMetadata(
            display = emptyList(),
            claims = emptyList(),
            issuerDisplay = emptyList(),
            credentialIssuerIdentifier = dummyIssuerUrl,
            documentConfigurationIdentifier = dummyDocumentId
        )

        val offeredDocument = Offer.OfferedDocument(
            offer = dummyOffer,
            configurationIdentifier = CredentialConfigurationIdentifier(dummyDocumentId),
            configuration = credentialWithNoClaims,
        )
        // When
        val actualMetaData = offeredDocument.extractIssuerMetadata()

        // Then
        assertEquals(expectedMetaData, actualMetaData)
    }
}