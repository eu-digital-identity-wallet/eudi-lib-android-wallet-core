/*
 *  Copyright (c) 2023-2024 European Commission
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
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuanceRequest

sealed interface IssueDocumentEvent {

    /**
     * The issuance was finished.
     */
    object Finished : IssueDocumentEvent

    /**
     * The issuance failed.
     * @property error the error that caused the failure
     */
    data class Failure(val error: Throwable) : IssueDocumentEvent

    /**
     * The document was successfully issued.
     * @property documentId the id of the issued document
     */
    data class DocumentIssued(val documentId: DocumentId) : IssueDocumentEvent

    /**
     * The document issuance failed.
     * @property error the error that caused the failure
     * @property documentName the name of the document that was being issued
     * @property docType the type of the document that was being issued
     */
    data class DocumentFailure(val error: Throwable, val documentName: String? = null, val docType: String? = null) :
        IssueDocumentEvent {
        internal constructor(error: Throwable, issuanceRequest: IssuanceRequest) : this(
            error,
            issuanceRequest.name,
            issuanceRequest.docType
        )

        internal constructor(error: Throwable, credentialConfiguration: CredentialConfiguration) : this(
            error,
            credentialConfiguration.name,
            credentialConfiguration.docType
        )
    }

    /**
     * The document issuance was canceled by the user during authentication.
     * @property documentName the name of the document that was being issued
     * @property docType the type of the document that was being issued
     */
    data class UserAuthCanceled(val documentName: String, val docType: String) : IssueDocumentEvent {
        internal constructor(credentialConfiguration: CredentialConfiguration) : this(
            credentialConfiguration.name,
            credentialConfiguration.docType
        )
    }

    /**
     * The document issuance requires user authentication.
     *
     * @property cryptoObject the crypto object to use for authentication
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
     *      EudiWallet.issueDocumentByDocType("eu.europa.ec.eudiw.pid.1", requireContext().mainExecutor) { result ->
     *          when (result) {
     *              is IssueDocumentEvent.UserAuthRequired -> {
     *                  onIssuingResume = result::resume
     *                  onIssuingCancel = result::cancel
     *                  if (result.cryptoObject != null) {
     *                      prompt.authenticate(promptInfo, result.cryptoObject)
     *                  } else {
     *                      prompt.authenticate(promptInfo)
     *                  }
     *              }
     *              else -> {
     *                  // handle other results
     *              }
     *          }
     *       }
     *    }
     * }
     */
    data class UserAuthRequired(
        val documentName: String,
        val docType: String,
        val cryptoObject: CryptoObject?,
        private val onResume: () -> Unit,
        private val onCancel: () -> Unit
    ) : IssueDocumentEvent {
        constructor(
            credentialConfiguration: CredentialConfiguration,
            cryptoObject: CryptoObject?,
            onResume: () -> Unit,
            onCancel: () -> Unit
        ) : this(
            credentialConfiguration.name,
            credentialConfiguration.docType,
            cryptoObject,
            onResume,
            onCancel
        )

        fun resume(): Unit = onResume()
        fun cancel(): Unit = onCancel()
    }
}