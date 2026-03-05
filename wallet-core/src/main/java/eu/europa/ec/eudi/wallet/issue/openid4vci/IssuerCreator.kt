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

import android.content.Context
import com.nimbusds.jose.jwk.Curve
import eu.europa.ec.eudi.openid4vci.CIAuthorizationServerMetadata
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.CredentialResponseEncryptionPolicy
import eu.europa.ec.eudi.openid4vci.EcConfig
import eu.europa.ec.eudi.openid4vci.EncryptionSupportConfig
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.openid4vci.OpenId4VCIConfig
import eu.europa.ec.eudi.openid4vci.ParUsage
import eu.europa.ec.eudi.openid4vci.RsaConfig
import eu.europa.ec.eudi.openid4vci.clientAttestationPOPJWSAlgs
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.DocTypeFilter
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.VctFilter
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopSigner
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.SecureAreaDpopSigner
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.provider.WalletAttestationsProvider
import eu.europa.ec.eudi.wallet.provider.WalletKeyManager
import io.ktor.client.HttpClient
import org.multipaz.crypto.Algorithm
import java.net.URI

/**
 * Creates an [Issuer] from the given [Offer].
 */
internal class IssuerCreator(
    private val context: Context,
    private val config: OpenId4VciManager.Config,
    private val ktorHttpClientFactory: () -> HttpClient,
    private val walletProvider: WalletAttestationsProvider?,
    private val walletAttestationKeyManager: WalletKeyManager,
    private val logger: Logger?,
) {

    internal var clientAttestationPopKeyId: String? = null
        private set

    internal lateinit var dpopKeyAlias: String
        private set

    internal lateinit var clientAuthentication: ClientAuthentication
        private set

    /**
     * Creates an [Issuer] from the given [Offer].
     * @param offer The [Offer].
     * @return The [Issuer].
     */
    suspend fun createIssuer(offer: Offer): Issuer = doCreateIssuer(offer.credentialOffer)

    /**
     * Creates an [Issuer] from the given [CredentialConfigurationIdentifier]s.
     * @param issuerUrl The issuer URL.
     * @param credentialConfigurationIdentifiers The list of [CredentialConfigurationIdentifier]s.
     * @param existingDpopKeyAlias Optional alias of an existing DPoP key to reuse (for re-issuance).
     * @return The [Issuer].
     */
    suspend fun createIssuer(
        issuerUrl: String,
        credentialConfigurationIdentifiers: List<CredentialConfigurationIdentifier>,
        existingDpopKeyAlias: String? = null,
    ): Issuer {

        val (issuerMetadata, authorizationServerMetadata) = CredentialIssuerId(issuerUrl)
            .map { getIssuerMetadata(it) }
            .getOrThrow()

        return doCreateIssuer(
            issuerMetadata, authorizationServerMetadata.first(), credentialConfigurationIdentifiers,
            existingDpopKeyAlias
        )
    }

    /**
     * Creates an [Issuer] from the given [DocumentFormat].
     * This method finds a suitable credential configuration based on the document format and creates an issuer.
     *
     * @param documentFormat The format of the document for which to create an issuer.
     * @return The [Issuer] supporting the given document format.
     * @throws IllegalStateException if no suitable configuration is found for the document format.
     */
    suspend fun createIssuer(issuerUrl: String, documentFormat: DocumentFormat): Issuer {
        val formatFilter = when (documentFormat) {
            is MsoMdocFormat -> DocTypeFilter(documentFormat.docType)
            is SdJwtVcFormat -> VctFilter(documentFormat.vct)
        }
        val (issuerMetadata, authorizationServerMetadata) = CredentialIssuerId(issuerUrl)
            .map { getIssuerMetadata(it) }
            .getOrThrow()

        val configurationId = issuerMetadata.credentialConfigurationsSupported
            .filterValues { conf -> formatFilter(conf) }
            .firstNotNullOfOrNull { (confId, _) -> confId }
            ?: throw IllegalStateException("No suitable configuration found")

        return doCreateIssuer(issuerMetadata, authorizationServerMetadata.first(), listOf(configurationId))
    }


    private suspend fun getIssuerMetadata(credentialIssuerId: CredentialIssuerId): Pair<CredentialIssuerMetadata, List<CIAuthorizationServerMetadata>> {
        return ktorHttpClientFactory().use {
            Issuer.metaData(it, credentialIssuerId, IssuerMetadataPolicy.IgnoreSigned)
        }
    }


    private suspend fun doCreateIssuer(
        credentialOffer: CredentialOffer,
    ): Issuer {
        return Issuer.make(
            config = config.toOpenId4VCIConfig(
                credentialOffer.authorizationServerMetadata,
            ),
            credentialOffer = credentialOffer,
            httpClient = ktorHttpClientFactory()
        ).getOrThrow()
    }

    private suspend fun doCreateIssuer(
        credentialIssuerMetadata: CredentialIssuerMetadata,
        authorizationServerMetadata: CIAuthorizationServerMetadata,
        credentialConfigurationIdentifiers: List<CredentialConfigurationIdentifier>,
        existingDpopKeyAlias: String? = null,
    ): Issuer {
        return Issuer.makeWalletInitiated(
            config = config.toOpenId4VCIConfig(
                authorizationServerMetadata,
                existingDpopKeyAlias
            ),
            credentialIssuerId = credentialIssuerMetadata.credentialIssuerIdentifier,
            credentialConfigurationIdentifiers = credentialConfigurationIdentifiers,
            httpClient = ktorHttpClientFactory()
        ).getOrThrow()
    }

    private suspend fun CIAuthorizationServerMetadata.toClientAuthentication(): Result<ClientAuthentication> =
        runCatching {
            val issuerUrl = this.issuer.value
            when (val type = config.clientAuthenticationType) {
                is OpenId4VciManager.ClientAuthenticationType.None -> ClientAuthentication.None(type.clientId)
                is OpenId4VciManager.ClientAuthenticationType.AttestationBased -> {
                    val walletAttestationsProvider = checkNotNull(walletProvider) {
                        "Wallet attestation provider is not provided in the IssuerCreator for attestation based client authentication"
                    }
                    val clientAttestationPOPJWSAlgs = clientAttestationPOPJWSAlgs
                        .takeUnless { it.isNullOrEmpty() }
                        ?: throw IllegalStateException(
                            "Client attestation based authentication is not supported by the authorization server at ${this.authorizationEndpointURI}"
                        )
                    val supportedAlgorithms = clientAttestationPOPJWSAlgs.map { a ->
                        Algorithm.fromJoseAlgorithmIdentifier(a.name)
                    }
                    walletAttestationKeyManager
                        .getOrCreateWalletAttestationKey(issuerUrl, supportedAlgorithms)
                        .map {
                            clientAttestationPopKeyId = it.keyInfo.alias
                            with(it) {
                                walletAttestationsProvider.toClientAuthentication().getOrThrow()
                            }
                        }.getOrThrow()
                }
            }
        }

    /**
     * Converts the [OpenId4VciManager.Config] to [OpenId4VCIConfig].
     * @receiver The [OpenId4VciManager.Config].
     * @return The [OpenId4VCIConfig].
     */
    private suspend fun OpenId4VciManager.Config.toOpenId4VCIConfig(
        authorizationServerMetadata: CIAuthorizationServerMetadata,
        existingDpopKeyAlias: String? = null,
    ): OpenId4VCIConfig {
        val auth = authorizationServerMetadata.toClientAuthentication().getOrThrow()
        clientAuthentication = auth
        return OpenId4VCIConfig(
            clientAuthentication = auth,
            authFlowRedirectionURI = URI.create(authFlowRedirectionURI),
            encryptionSupportConfig = EncryptionSupportConfig(
                credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED,
                ecConfig = EcConfig(ecKeyCurve = Curve.P_256),
                rsaConfig = RsaConfig(rcaKeySize = 2048)
            ),
            dPoPSigner = if (existingDpopKeyAlias != null) {
                // Re-issuance: reuse existing DPoP key bound to the access token
                val resolvedConfig = when (val cfg = dpopConfig) {
                    DPopConfig.Disabled -> null
                    DPopConfig.Default -> DPopConfig.Default.make(context)
                    is DPopConfig.Custom -> cfg
                }
                resolvedConfig?.let { cfg ->
                    SecureAreaDpopSigner.fromExistingKey(cfg, existingDpopKeyAlias, logger).also {
                        dpopKeyAlias = it.keyInfo.alias
                    }
                }
            } else {
                // Normal issuance: create new DPoP key
                DPopSigner.makeIfSupported(
                    context = context,
                    config = dpopConfig,
                    authorizationServerMetadata = authorizationServerMetadata,
                    logger = logger
                )
                    .onSuccess { signer ->
                        // Track DPoP key alias for re-issuance metadata
                        dpopKeyAlias = (signer as SecureAreaDpopSigner).keyInfo.alias
                    }
                    .getOrElse {
                        logger?.log(
                            Logger.Record(
                                level = Logger.Companion.LEVEL_DEBUG,
                                message = "DPoP not supported: ${it.message}",
                                sourceClassName = "eu.europa.ec.eudi.wallet.issue.openid4vci.IssuerCreator",
                                sourceMethod = "toOpenId4VCIConfig"
                            )
                        )
                        null
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