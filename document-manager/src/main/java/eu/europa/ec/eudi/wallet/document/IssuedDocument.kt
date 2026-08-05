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

package eu.europa.ec.eudi.wallet.document

import eu.europa.ec.eudi.wallet.document.format.DocumentData
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.internal.CredentialPolicyApplier
import eu.europa.ec.eudi.wallet.document.internal.applicationMetadata
import eu.europa.ec.eudi.wallet.document.internal.createdAt
import eu.europa.ec.eudi.wallet.document.internal.documentManagerId
import eu.europa.ec.eudi.wallet.document.internal.documentName
import eu.europa.ec.eudi.wallet.document.internal.format
import eu.europa.ec.eudi.wallet.document.internal.issuedAt
import eu.europa.ec.eudi.wallet.document.internal.issuerMetaData
import eu.europa.ec.eudi.wallet.document.internal.asProvider
import eu.europa.ec.eudi.wallet.document.internal.toCoseBytes
import eu.europa.ec.eudi.wallet.document.internal.toEcPublicKey
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.crypto.EcSignature
import org.multipaz.prompt.Reason
import org.multipaz.securearea.KeyInfo
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.SecureArea
import java.time.Instant
import kotlin.time.Clock
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/**
 * Represents an Issued Document in the EUDI Wallet.
 *
 * This class models a document that has been issued and stored in the wallet. It provides methods
 * to access the document's data, verify its validity, and perform cryptographic operations using
 * the document's credentials. Documents follow a specific credential policy that determines how
 * credentials are used and managed after cryptographic operations.
 *
 * The document's credentials are managed according to the specified [credentialPolicy],
 * which can either rotate credentials after use or enforce one-time use.
 *
 * @property id The unique identifier of the document
 * @property name The human-readable name of the document
 * @property format The format specification of the document (e.g., MsoMdoc, SdJwtVc)
 * @property documentManagerId The identifier of the [DocumentManager] that manages this document
 * @property createdAt The timestamp when the document was created in the wallet
 * @property issuedAt The timestamp when the document was issued by the issuer
 * @property baseDocument The underlying document implementation
 * @property credentialPolicy The policy that determines how credentials are managed after use
 * @property issuerMetadata The document metadata provided by the issuer
 * @property data The document data in its format-specific representation
 */
class IssuedDocument(
    internal val baseDocument: org.multipaz.document.Document,
) : Document {

    override val format: DocumentFormat
        get() = baseDocument.format
    override val id: DocumentId
        get() = baseDocument.identifier
    override val name: String
        get() = baseDocument.documentName
    override val documentManagerId: String
        get() = baseDocument.documentManagerId
    override val createdAt: Instant
        get() = baseDocument.createdAt.toJavaInstant()
    override val issuerMetadata: IssuerMetadata?
        get() = baseDocument.issuerMetaData
    val issuedAt: Instant
        get() = baseDocument.issuedAt.toJavaInstant()

    val data: DocumentData
        get() {
            val issuerProvidedData = baseDocument.applicationMetadata.issuerProvidedData
            requireNotNull(issuerProvidedData) { "Issuer provided data not found" }

            return DocumentData.make(
                format = format,
                issuerProvidedData = issuerProvidedData,
                issuerMetadata = issuerMetadata
            )
        }

    /**
     * The credential policy associated with this document.
     *
     * This property determines how credentials are managed after cryptographic operations:
     * - [CreateDocumentSettings.CredentialPolicy.OnceOnly]: The credential is deleted after use
     * - [CreateDocumentSettings.CredentialPolicy.RotatingBatch]: The credential's usage count is incremented after use
     *
     * @see CreateDocumentSettings.CredentialPolicy
     */
    val credentialPolicy: CreateDocumentSettings.CredentialPolicy
        get() = baseDocument.applicationMetadata.credentialPolicy

    companion object {
        const val NO_CREDENTIALS = "Credential not found"
    }

    /**
     * Retrieves all credentials associated with this document that pass structural validity checks.
     *
     * This method filters the document's credentials based on several criteria:
     * - Only certified credentials bound to a secure area
     * - Only credentials that are not invalidated
     * - Only credentials that belong to the current document manager
     * - For OnceOnly policy, only credentials that haven't been used (usageCount == 0)
     * - For RotatingBatch policy, all credentials regardless of usage count
     *
     * **Note:** This method does **not** filter by temporal validity (`validFrom`/`validUntil`).
     * The returned list may include credentials that are expired or not yet valid.
     * Use [findCredential] to obtain a credential that is valid at a specific point in time.
     *
     * @return A list of [SecureAreaBoundCredential] objects that pass structural validity checks
     */
    suspend fun getCredentials(): List<SecureAreaBoundCredential> {
        return baseDocument.getCertifiedCredentials()
            .filterIsInstance<SecureAreaBoundCredential>()
            .filterNot {
                try {
                    it.secureArea.getKeyInvalidated(it.alias)
                } catch (_: IllegalArgumentException) {
                    true // key no longer exists — treat as invalidated
                }
            }
            .filter { it.domain == documentManagerId }
            .filter {
                when (credentialPolicy) {
                    is CreateDocumentSettings.CredentialPolicy.LimitedTime,
                    is CreateDocumentSettings.CredentialPolicy.RotatingBatch -> true
                    is CreateDocumentSettings.CredentialPolicy.OnceOnly -> it.usageCount == 0
                }
            }
    }

    /**
     * Finds the most appropriate credential for the current time or a specified time.
     *
     * This method selects a valid credential based on the following criteria:
     * 1. The credential must be valid at the specified time (now by default)
     * 2. Among valid credentials, the one with the lowest usage count is selected
     *
     * This approach ensures optimal credential rotation when using the RotatingBatch policy
     * and helps prevent unnecessary credential invalidation.
     *
     * @param now Optional timestamp for which to find a valid credential. If null, the current time is used.
     * @return The most appropriate credential, or null if no valid credential is found
     */
    suspend fun findCredential(now: Instant? = null): SecureAreaBoundCredential? {
        var candidate: SecureAreaBoundCredential? = null
        val now = now?.toKotlinInstant() ?: Clock.System.now()
        getCredentials()
            .filter { now >= it.validFrom && now <= it.validUntil }
            .forEach { credential ->
                // If we already have a candidate, prefer this one if its usage count is lower
                candidate?.let { candidateCredential ->
                    if (credential.usageCount < candidateCredential.usageCount) {
                        candidate = credential
                    }
                } ?: run {
                    candidate = credential
                }

            }
        return candidate
    }

    /**
     * Returns the number of credentials that pass structural validity checks.
     *
     * Delegates to [getCredentials], which does **not** filter by temporal validity.
     * This count may include expired or not-yet-valid credentials.
     * To check how many credentials are currently usable, filter [getCredentials] by
     * `validFrom`/`validUntil` or use [findCredential] to check if at least one is valid.
     */
    override suspend fun credentialsCount(): Int {
        return getCredentials().size
    }

    /**
     * Retrieves the initial number of credentials for this document.
     * The number of credentials initially created for this document.
     *
     * @return The initial number of credentials
     */
    fun initialCredentialsCount(): Int {
        return baseDocument.applicationMetadata.initialCredentialsCount
    }

    /**
     * Retrieves the start date from which the document's credential is valid.
     *
     * This method safely retrieves the validity start date from the document's current
     * credential using the credential policy. Like other credential operations, this method
     * applies the document's credential usage policy after accessing the credential.
     *
     * @return A [Result] containing the validity start date as an [Instant] if successful,
     *         or an exception if no valid credential is found
     */
    suspend fun getValidFrom() = runCatching {
        findCredential()?.validFrom?.toJavaInstant()
            ?: throw IllegalStateException("No valid credential found")
    }

    /**
     * Retrieves the end date until which the document's credential is valid.
     *
     * This method safely retrieves the validity end date from the document's current
     * credential using the credential policy. Like other credential operations, this method
     * applies the document's credential usage policy after accessing the credential.
     *
     * @return A [Result] containing the validity end date as an [Instant] if successful,
     *         or an exception if no valid credential is found
     */
    suspend fun getValidUntil() = runCatching {
        findCredential()?.validUntil?.toJavaInstant()
            ?: throw IllegalStateException("No valid credential found")
    }

    /**
     * Checks if the document is certified.
     *
     * A document is certified if it has no pending credentials, meaning all
     * credentials have been signed by an issuer.
     *
     * @return true if the document is certified, false otherwise
     */
    suspend fun isCertified(): Boolean {
        return baseDocument.getPendingCredentials().isEmpty()
    }

    /**
     * Performs an operation with a valid credential and handles usage policy enforcement.
     *
     * This method finds a valid credential, executes the provided block with it,
     * and then applies the appropriate credential policy (either incrementing usage count
     * for RotatingBatch or deleting the credential for OnceOnly).
     *
     * @param T The return type of the operation
     * @param credentialContext The suspend function to execute with the credential as receiver
     * @return A [Result] containing the operation result or an exception if the operation failed
     */
    suspend fun <T> consumingCredential(credentialContext: suspend SecureAreaBoundCredential.() -> T): Result<T> {
        return runCatching {
            val credential = findCredential() ?: throw IllegalStateException(NO_CREDENTIALS)
            val result = credentialContext.invoke(credential)

            // Use the internal policy applier to apply the policy
            val policyApplier = CredentialPolicyApplier.from(credentialPolicy)
            policyApplier.apply(baseDocument, credential)

            result
        }
    }

    /**
     * Signs data with a document credential and applies the credential policy.
     *
     * This method finds a valid credential, uses it to sign the provided data,
     * and then applies the document's credential policy to the used credential.
     *
     * @param dataToSign The byte array containing the data to be signed
     * @param keyUnlockData Optional data required to unlock the key if it's protected
     * @return A [Result] containing the [EcSignature] or an exception if the operation failed
     */
    suspend fun signConsumingCredential(
        dataToSign: ByteArray,
        keyUnlockData: KeyUnlockData? = null
    ): Result<EcSignature> {
        val provider = keyUnlockData.asProvider()
        return withContext(provider) {
            consumingCredential {
                secureArea.sign(
                    alias = alias,
                    dataToSign = dataToSign,
                    unlockReason = Reason.Unspecified
                )
            }
        }
    }

    /**
     * Performs key agreement with a document credential and applies the credential policy.
     *
     * This method finds a valid credential, uses it to establish a shared secret with
     * the provided public key, and then applies the document's credential policy to the
     * used credential. This implementation follows the document's credential policy:
     * for OnceOnly, the credential is deleted after use, and for RotatingBatch, the credential's
     * usage count is incremented.
     *
     * @param otherPublicKey The public key of the other party as a byte array
     * @param keyUnlockData Optional data required to unlock the key if it's protected
     * @return A [Result] containing the [SharedSecret] or an exception if the operation failed
     */
    suspend fun keyAgreementConsumingCredential(
        otherPublicKey: ByteArray,
        keyUnlockData: KeyUnlockData? = null
    ): Result<SharedSecret> {
        val provider = keyUnlockData.asProvider()
        return withContext(provider) {
            consumingCredential {
                secureArea.keyAgreement(
                    alias = alias,
                    otherKey = otherPublicKey.toEcPublicKey,
                    unlockReason = Reason.Unspecified
                )
            }
        }
    }

    // Deprecated properties and methods moved to the end of the file

    @Deprecated("Use method isCertified()")
    override val isCertified: Boolean
        get() = runBlocking { isCertified() }

    @Deprecated("Use findCredential()?.secureArea instead")
    override val secureArea: SecureArea
        get() = runBlocking {
            findCredential()?.secureArea ?: throw IllegalStateException(NO_CREDENTIALS)
        }

    @Deprecated("Use findCredential()?.alias instead")
    override val keyAlias: String
        get() = runBlocking {
            findCredential()?.alias ?: throw IllegalStateException(NO_CREDENTIALS)
        }

    @Deprecated("Use findCredential()?.isKeyInvalidated instead")
    override val isKeyInvalidated: Boolean
        get() = runBlocking {
            findCredential()?.isInvalidated() ?: throw IllegalStateException(NO_CREDENTIALS)
        }

    @Deprecated("Use findCredential()?.secureArea instead to get KeyInfo")
    override val keyInfo: KeyInfo
        get() = runBlocking {
            findCredential()?.let { credential ->
                credential.secureArea.getKeyInfo(credential.alias)
            } ?: throw IllegalStateException(NO_CREDENTIALS)
        }

    @Deprecated("Use findCredential()?.secureArea instead to get KeyInfo")
    override val publicKeyCoseBytes: ByteArray
        get() = keyInfo.publicKey.toCoseBytes

    @Deprecated("Use findCredential()?.issuerProvidedData instead")
    val issuerProvidedData: ByteArray
        get() = runBlocking {
            findCredential()?.issuerProvidedData?.toByteArray()
                ?: throw IllegalStateException(NO_CREDENTIALS)
        }

    @Deprecated("Use getValidFrom() instead")
    val validFrom: Instant
        get() = runBlocking {
            findCredential()?.validFrom?.toJavaInstant()
                ?: throw IllegalStateException(NO_CREDENTIALS)

        }

    @Deprecated("Use getValidUntil() instead")
    val validUntil: Instant
        get() = runBlocking {
            findCredential()?.validUntil?.toJavaInstant()
                ?: throw IllegalStateException(NO_CREDENTIALS)
        }

    /**
     * Checks if the document is valid at a specified point in time.
     *
     * A document is considered valid if it has at least one valid credential
     * at the specified time according to the findCredential criteria.
     *
     * @param time The timestamp at which to check validity
     * @return true if the document is valid at the specified time, false otherwise
     */
    @Deprecated("Use findCredential() instead to check validity at a specific time. If findCredential() returns null, the document is not valid.")
    fun isValidAt(time: Instant): Boolean {
        return runBlocking { findCredential(now = time) != null }
    }

    /**
     * Signs data using the document's cryptographic key.
     *
     * This method uses the document's credential to create a cryptographic signature
     * for the provided data. After signing, it applies the document's credential policy.
     *
     * @param dataToSign The byte array containing the data to be signed
     * @param keyUnlockData Optional data required to unlock the key if it's protected
     * @return An [Outcome] containing either the [EcSignature] or a failure reason
     */
    @Deprecated("use signConsumingCredential method instead")
    fun sign(
        dataToSign: ByteArray,
        keyUnlockData: KeyUnlockData? = null
    ): Outcome<EcSignature> {
        return runBlocking {
            signConsumingCredential(dataToSign, keyUnlockData)
        }.fold(
            onSuccess = { Outcome.success(it) },
            onFailure = { Outcome.failure(it) }
        )
    }

    /**
     * Performs a key agreement protocol to create a shared secret with another party.
     *
     * This method uses the document's private key and the other party's public key
     * to establish a shared secret through a cryptographic key agreement protocol.
     * After the operation, it applies the document's credential policy.
     *
     * @param otherPublicKey The public key of the other party as a byte array
     * @param keyUnlockData Optional data required to unlock the document's key if it's protected
     * @return An [Outcome] containing either the [SharedSecret] or a failure reason
     */
    @Deprecated("use keyAgreementConsumingCredential method instead")
    fun keyAgreement(
        otherPublicKey: ByteArray,
        keyUnlockData: KeyUnlockData? = null
    ): Outcome<SharedSecret> {
        return runBlocking {
            keyAgreementConsumingCredential(otherPublicKey, keyUnlockData)
                .fold(
                    onSuccess = { Outcome.success(it) },
                    onFailure = { Outcome.failure(it) }
                )
        }
    }
}

