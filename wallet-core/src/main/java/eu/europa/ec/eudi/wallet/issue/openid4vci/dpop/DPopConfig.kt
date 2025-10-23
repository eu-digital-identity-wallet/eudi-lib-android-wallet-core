package eu.europa.ec.eudi.wallet.issue.openid4vci.dpop

import java.io.File
import java.security.SecureRandom
import kotlinx.io.bytestring.ByteString
import org.multipaz.context.initializeApplication
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.AndroidKeystoreCreateKeySettings
import org.multipaz.securearea.AndroidKeystoreSecureArea
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.UserAuthenticationType
import org.multipaz.storage.android.AndroidStorage
import kotlin.time.Duration

/**
 * Configuration for DPoP (Demonstrating Proof-of-Possession) in OpenID4VCI credential issuance.
 *
 * DPoP is a security mechanism that binds OAuth 2.0 access tokens to cryptographic keys,
 * preventing token theft and replay attacks. This sealed interface defines the available
 * DPoP configuration options for credential issuance flows.
 *
 * ## Available Configurations
 *
 * - [Disabled] - DPoP is not used
 * - [Default] - Uses Android Keystore with default settings
 * - [Custom] - Uses a custom secure area with custom key settings
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Disable DPoP
 * val config = DPopConfig.Disabled
 *
 * // Use default configuration
 * val config = DPopConfig.Default
 *
 * // Use custom secure area
 * val config = DPopConfig.Custom(
 *     secureArea = mySecureArea,
 *     createKeySettingsBuilder = { algorithms ->
 *         MyCreateKeySettings(algorithms.first())
 *     }
 * )
 * ```
 *
 * @see Disabled
 * @see Default
 * @see Custom
 */
sealed interface DPopConfig {

    /**
     * DPoP is disabled.
     *
     * When this configuration is used, no DPoP proofs are generated or sent during
     * credential issuance. Access tokens will not be bound to cryptographic keys.
     *
     * Use this when:
     * - The authorization server does not support DPoP
     * - DPoP is not required for your use case
     * - Testing without DPoP protection
     */
    data object Disabled : DPopConfig

    /**
     * Default DPoP configuration using Android Keystore.
     *
     * This configuration automatically sets up DPoP with sensible defaults:
     * - Keys stored in Android Keystore
     * - Storage in app's no-backup files directory
     * - StrongBox support when available
     * - No user authentication required for key usage
     * - Algorithm automatically negotiated with the server
     *
     * The actual configuration is created lazily when needed by calling [make].
     *
     * ## Example
     *
     * ```kotlin
     * val openId4VciConfig = OpenId4VciManager.Config.Builder()
     *     .withIssuerUrl("https://issuer.com")
     *     .withClientId("client-id")
     *     .withAuthFlowRedirectionURI("app://callback")
     *     .withDPopConfig(DPopConfig.Default)
     *     .build()
     * ```
     */
    data object Default : DPopConfig {
        /**
         * Creates a [Custom] DPoP configuration with default Android Keystore settings.
         *
         * This internal function is called automatically when the Default configuration is used.
         * It initializes:
         * - Android Keystore secure area for key storage
         * - Storage in the no-backup directory (not backed up to cloud)
         * - StrongBox support if the device supports it
         * - Random attestation challenge if not provided
         * - Key settings builder that creates keys with the negotiated algorithm
         * - Key unlock data provider set to [KeyUnlockDataProvider.None] (no unlock required)
         *
         * @param context The Android context for initializing storage and secure area
         * @param attestationChallenge Optional 16-byte challenge for key attestation.
         *        If null, a random challenge is generated.
         * @return A [Custom] configuration with Android Keystore secure area and default settings
         */
        internal suspend fun make(
            context: android.content.Context,
            attestationChallenge: ByteArray? = null
        ): Custom {
            initializeApplication(context)
            val storage = AndroidStorage(
                databasePath = File(context.noBackupFilesDir, "dpop_metadata").path
            )
            val capabilities = AndroidKeystoreSecureArea.Capabilities()
            val secureArea = AndroidKeystoreSecureArea.create(storage = storage)
            val attestationChallengeBytes = ByteString(attestationChallenge ?: ByteArray(16).also {
                SecureRandom().nextBytes(it)
            })

            return Custom(
                secureArea = secureArea,
                createKeySettingsBuilder = { algorithms ->
                    val algorithm = algorithms.firstOrNull { it.isSigning }
                    requireNotNull(algorithm) { "No suitable signing algorithm found for dPop" }
                    AndroidKeystoreCreateKeySettings.Builder(attestationChallengeBytes)
                        .setAlgorithm(algorithm)
                        .setUserAuthenticationRequired(
                            false, Duration.ZERO, setOf(
                                UserAuthenticationType.BIOMETRIC,
                                UserAuthenticationType.LSKF
                            )
                        )
                        .setUseStrongBox(capabilities.strongBoxSupported)
                        .build()
                },
                keyUnlockDataProvider = KeyUnlockDataProvider.None
            )
        }
    }

    /**
     * Custom DPoP configuration using a specific secure area.
     *
     * This configuration allows full control over how DPoP keys are created and stored.
     * The algorithms for key creation are automatically selected based on what both the
     * authorization server and the secure area support.
     *
     * ## How It Works
     *
     * 1. The library checks the authorization server's supported DPoP algorithms
     * 2. It finds all algorithms supported by both server and [secureArea]
     * 3. The list of matched algorithms is passed to [createKeySettingsBuilder]
     * 4. Your builder function selects an appropriate algorithm from the list
     * 5. A DPoP key is created using the returned settings
     * 6. The key is used to sign all DPoP proofs during issuance
     *
     * ## Example with Custom Secure Area
     *
     * ```kotlin
     * val customConfig = DPopConfig.Custom(
     *     secureArea = myCloudSecureArea,
     *     createKeySettingsBuilder = { algorithms ->
     *         val algorithm = algorithms.first() // Select the first supported algorithm
     *         CloudKeySettings.Builder()
     *             .setAlgorithm(algorithm)
     *             .setKeyName("dpop_key")
     *             .setRequireUserAuth(true)
     *             .build()
     *     }
     * )
     * ```
     *
     * ## Example with Android Keystore
     *
     * ```kotlin
     * val androidConfig = DPopConfig.Custom(
     *     secureArea = AndroidKeystoreSecureArea.create(storage),
     *     createKeySettingsBuilder = { algorithms ->
     *         // Select the first signing algorithm from the list
     *         val algorithm = algorithms.firstOrNull { it.isSigning }
     *             ?: throw IllegalStateException("No signing algorithm available")
     *         AndroidKeystoreCreateKeySettings.Builder(attestationChallenge)
     *             .setAlgorithm(algorithm) // e.g., ES256, ES384, ES512
     *             .setUserAuthenticationRequired(true, 30_000, setOf(
     *                 UserAuthenticationType.BIOMETRIC
     *             ))
     *             .setUseStrongBox(true)
     *             .build()
     *     }
     * )
     * ```
     *
     * @property secureArea The secure area where DPoP keys are stored and managed.
     *           Must support at least one signing algorithm (e.g., ES256, ES384, ES512).
     * @property createKeySettingsBuilder A function that creates [CreateKeySettings] from a list of
     *           supported algorithms. The algorithms parameter contains all algorithms that are
     *           supported by both the authorization server and the secure area. Your implementation
     *           should select an appropriate algorithm from this list and return valid settings for
     *           creating a key with that algorithm.
     * @property keyUnlockDataProvider A [KeyUnlockDataProvider] that provides [KeyUnlockData] for unlocking
     *           the DPoP key when performing signing operations. This is invoked each time a DPoP proof
     *           needs to be signed. The provider receives the key alias and secure area as parameters
     *           and should return the appropriate unlock data, or null if no unlock is required.
     *
     *           **Default value:** [KeyUnlockDataProvider.None] - Returns null (no unlock data required).
     *
     *           **When to customize:** Provide a custom implementation when your keys require user
     *           authentication (e.g., biometric or PIN). The provider should prompt the user for
     *           authentication and return the unlock data upon successful authentication.
     *
     *           **Example with custom provider:**
     *           ```kotlin
     *           val config = DPopConfig.Custom(
     *               secureArea = mySecureArea,
     *               createKeySettingsBuilder = { algorithms -> /* ... */ },
     *               keyUnlockDataProvider = KeyUnlockDataProvider { keyAlias, secureArea ->
     *                   // Prompt user for biometric authentication
     *                   promptForBiometric()?.let { authResult ->
     *                       AndroidKeystoreKeyUnlockData(authResult.cryptoObject)
     *                   }
     *               }
     *           )
     *           ```
     */
    data class Custom(
        val secureArea: SecureArea,
        val createKeySettingsBuilder: (List<Algorithm>) -> CreateKeySettings,
        val keyUnlockDataProvider: KeyUnlockDataProvider = KeyUnlockDataProvider.None
    ) : DPopConfig
}

/**
 * Functional interface for providing unlock data for DPoP keys stored in a secure area.
 *
 * This interface is used to obtain [KeyUnlockData] when a DPoP key needs to be accessed
 * for signing operations. Implementations can provide user authentication (e.g., biometric
 * or PIN) when required by the key's security settings.
 *
 * ## Usage
 *
 * The provider is invoked each time a DPoP proof JWT needs to be signed. If the key
 * requires user authentication, the implementation should:
 * 1. Prompt the user for authentication (e.g., show biometric prompt)
 * 2. Wait for successful authentication
 * 3. Return the unlock data obtained from the authentication result
 * 4. Return null if no authentication is required
 *
 * ## Predefined Providers
 *
 * - [None] - Returns null, suitable for keys that don't require authentication
 *
 * ## Examples
 *
 * ```kotlin
 * // No authentication required
 * val provider = KeyUnlockDataProvider.None
 *
 * // Custom authentication with biometric prompt
 * val provider = KeyUnlockDataProvider { keyAlias, secureArea ->
 *     withContext(Dispatchers.Main) {
 *         val promptInfo = BiometricPrompt.PromptInfo.Builder()
 *             .setTitle("Authenticate for DPoP")
 *             .setSubtitle("Sign credential request")
 *             .setNegativeButtonText("Cancel")
 *             .build()
 *
 *         val result = showBiometricPrompt(promptInfo)
 *         result?.let { AndroidKeystoreKeyUnlockData(it.cryptoObject) }
 *     }
 * }
 *
 * // Use in configuration
 * val config = DPopConfig.Custom(
 *     secureArea = secureArea,
 *     createKeySettingsBuilder = { algorithms -> /* ... */ },
 *     keyUnlockDataProvider = provider
 * )
 * ```
 *
 * @see KeyUnlockData
 * @see DPopConfig.Custom
 */
fun interface KeyUnlockDataProvider {
    suspend operator fun invoke(keyAlias: String, secureArea: SecureArea): KeyUnlockData?

    companion object {
        val None = KeyUnlockDataProvider { _, _ -> null }
    }
}
