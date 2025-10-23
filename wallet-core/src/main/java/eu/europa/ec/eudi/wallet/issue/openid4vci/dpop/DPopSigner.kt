package eu.europa.ec.eudi.wallet.issue.openid4vci.dpop

import android.content.Context
import com.nimbusds.jose.jwk.JWK
import eu.europa.ec.eudi.openid4vci.CIAuthorizationServerMetadata
import eu.europa.ec.eudi.openid4vci.Signer
import eu.europa.ec.eudi.wallet.logging.Logger

/**
 * Signer interface for DPoP (Demonstrating Proof-of-Possession) in OpenID4VCI flows.
 *
 * DPoP is a security mechanism that binds OAuth 2.0 access tokens to cryptographic keys,
 * preventing token theft and replay attacks. This interface extends the [Signer] interface
 * with [JWK] (JSON Web Key) as the public key material type.
 *
 * ## How DPoP Works
 *
 * 1. A cryptographic key pair is created in a secure area
 * 2. For each token request, a DPoP proof JWT is generated and signed with the private key
 * 3. The proof includes the public key (JWK), request details, and a timestamp
 * 4. The authorization server validates the proof and binds the access token to the public key
 * 5. The same key must be used for all subsequent requests with that token
 *
 * ## Creating a DPoP Signer
 *
 * Use the [eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopSigner.Companion.makeIfSupported] factory method to create a DPoP signer:
 *
 * ```kotlin
 * val result = DPopSigner.makeIfSupported(
 *     context = context,
 *     config = DPopConfig.Default,
 *     authorizationServerMetadata = serverMetadata,
 *     logger = logger
 * )
 *
 * result.onSuccess { signer ->
 *     // Use the signer for DPoP operations
 * }.onFailure { error ->
 *     // Handle error (e.g., DPoP not supported)
 * }
 * ```
 *
 * @see Signer
 * @see JWK
 * @see DPopConfig
 * @see makeIfSupported
 */
interface DPopSigner : Signer<JWK> {

    companion object {
        /**
         * Creates a [DPopSigner] instance if DPoP is supported by both the configuration
         * and the authorization server.
         *
         * This factory method performs the following operations:
         *
         * 1. **Configuration Resolution**: Resolves the DPoP configuration
         *    - If [DPopConfig.Disabled], fails with an error
         *    - If [DPopConfig.Default], creates default Android Keystore configuration
         *    - If [DPopConfig.Custom], uses the provided configuration
         *
         * 2. **Algorithm Negotiation**: Finds common signing algorithms
         *    - Retrieves signing algorithms supported by the secure area
         *    - Compares with algorithms advertised by the authorization server
         *    - Collects all matching algorithms into a list
         *
         * 3. **Signer Creation**: Creates a DPoP signer with the matched algorithms
         *    - Uses the secure area from the configuration
         *    - Passes the list of matched algorithms to the signer
         *    - The signer will use the configuration's builder to create keys as needed
         *    - Returns a configured [SecureAreaDpopSigner]
         *
         * ## Success Conditions
         *
         * The method succeeds when:
         * - DPoP is enabled in the configuration
         * - The authorization server advertises DPoP support (non-empty `dPoPJWSAlgs`)
         * - At least one algorithm is supported by both the server and secure area
         *
         * ## Failure Conditions
         *
         * The method returns [Result.failure] with [IllegalStateException] when:
         * - [config] is [DPopConfig.Disabled]
         * - [authorizationServerMetadata] doesn't contain any DPoP algorithms
         * - No common algorithm exists between server and secure area
         *
         * ## Example Usage
         *
         * ```kotlin
         * // With default configuration
         * val signer = DPopSigner.makeIfSupported(
         *     context = applicationContext,
         *     config = DPopConfig.Default,
         *     authorizationServerMetadata = metadata
         * ).getOrThrow()
         *
         * // With custom configuration
         * val customConfig = DPopConfig.Custom(
         *     secureArea = mySecureArea,
         *     createKeySettingsBuilder = { algorithms ->
         *         val algorithm = algorithms.first()
         *         MyKeySettings(algorithm)
         *     },
         *     keyUnlockDataProvider = { keyAlias, secureArea ->
         *         // Provide unlock data if needed
         *         null
         *     }
         * )
         *
         * val signer = DPopSigner.makeIfSupported(
         *     context = applicationContext,
         *     config = customConfig,
         *     authorizationServerMetadata = metadata,
         *     logger = myLogger
         * ).getOrThrow()
         * ```
         *
         * @param context The Android context, required when using [DPopConfig.Default]
         *        to initialize the secure area and storage
         * @param config The DPoP configuration specifying how keys should be created and stored.
         *        Use [DPopConfig.Default] for standard Android Keystore setup,
         *        [DPopConfig.Custom] for custom secure areas, or [DPopConfig.Disabled] to opt out
         *        (which will cause this method to fail)
         * @param authorizationServerMetadata The authorization server's metadata obtained from
         *        the OpenID4VCI issuer. Must contain the `dpop_signing_alg_values_supported`
         *        field with at least one algorithm
         * @param logger Optional logger for debugging. If provided, it's passed to the
         *        [SecureAreaDpopSigner] to log DPoP key creation details
         * @return A [Result] containing:
         *         - [Result.success] with a [DPopSigner] instance if DPoP is supported
         *         - [Result.failure] with [IllegalStateException] if DPoP is disabled,
         *           not supported by the server, or no common algorithm is found
         */
        internal suspend fun makeIfSupported(
            context: Context,
            config: DPopConfig,
            authorizationServerMetadata: CIAuthorizationServerMetadata,
            logger: Logger? = null
        ): Result<DPopSigner> = runCatching {

            val config = when (config) {
                DPopConfig.Disabled -> throw IllegalStateException("DPoP is disabled in the configuration")
                DPopConfig.Default -> DPopConfig.Default.make(context)
                is DPopConfig.Custom -> config
            }

            val supportedAlgorithms = config.secureArea.supportedAlgorithms
                .filter { it.joseAlgorithmIdentifier != null }
                .filter { it.isSigning }
                .associateBy { it.joseAlgorithmIdentifier }

            val serverAlgorithms = authorizationServerMetadata.dPoPJWSAlgs
                ?.map { it.name }
                ?.toList()

            requireNotNull(serverAlgorithms) {
                "Authorization server metadata does not contain dpop_signing_alg_values_supported field"
            }
            require(serverAlgorithms.isNotEmpty()) {
                "Authorization server does not support DPoP: dpop_signing_alg_values_supported is empty"
            }

            val matchedAlgorithms = supportedAlgorithms
                .filterKeys { it in serverAlgorithms }
                .values
                .toList()

            require(matchedAlgorithms.isNotEmpty()) {
                "No common algorithm found for DPoP. Server algorithms: $serverAlgorithms, " +
                        "supported algorithms: ${supportedAlgorithms.keys}"
            }

            SecureAreaDpopSigner(config, matchedAlgorithms, logger)

        }
    }
}
