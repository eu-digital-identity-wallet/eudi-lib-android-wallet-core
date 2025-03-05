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

package eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing

import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentationTransactionLog
import java.time.Instant

/**
 * Parses a [TransactionLog] and returns a [PresentationTransactionLog].
 *
 * @param transactionLog The transaction log to parse.
 * @return A [PresentationTransactionLog] object.
 * @throws IllegalArgumentException If the transaction log is not a presentation transaction log or if any required fields are null.
 */
fun parsePresentationTransactionLog(transactionLog: TransactionLog): PresentationTransactionLog {
    require(transactionLog.type == TransactionLog.Type.Presentation) {
        "Transaction log is not a presentation transaction log"
    }
    requireNotNull(transactionLog.dataFormat) { "Transaction log data format is null" }
    requireNotNull(transactionLog.rawRequest) { "Transaction log raw request is null" }
    requireNotNull(transactionLog.rawResponse) { "Transaction log raw response is null" }
    requireNotNull(transactionLog.relyingParty) { "Transaction log relying party is null" }

    val presentedDocuments = when (transactionLog.dataFormat) {
        TransactionLog.DataFormat.Cbor -> {
            parseMsoMdoc(
                rawResponse = transactionLog.rawResponse,
                sessionTranscript = transactionLog.sessionTranscript,
                metadata = transactionLog.metadata
            )
        }

        TransactionLog.DataFormat.Json -> {
            parseVp(
                rawResponse = transactionLog.rawResponse,
                metadata = transactionLog.metadata
            )
        }
    }

    return PresentationTransactionLog(
        timestamp = Instant.ofEpochMilli(transactionLog.timestamp),
        status = transactionLog.status,
        relyingParty = transactionLog.relyingParty,
        documents = presentedDocuments
    )
}
