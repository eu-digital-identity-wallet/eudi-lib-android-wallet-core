package eu.europa.ec.eudi.wallet.issue.openid4vci.dpop

import com.nimbusds.jose.jwk.JWK
import eu.europa.ec.eudi.openid4vci.SignOperation
import eu.europa.ec.eudi.wallet.issue.openid4vci.javaAlgorithm
import kotlinx.coroutines.runBlocking
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.KeyInfo
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.SecureArea

/**
 * Secure area-based implementation of [DPopSigner] for OpenID4VCI credential issuance.
 *
 * This class creates and manages a DPoP key in a secure area (such as Android Keystore)
 * and uses it to sign DPoP proof JWTs. The key is created once during initialization
 * and persists in the secure area for the duration of the credential issuance flow.
 *
 * ## Key Features
 *
 * - **Secure Key Storage**: Keys are stored in a hardware-backed secure area when available
 * - **Algorithm Negotiation**: Uses an algorithm supported by both the server and secure area
 * - **User Authentication**: Optionally requires user authentication (biometric/PIN) for signing
 * - **Persistent Keys**: Keys remain in the secure area across multiple signing operations
 *
 * ## Lifecycle
 *
 * 1. **Initialization**: A new key is created in the secure area with the specified algorithm
 * 2. **Signing**: The [acquire] method provides a signing function for creating DPoP proofs
 * 3. **Cleanup**: The [release] method is called but performs no action (keys persist)
 *
 * ## Usage
 *
 * This class is typically created by [DPopSigner.makeIfSupported] and should not be
 * instantiated directly by application code.
 *
 * ```kotlin
 * val signer = DPopSigner.makeIfSupported(
 *     context = context,
 *     config = DPopConfig.Custom(
 *         secureArea = mySecureArea,
 *         createKeySettingsBuilder = { algorithm ->
 *             AndroidKeystoreCreateKeySettings.Builder(challenge)
 *                 .setAlgorithm(algorithm)
 *                 .setUserAuthenticationRequired(true, 30_000, setOf(
 *                     UserAuthenticationType.BIOMETRIC
 *                 ))
 *                 .build()
 *         },
 *         unlockKey = { keyAlias, secureArea ->
 *             // Provide unlock data for authenticated keys
 *             biometricPrompt.authenticate()
 *             keyUnlockData
 *         }
 *     ),
 *     authorizationServerMetadata = metadata
 * ).getOrThrow() as SecureAreaDpopSigner
 *
 * // Use the signer
 * val signOperation = signer.acquire()
 * val signature = signOperation.function(dataToSign)
 * signer.release(signOperation)
 * ```
 *
 * @property config The DPoP configuration containing the secure area and key settings builder.
 *           This configuration determines where keys are stored and how they are protected.
 * @property algorithm The cryptographic algorithm used for DPoP signing (e.g., ES256, ES384, ES512).
 *           This algorithm is selected during [DPopSigner.makeIfSupported] based on compatibility
 *           between the authorization server's supported algorithms and the secure area's capabilities.
 * @property unlockKey A suspend function that provides [KeyUnlockData] for unlocking the DPoP key
 *           when performing signing operations. This function is invoked each time a signature
 *           is created via [acquire].
 *
 *           **Default:** Returns null (no authentication required)
 *
 *           **Parameters:**
 *           - `keyAlias`: The alias of the DPoP key in the secure area
 *           - `secureArea`: The secure area containing the key
 *
 *           **Returns:** [KeyUnlockData] if user authentication is required, or null otherwise
 *
 *           **Example with biometric authentication:**
 *           ```kotlin
 *           unlockKey = { keyAlias, secureArea ->
 *               val cryptoObject = createCryptoObject(keyAlias)
 *               biometricPrompt.authenticate(cryptoObject)
 *               AndroidKeystoreKeyUnlockData(cryptoObject)
 *           }
 *           ```
 * @property keyInfo Information about the DPoP key created in the secure area.
 *           Contains the key alias, algorithm, and public key. This is populated
 *           during initialization and is accessible for inspection.
 *
 * @constructor Creates a new DPoP signer with a key in the specified secure area.
 *              **Note:** This constructor is typically called by [DPopSigner.makeIfSupported]
 *              and should not be invoked directly by application code.
 *
 * @see DPopSigner
 * @see DPopConfig.Custom
 * @see DPopSigner.makeIfSupported
 */
class SecureAreaDpopSigner(
    val config: DPopConfig.Custom,
    private val algorithm: Algorithm,
    private val unlockKey: suspend (keyAlias: String, secureArea: SecureArea) -> KeyUnlockData? = { _, _ -> null }
) : DPopSigner {

    private val secureArea = config.secureArea

    /**
     * Information about the DPoP key created in the secure area.
     *
     * This property is initialized during object construction by creating a new key
     * using the algorithm and key settings from the configuration. The key is created
     * synchronously using [runBlocking] to ensure it's available immediately.
     *
     * The [KeyInfo] contains:
     * - **alias**: The unique identifier for the key in the secure area
     * - **algorithm**: The cryptographic algorithm used (matches the [algorithm] property)
     * - **publicKey**: The public key material in EC format
     */
    val keyInfo: KeyInfo = runBlocking {
        val createKeySettings = config.createKeySettingsBuilder(algorithm)
        secureArea.createKey(null, createKeySettings)
    }

    /**
     * The Java algorithm identifier for the signing algorithm.
     *
     * This property provides the Java Security API algorithm name (e.g., "SHA256withECDSA"
     * for ES256) that corresponds to the DPoP signing algorithm. The value is derived
     * from the [keyInfo] and is used internally by the OpenID4VCI library.
     *
     * @throws IllegalArgumentException if the key's algorithm is not a signing algorithm
     *         or doesn't have a corresponding Java algorithm identifier
     */
    override val javaAlgorithm: String
        get() = keyInfo.algorithm
            .takeIf { it.isSigning }
            ?.javaAlgorithm
            ?: throw IllegalArgumentException("Unsupported algorithm")

    /**
     * Acquires a signing operation for creating DPoP proofs.
     *
     * This method prepares a [SignOperation] that can be used to sign DPoP proof JWTs.
     * The operation includes:
     *
     * 1. **Public Key Material**: The public key as a JWK (JSON Web Key) that will be
     *    included in the DPoP proof header
     * 2. **Signing Function**: A function that signs input data using the private key
     *    stored in the secure area
     *
     * ## Signing Process
     *
     * When the signing function is invoked:
     * 1. The [unlockKey] callback is called to obtain unlock data (if required)
     * 2. The secure area signs the input data using the private key
     * 3. The signature is returned in DER-encoded format
     *
     * ## User Authentication
     *
     * If the key requires user authentication (configured via [DPopConfig.Custom.createKeySettingsBuilder]),
     * the [unlockKey] function must prompt the user (e.g., via biometric prompt) and provide
     * the unlock data before signing can proceed.
     *
     * ## Example
     *
     * ```kotlin
     * val signOperation = signer.acquire()
     * try {
     *     val publicKey = signOperation.publicMaterial // JWK for DPoP header
     *     val signature = signOperation.function(dataToSign) // Create signature
     *     // Use publicKey and signature in DPoP proof
     * } finally {
     *     signer.release(signOperation)
     * }
     * ```
     *
     * @return A [SignOperation] containing the public JWK and signing function
     */
    override suspend fun acquire(): SignOperation<JWK> {
        val jwk = JWK.parse(
            keyInfo.publicKey.toJwk(
                // todo add attestations
            ).toString()
        )
        return SignOperation(
            function = { input ->
                val keyUnlockData = unlockKey(keyInfo.alias, secureArea)
                secureArea.sign(keyInfo.alias, input, keyUnlockData).toDerEncoded()
            },
            publicMaterial = jwk
        )
    }

    /**
     * Releases resources associated with a signing operation.
     *
     * This implementation does not require any cleanup. The DPoP key remains in the
     * secure area and can be reused for subsequent signing operations during the
     * credential issuance flow.
     *
     * This method is called by the OpenID4VCI library when the signing operation is
     * complete, typically after a DPoP proof has been created and sent to the server.
     *
     * @param signOperation The signing operation to release, or null if no operation
     *        is currently held. This parameter is ignored in this implementation.
     */
    override suspend fun release(signOperation: SignOperation<JWK>?) {
        // Nothing to release - key persists in secure area
    }
}