package eu.europa.ec.eudi.wallet.issue.openid4vci.dpop

import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig.Default.make
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
import java.io.File
import java.security.SecureRandom

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
 *     createKeySettingsBuilder = { algorithm ->
 *         MyCreateKeySettings(algorithm)
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
                createKeySettingsBuilder = { algorithm ->
                    require(algorithm.isSigning) { "Algorithm must be a signing algorithm" }
                    AndroidKeystoreCreateKeySettings.Builder(attestationChallengeBytes)
                        .setAlgorithm(algorithm)
                        .setUserAuthenticationRequired(
                            false, 0, setOf(
                                UserAuthenticationType.BIOMETRIC,
                                UserAuthenticationType.LSKF
                            )
                        )
                        .setUseStrongBox(capabilities.strongBoxSupported)
                        .build()
                }
            )
        }
    }

    /**
     * Custom DPoP configuration using a specific secure area.
     *
     * This configuration allows full control over how DPoP keys are created and stored.
     * The algorithm for key creation is automatically selected based on what both the
     * authorization server and the secure area support.
     *
     * ## How It Works
     *
     * 1. The library checks the authorization server's supported DPoP algorithms
     * 2. It finds the first algorithm supported by both server and [secureArea]
     * 3. The matched algorithm is passed to [createKeySettingsBuilder]
     * 4. A DPoP key is created using the returned settings
     * 5. The key is used to sign all DPoP proofs during issuance
     *
     * ## Example with Custom Secure Area
     *
     * ```kotlin
     * val customConfig = DPopConfig.Custom(
     *     secureArea = myCloudSecureArea,
     *     createKeySettingsBuilder = { algorithm ->
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
     *     createKeySettingsBuilder = { algorithm ->
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
     * ## Example with User Authentication
     *
     * ```kotlin
     * val authConfig = DPopConfig.Custom(
     *     secureArea = AndroidKeystoreSecureArea.create(storage),
     *     createKeySettingsBuilder = { algorithm ->
     *         AndroidKeystoreCreateKeySettings.Builder(attestationChallenge)
     *             .setAlgorithm(algorithm)
     *             .setUserAuthenticationRequired(true, 30_000, setOf(
     *                 UserAuthenticationType.BIOMETRIC
     *             ))
     *             .build()
     *     },
     *     unlockKey = { keyAlias, secureArea ->
     *         // Prompt user for biometric authentication
     *         val cryptoObject = createCryptoObjectForKey(keyAlias, secureArea)
     *         biometricPrompt.authenticate(cryptoObject)
     *         // Return the unlock data after successful authentication
     *         AndroidKeystoreKeyUnlockData(cryptoObject)
     *     }
     * )
     * ```
     *
     * @property secureArea The secure area where DPoP keys are stored and managed.
     *           Must support at least one signing algorithm (e.g., ES256, ES384, ES512).
     * @property createKeySettingsBuilder A function that creates [CreateKeySettings] for a given
     *           [Algorithm]. The algorithm parameter is the one negotiated between the server
     *           and secure area capabilities. This function must return valid settings for
     *           creating a key with that algorithm.
     * @property unlockKey A suspend function that provides [KeyUnlockData] for unlocking the DPoP key
     *           when performing signing operations. This is invoked each time a DPoP proof needs to
     *           be signed. The function receives the key alias and secure area as parameters and should
     *           return the appropriate unlock data, or null if no unlock is required.
     *
     *           **Default behavior:** Returns null (no unlock data required).
     *
     *           **When to customize:** Provide a custom implementation when your keys require user
     *           authentication (e.g., biometric or PIN). The function should prompt the user for
     *           authentication and return the unlock data upon successful authentication.
     *
     *           **Parameters:**
     *           - `keyAlias`: The alias of the DPoP key that needs to be unlocked
     *           - `secureArea`: The secure area containing the key
     *
     *           **Returns:** [KeyUnlockData] if authentication is required and successful, or null
     *           if no authentication is needed.
     */
    data class Custom(
        val secureArea: SecureArea,
        val createKeySettingsBuilder: (Algorithm) -> CreateKeySettings,
        val unlockKey: suspend (keyAlias: String, secureArea: SecureArea) -> KeyUnlockData? = { _, _ -> null }
    ) : DPopConfig
}
