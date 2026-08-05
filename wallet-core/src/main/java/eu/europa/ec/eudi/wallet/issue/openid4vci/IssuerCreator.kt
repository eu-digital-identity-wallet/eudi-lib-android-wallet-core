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
import eu.europa.ec.eudi.openid4vci.CIAuthorizationServerMetadata
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.openid4vci.OpenId4VCIConfig
import eu.europa.ec.eudi.openid4vci.DPoPUsage
import eu.europa.ec.eudi.openid4vci.HttpsUrl
import eu.europa.ec.eudi.openid4vci.JwsAlgorithm
import eu.europa.ec.eudi.openid4vci.ParUsage
import eu.europa.ec.eudi.openid4vci.ProofsConfig
import eu.europa.ec.eudi.openid4vci.Signer
import eu.europa.ec.eudi.openid4vci.clientAttestationPOPJWSAlgs
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.DocTypeFilter
import eu.europa.ec.eudi.wallet.issue.openid4vci.CredentialConfigurationFilter.Companion.VctFilter
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.SecureAreaDpopSigner
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.provider.WalletInstanceAttestationProvider
import eu.europa.ec.eudi.wallet.provider.WalletKeyManager
import io.ktor.client.HttpClient
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK
import org.multipaz.crypto.Algorithm
import java.net.URI
import eu.europa.ec.eudi.openid4vci.DPoPConfig as VciDPoPConfig
import eu.europa.ec.eudi.openid4vci.ProvisionDPoPSigner as VciProvisionDPoPSigner

/**
 * Creates an [Issuer] from the given [Offer].
 */
internal class IssuerCreator(
    private val context: Context,
    private val config: OpenId4VciManager.Config,
    private val ktorHttpClientFactory: () -> HttpClient,
    private val walletInstanceAttestationProvider: WalletInstanceAttestationProvider?,
    private val walletAttestationKeyManager: WalletKeyManager,
    private val logger: Logger?,
    private val issuerMetadataPolicy: IssuerMetadataPolicy = IssuerMetadataPolicy.IgnoreSigned,
) {

    internal var clientAttestationPopKeyId: String? = null
        private set

    internal var dpopKeyAlias: String? = null
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
            Issuer.metaData(it, credentialIssuerId, issuerMetadataPolicy)
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
        return try {
            Issuer.makeWalletInitiated(
                config = config.toOpenId4VCIConfig(
                    authorizationServerMetadata,
                    existingDpopKeyAlias
                ),
                credentialIssuerId = credentialIssuerMetadata.credentialIssuerIdentifier,
                credentialConfigurationIdentifiers = credentialConfigurationIdentifiers,
                httpClient = ktorHttpClientFactory()
            ).getOrThrow()
        } catch (e: Throwable) {
            logger?.e(TAG, "Failed to create wallet-initiated issuer", e)
            throw e
        }
    }

    private suspend fun CIAuthorizationServerMetadata.toClientAuthentication(): Result<ClientAuthentication> =
        runCatching {
            val issuerUrl = this.issuer.value
            when (val type = config.clientAuthenticationType) {
                is OpenId4VciManager.ClientAuthenticationType.None -> ClientAuthentication.None(type.clientId)
                is OpenId4VciManager.ClientAuthenticationType.AttestationBased -> {
                    val walletAttestationsProvider = checkNotNull(walletInstanceAttestationProvider) {
                        "WalletInstanceAttestationProvider is required for attestation-based client authentication"
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
                                walletAttestationsProvider.toClientAuthentication(type.clientId).getOrThrow()
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
        // Resolve DPoP usage
        val dPoPUsage = when (dpopConfig) {
            DPopConfig.Disabled -> DPoPUsage.Never

            DPopConfig.Default, is DPopConfig.Custom -> {
                val resolvedConfig = when (dpopConfig) {
                    DPopConfig.Default -> DPopConfig.Default.make(context)
                    is DPopConfig.Custom -> dpopConfig
                    else -> error("unreachable")
                }

                val signingAlg = resolvedConfig.secureArea.supportedAlgorithms
                    .firstOrNull { it.isSigning && it.joseAlgorithmIdentifier != null }
                    ?: throw IllegalStateException("No signing algorithm available for DPoP")

                val provisionDPoPSigner = if (existingDpopKeyAlias != null) {
                    // Re-issuance: reuse existing DPoP key bound to the access token
                    object : VciProvisionDPoPSigner {
                        override val popAlgorithm = JwsAlgorithm(signingAlg.joseAlgorithmIdentifier!!)
                        override suspend fun invoke(authorizationServer: HttpsUrl): Signer<JWK> {
                            return SecureAreaDpopSigner.fromExistingKey(
                                resolvedConfig, existingDpopKeyAlias, logger
                            ).also {
                                dpopKeyAlias = it.keyInfo.alias
                            }
                        }
                    }
                } else {
                    // Normal issuance: create new DPoP key
                    object : VciProvisionDPoPSigner {
                        override val popAlgorithm = JwsAlgorithm(signingAlg.joseAlgorithmIdentifier!!)
                        override suspend fun invoke(authorizationServer: HttpsUrl): Signer<JWK> {
                            return SecureAreaDpopSigner(
                                resolvedConfig, listOf(signingAlg), logger
                            ).also {
                                dpopKeyAlias = it.keyInfo.alias
                            }
                        }
                    }
                }

                DPoPUsage.IfSupported(VciDPoPConfig(provisionDPoPSigner))
            }
        }

        return OpenId4VCIConfig(
            clientAuthentication = auth,
            authFlowRedirectionURI = URI.create(authFlowRedirectionURI),
            encryptionSupportConfig = responseEncryptionConfig,
            supportedCredentialReusePolicies = supportedCredentialReusePolicies,
            dPoPUsage = dPoPUsage,
            parUsage = when (parUsage) {
                OpenId4VciManager.Config.ParUsage.IF_SUPPORTED -> ParUsage.IfSupported()
                OpenId4VciManager.Config.ParUsage.REQUIRED -> ParUsage.Required()
                OpenId4VciManager.Config.ParUsage.NEVER -> ParUsage.Never
                else -> ParUsage.IfSupported()
            },
            proofs = proofTypes.toProofsConfig(),
        )
    }
}

private fun OpenId4VciManager.SupportedProofTypes.toProofsConfig(): ProofsConfig {
    return ProofsConfig(
        isNoProofSupported = isNoProofSupported,
        jwtProof = jwtProofAlgorithms?.let { algs ->
            ProofsConfig.SupportedJwtProof(algs.mapToJWSAlgorithms())
        },
        attestationProof = attestationProofAlgorithms?.let { algs ->
            ProofsConfig.SupportedAttestationProof(algs.mapToJWSAlgorithms())
        },
    )
}

private fun Set<Algorithm>.mapToJWSAlgorithms(): Set<JWSAlgorithm> =
    mapNotNull { it.joseAlgorithmIdentifier }
        .map { JWSAlgorithm.parse(it) }
        .toSet()