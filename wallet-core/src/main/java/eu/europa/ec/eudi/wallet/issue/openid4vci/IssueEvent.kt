/*
 *  Copyright (c) 2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package eu.europa.ec.eudi.wallet.issue.openid4vci

import androidx.biometric.BiometricPrompt.CryptoObject
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.UnsignedDocument

/**
 * Events related to document issuance.
 */
sealed interface IssueEvent : OpenId4VciResult {

    /**
     * The issuance was started.
     * @property total the total number of documents to issue
     */
    data class Started(val total: Int) : IssueEvent

    /**
     * The issuance has finished.
     * @property issuedDocuments the ids of the issued documents
     * @see[DocumentId] for the document id
     */
    data class Finished(val issuedDocuments: List<DocumentId>) : IssueEvent

    /**
     * The issuance failed.
     * @property cause the error that caused the failure
     */
    data class Failure(override val cause: Throwable) : IssueEvent, OpenId4VciResult.Erroneous

    /**
     * Document issued successfully.
     * @property documentId the id of the issued document
     * @property name the name of the document
     * @property docType the document type
     * @see[DocumentId] for the document id
     */
    data class DocumentIssued(val documentId: DocumentId, val name: String, val docType: String) : IssueEvent

    /**
     * Document issuance failed.
     * @property name the name of the document
     * @property docType the document type
     * @property cause the error that caused the failure
     */
    data class DocumentFailed(val name: String, val docType: String, override val cause: Throwable) : IssueEvent,
        OpenId4VciResult.Erroneous

    /**
     * The document issuance requires user authentication.
     *
     * @property name the name of the document
     * @property docType the document type
     * @property cryptoObject the crypto object to use for authentication
     * @property resume the callback to resume the issuance
     * @property cancel the callback to cancel the issuance
     *
     * Example usage:
     *
     * ```
     * class SomeFragment : Fragment() {
     *
     *    private lateinit var onIssuingResume: () -> Unit
     *    private lateinit var onIssuingCancel: () -> Unit
     *
     *    private lateinit var prompt: BiometricPrompt
     *    private val promptInfo: BiometricPrompt.PromptInfo by lazy {
     *      BiometricPrompt.PromptInfo.Builder()
     *          .setTitle("Title")
     *          .setSubtitle("Subtitle")
     *          .setDescription("Description")
     *          .setNegativeButtonText("Cancel")
     *          .build()
     *    }
     *    private val promptCallback by lazy {
     *      object : BiometricPrompt.AuthenticationCallback() {
     *          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
     *              super.onAuthenticationError(errorCode, errString)
     *                  if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
     *                  onIssuingCancel.invoke()
     *              } else {
     *                  log("User authentication failed $errorCode - $errString")
     *              }
     *          }
     *
     *          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
     *              super.onAuthenticationSucceeded(result)
     *              log("User authentication succeeded")
     *              onIssuingResume.invoke()
     *          }
     *
     *          override fun onAuthenticationFailed() {
     *              super.onAuthenticationFailed()
     *              log("User authentication failed")
     *          }
     *       }
     *    }
     *
     *   fun onResume() {
     *      super.onResume()
     *      prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(requireContext()), promptCallback)
     *   }
     *
     *   fun issueDocument() {
     *      EudiWallet.issueDocumentByDocType("eu.europa.ec.eudi.pid.1", requireContext().mainExecutor) { event ->
     *          when (event) {
     *              is IssueEvent.DocumentRequiresUserAuth -> {
     *                  onIssuingResume = result::resume
     *                  onIssuingCancel = result::cancel
     *                  if (result.cryptoObject != null) {
     *                      prompt.authenticate(promptInfo, result.cryptoObject)
     *                  } else {
     *                      prompt.authenticate(promptInfo)
     *                  }
     *              }
     *              else -> {
     *                  // handle other events
     *              }
     *          }
     *       }
     *    }
     * }
     */
    data class DocumentRequiresUserAuth(
        val name: String,
        val docType: String,
        val cryptoObject: CryptoObject?,
        val resume: () -> Unit,
        val cancel: () -> Unit
    ) : IssueEvent {
        internal constructor(
            unsignedDocument: UnsignedDocument,
            cryptoObject: CryptoObject?,
            resume: () -> Unit,
            cancel: () -> Unit
        ) : this(
            unsignedDocument.name,
            unsignedDocument.docType,
            cryptoObject,
            resume,
            cancel
        )
    }

    /**
     * Document issuance deferred.
     * @property documentId the id of the deferred document
     * @property name the name of the document
     * @property docType the document type
     */
    data class DocumentDeferred(val documentId: DocumentId, val name: String, val docType: String) : IssueEvent

    companion object {
        internal fun failure(e: Throwable) = Failure(e)

        internal fun documentFailed(unsignedDocument: UnsignedDocument, cause: Throwable) =
            DocumentFailed(
                unsignedDocument.name,
                unsignedDocument.docType,
                cause
            )
    }
}
