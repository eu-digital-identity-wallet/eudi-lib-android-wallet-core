/*
 *  Copyright (c) 2023 European Commission
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

package eu.europa.ec.eudi.wallet.document.issue

import androidx.biometric.BiometricPrompt.CryptoObject
import eu.europa.ec.eudi.wallet.document.DocumentId

sealed interface IssueDocumentResult {
    data class Success(val documentId: DocumentId) : IssueDocumentResult
    data class Failure(val error: Throwable) : IssueDocumentResult

    data class UserAuthRequired(
        val cryptoObject: CryptoObject? = null,
        private val _resume: () -> Unit,
        private val _cancel: () -> Unit
    ) : IssueDocumentResult {
        fun resume(): Unit = _resume()
        fun cancel(): Unit = _cancel()
    }
}