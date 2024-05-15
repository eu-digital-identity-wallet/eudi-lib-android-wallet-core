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
import eu.europa.ec.eudi.wallet.document.DocumentId

sealed interface IssueDocumentResult {
    /**
     * The document was successfully issued.
     * @property documentId the id of the issued document
     */
    data class Success(val documentId: DocumentId) : IssueDocumentResult

    /**
     * The document issuance failed.
     * @property error the error that caused the failure
     */
    data class Failure(val error: Throwable) : IssueDocumentResult

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
     *      EudiWallet.issueDocument("eu.europa.ec.eudiw.pid.1", requireContext().mainExecutor) { result ->
     *          when (result) {
     *              is IssueDocumentResult.UserAuthRequired -> {
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
        val cryptoObject: CryptoObject?,
        private val onResume: () -> Unit,
        private val onCancel: () -> Unit
    ) : IssueDocumentResult {
        fun resume(): Unit = onResume()
        fun cancel(): Unit = onCancel()
    }
}