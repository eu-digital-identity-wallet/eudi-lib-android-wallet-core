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

/**
 * A logger for transactions.
 *
 * Implementations of this interface should log transactions to some persistent storage.
 * The storage can be a file, a database, or any other storage medium.
 */
fun interface TransactionLogger {
    /**
     * Logs a transaction.
     * @param transaction The transaction to log.
     */
    fun log(transaction: TransactionLog)
}