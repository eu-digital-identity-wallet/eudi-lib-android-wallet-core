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
import com.nimbusds.jose.jwk.Curve
import eu.europa.ec.eudi.openid4vci.CIAuthorizationServerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialResponseEncryptionPolicy
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.openid4vci.KeyGenerationConfig
import eu.europa.ec.eudi.openid4vci.OpenId4VCIConfig
import eu.europa.ec.eudi.openid4vci.ParUsage
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.DocTypeFilter
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.VctFilter
import eu.europa.ec.eudi.wallet.logging.Logger
import io.ktor.client.HttpClient
import java.net.URI

/**
 * Creates an [Issuer] from the given [Offer].
 */
internal class IssuerCreator(
    private val config: OpenId4VciManager.Config,
    private val ktorHttpClientFactory: () -> HttpClient,
    private val logger: Logger?
) {

    private var issuerMetadata: Pair<CredentialIssuerMetadata, List<CIAuthorizationServerMetadata>>? =
        null


    /**
     * Creates an [Issuer] from the given [Offer].
     * @param offer The [Offer].
     * @return The [Issuer].
     */
    suspend fun createIssuer(offer: Offer): Issuer {
        val credentialOffer = offer.credentialOffer
        return Issuer.make(
            config = config.toOpenId4VCIConfig(),
            credentialOffer = credentialOffer,
            httpClient = ktorHttpClientFactory()
        ).getOrThrow()
    }

    /**
     * Creates an [Issuer] from the given [CredentialConfigurationIdentifier]s.
     * @param credentialConfigurationIdentifiers The [CredentialConfigurationIdentifier]s.
     * @return The [Issuer].
     */
    suspend fun createIssuer(credentialConfigurationIdentifiers: List<CredentialConfigurationIdentifier>): Issuer {
        return Issuer.makeWalletInitiated(
            config = config.toOpenId4VCIConfig(),
            credentialIssuerId = CredentialIssuerId(config.issuerUrl).getOrThrow(),
            credentialConfigurationIdentifiers = credentialConfigurationIdentifiers,
            httpClient = ktorHttpClientFactory()
        ).getOrThrow()
    }

    /**
     * Creates an [Issuer] from the given [DocumentFormat].
     * This method finds a suitable credential configuration based on the document format and creates an issuer.
     *
     * @param documentFormat The format of the document for which to create an issuer.
     * @return The [Issuer] supporting the given document format.
     * @throws IllegalStateException if no suitable configuration is found for the document format.
     */
    suspend fun createIssuer(documentFormat: DocumentFormat): Issuer {
        val formatFilter = when (documentFormat) {
            is MsoMdocFormat -> DocTypeFilter(documentFormat.docType)
            is SdJwtVcFormat -> VctFilter(documentFormat.vct)
        }
        val configurationId = getCredentialIssuerMetadata()
            .credentialConfigurationsSupported
            .filterValues { conf -> formatFilter(conf) }
            .firstNotNullOfOrNull { (confId, _) -> confId }
            ?: throw IllegalStateException("No suitable configuration found")

        return createIssuer(listOf(configurationId))
    }

    /**
     * Loads the issuer metadata from the configured issuer URL if not already loaded.
     *
     * @return A pair containing the credential issuer metadata and a list of authorization server metadata.
     * @throws Exception if the metadata cannot be retrieved.
     */
    private suspend fun loadIssuerMetadata(): Pair<CredentialIssuerMetadata, List<CIAuthorizationServerMetadata>> {
        return issuerMetadata ?: CredentialIssuerId(config.issuerUrl)
            .mapCatching { createIssuerId ->
                ktorHttpClientFactory().use { client ->
                    Issuer.metaData(
                        httpClient = client,
                        credentialIssuerId = createIssuerId,
                        policy = IssuerMetadataPolicy.IgnoreSigned
                    )
                }
            }.onSuccess {
                issuerMetadata = it
            }.getOrThrow()
    }

    /**
     * Retrieves the credential issuer metadata from the loaded issuer metadata.
     *
     * @return The credential issuer metadata.
     */
    private suspend fun getCredentialIssuerMetadata(): CredentialIssuerMetadata {
        return loadIssuerMetadata().first
    }

    /**
     * Retrieves the authorization server metadata from the loaded issuer metadata.
     *
     * @return A list of authorization server metadata objects.
     */
    private suspend fun getAuthorizationServerMetadata(): List<CIAuthorizationServerMetadata> {
        return loadIssuerMetadata().second
    }

    /**
     * Verifies that the specified DPoP algorithm is supported by the authorization server.
     *
     * @param dPoPUsage The DPoP usage configuration with the algorithm to check.
     * @throws IllegalStateException if the specified algorithm is not supported by the issuer.
     */
    private suspend fun ensureDPoPAlgorithmSupported(dPoPUsage: OpenId4VciManager.Config.DPoPUsage.IfSupported) {
        check(
            getAuthorizationServerMetadata().first()
                .dPoPJWSAlgs
                .contains(JWSAlgorithm.parse(dPoPUsage.algorithm.joseAlgorithmIdentifier))
        ) {

            "DPoP algorithm ${dPoPUsage.algorithm.joseAlgorithmIdentifier} is not supported by the issuer"
                .also {
                    logger?.log(
                        Logger.Record(
                            level = Logger.Companion.LEVEL_ERROR,
                            message = it,
                            sourceClassName = "eu.europa.ec.eudi.wallet.issue.openid4vci.IssuerCreator",
                            sourceMethod = "ensureDPoPAlgorithmSupported"
                        )
                    )
                }
        }
    }

    /**
     * Converts the [OpenId4VciManager.Config] to [OpenId4VCIConfig].
     * @receiver The [OpenId4VciManager.Config].
     * @return The [OpenId4VCIConfig].
     */
    private suspend fun OpenId4VciManager.Config.toOpenId4VCIConfig(): OpenId4VCIConfig {
        return OpenId4VCIConfig(
            clientId = clientId,
            authFlowRedirectionURI = URI.create(authFlowRedirectionURI),
            keyGenerationConfig = KeyGenerationConfig(Curve.P_256, 2048),
            credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED,
            dPoPSigner = when (dPoPUsage) {
                OpenId4VciManager.Config.DPoPUsage.Disabled -> null

                is OpenId4VciManager.Config.DPoPUsage.IfSupported -> {
                    ensureDPoPAlgorithmSupported(dPoPUsage)
                    DPoPSigner(dPoPUsage.algorithm, logger).getOrNull()
                }
            },
            parUsage = when (parUsage) {
                OpenId4VciManager.Config.ParUsage.IF_SUPPORTED -> ParUsage.IfSupported
                OpenId4VciManager.Config.ParUsage.REQUIRED -> ParUsage.Required
                OpenId4VciManager.Config.ParUsage.NEVER -> ParUsage.Never
                else -> ParUsage.IfSupported
            }
        )
    }
}