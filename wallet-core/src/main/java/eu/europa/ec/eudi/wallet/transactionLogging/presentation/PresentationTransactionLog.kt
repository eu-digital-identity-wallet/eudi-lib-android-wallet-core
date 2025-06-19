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

package eu.europa.ec.eudi.wallet.transactionLogging.presentation

import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing.parsePresentationTransactionLog
import java.time.Instant

/**
 * Data class representing a presented document in a presentation transaction log.
 *
 * @property format The format of the document.
 * @property metadata The metadata associated with the document.
 * @property claims The list of claims associated with the document.
 */
data class PresentedDocument(
    val format: DocumentFormat,
    val metadata: IssuerMetadata?,
    val claims: List<PresentedClaim>
)

/**
 * Data class representing a presented claim in a presentation transaction log.
 *
 * @property path The path to the claim.
 * @property value The issuerMetadata of the claim.
 * @property rawValue The raw issuerMetadata of the claim.
 * @property metadata The metadata associated with the claim.
 */
data class PresentedClaim(
    val path: List<String>,
    val value: Any?,
    val rawValue: Any,
    val metadata: IssuerMetadata.Claim?
)

/**
 * Data class representing a presentation transaction log.
 * @property timestamp The timestamp of the transaction.
 * @property status The status of the transaction.
 * @property relyingParty The relying party associated with the transaction.
 * @property documents The list of presented documents.
 */
data class PresentationTransactionLog(
    val timestamp: Instant,
    val status: TransactionLog.Status,
    val relyingParty: TransactionLog.RelyingParty,
    val documents: List<PresentedDocument>
) {

    companion object {
        /**
         * Parses a [TransactionLog] into a [PresentationTransactionLog].
         *
         * This function is a wrapper around the [parsePresentationTransactionLog] function.
         *
         * @param transactionLog The [TransactionLog] to be parsed.
         * @return A [Result] containing the parsed [PresentationTransactionLog] or an exception if parsing fails.
         */
        fun fromTransactionLog(transactionLog: TransactionLog): Result<PresentationTransactionLog> {
            return runCatching {
                parsePresentationTransactionLog(transactionLog)
            }
        }
    }
}