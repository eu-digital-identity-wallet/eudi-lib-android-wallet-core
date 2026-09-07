/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.transactionLogging.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The result of a transaction (TS10 §3.1). Either [Completed] or [NotCompleted].
 *
 * Non-completed transactions are logged too (ARF DASH_02). The reason for non-completion is
 * carried on [NotCompleted].
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
sealed interface TransactionResult {

    /** The transaction completed successfully. */
    @Serializable
    @SerialName("Completed")
    data object Completed : TransactionResult

    /**
     * The transaction did not complete.
     *
     * @property reason why it did not complete, if known (TS10 `reasonOfNoncompletion`).
     */
    @Serializable
    @SerialName("NotCompleted")
    data class NotCompleted(val reason: String? = null) : TransactionResult
}
