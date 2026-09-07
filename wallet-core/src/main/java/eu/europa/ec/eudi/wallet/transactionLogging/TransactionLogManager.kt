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

package eu.europa.ec.eudi.wallet.transactionLogging

import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry

/**
 * Single entry point for recording all transaction-log entries (presentation, issuance,
 * deletion, and host-app actions). Hands entries to the [TransactionLogger] for storage.
 */
interface TransactionLogManager {
    /** Records one transaction-log entry. */
    fun log(entry: TransactionEntry)
}

/**
 * Default [TransactionLogManager]. Saves each entry via [storage].
 *
 * @property storage where entries are saved.
 */
class DefaultTransactionLogManager(
    private val storage: TransactionLogger,
) : TransactionLogManager {
    override fun log(entry: TransactionEntry) {
        storage.log(entry)
    }
}
