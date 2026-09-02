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

package eu.europa.ec.eudi.wallet.transactionLogging.producers

import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.ProofOfDeletion
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.registration.QualifiedIdentifier
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogManager
import eu.europa.ec.eudi.wallet.transactionLogging.model.MultiLangString
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import java.time.Instant
import java.util.UUID

/**
 * What the wallet kept about a credential's issuer when it was issued, so a deletion can still name
 * it (TS10 §3.6). The registration certificate itself is gone by then.
 *
 * @property identifier the issuer's registered identifier.
 * @property name the issuer's registered name.
 */
data class RegisteredIssuer(
    val identifier: QualifiedIdentifier? = null,
    val name: String? = null,
)

/**
 * Wraps a [DocumentManager] and logs a [TransactionEntry.CredentialDeletion] entry each time a
 * document is deleted (TS10 §3.6).
 *
 * Only deletions of credentials the user actually holds ([IssuedDocument]) are logged; in-progress
 * documents are skipped. Both successful and failed deletions are logged. The credential identifier
 * and issuer name are read before the deletion, since the issuer metadata is removed with the document.
 *
 * @property delegate the wrapped [DocumentManager].
 * @property transactionLogManager records the deletion entry.
 * @property logger optional logger for internal errors.
 * @property registeredIssuerResolver reads what was kept about a document's issuer at issuance.
 *   Defaults to a no-op; a failing resolver never disrupts the deletion.
 */
class CredentialDeletionLogger(
    private val delegate: DocumentManager,
    private val transactionLogManager: TransactionLogManager,
    private val logger: Logger? = null,
    private val registeredIssuerResolver: (DocumentId) -> RegisteredIssuer? = { null },
) : DocumentManager by delegate {

    override fun deleteDocumentById(documentId: DocumentId): Outcome<ProofOfDeletion?> {

        val snapshot = runCatching {
            (delegate.getDocumentById(documentId) as? IssuedDocument)?.toSnapshot(documentId)
        }
            .onFailure { logError(it, "deleteDocumentById: capture") }
            .getOrNull()

        val outcome = delegate.deleteDocumentById(documentId)

        // Only log when a credential actually existed to be deleted.
        snapshot?.let { safeSnapshot ->
            runCatching { transactionLogManager.log(safeSnapshot.toEntry(outcome)) }
                .onFailure { logError(it, "deleteDocumentById: log") }
        }
        return outcome
    }

    private fun Document.toSnapshot(documentId: DocumentId): DeletionSnapshot = DeletionSnapshot(
        credentialIdentifier = when (val documentFormat = format) {
            is MsoMdocFormat -> documentFormat.docType
            is SdJwtVcFormat -> documentFormat.vct
        },
        // Read before the deletion, since it is removed along with the document.
        registeredIssuer = runCatching { registeredIssuerResolver(documentId) }
            .onFailure { logError(it, "deleteDocumentById: registered issuer") }
            .getOrNull(),
        displayName = issuerMetadata?.issuerDisplay?.firstOrNull()?.let {
            MultiLangString(lang = it.locale?.toLanguageTag() ?: DEFAULT_LANG, content = it.name)
        },
    )

    private fun DeletionSnapshot.toEntry(
        outcome: Outcome<ProofOfDeletion?>,
    ): TransactionEntry.CredentialDeletion = TransactionEntry.CredentialDeletion(
        transactionIdentifier = UUID.randomUUID().toString(),
        time = Instant.now(),
        transactionResult = if (outcome.isSuccess) {
            TransactionResult.Completed
        } else {
            TransactionResult.NotCompleted(
                outcome.exceptionOrNull()?.toNoncompletionReason(REASON_DELETION_FAILED)
                    ?: REASON_DELETION_FAILED
            )
        },
        credentialIdentifier = credentialIdentifier,
        credentialIssuerIdentifier = registeredIssuer?.identifier,
        credentialIssuerName = registeredIssuer?.name
            ?.let { MultiLangString(lang = DEFAULT_LANG, content = it) }
            ?: displayName
    )

    private fun logError(e: Throwable, source: String) {
        logger?.log(
            Logger.Record(
                level = Logger.LEVEL_ERROR,
                message = "Failed to log credential deletion",
                thrown = e,
                sourceClassName = CredentialDeletionLogger::class.java.name,
                sourceMethod = source,
            )
        )
    }

    private data class DeletionSnapshot(
        val credentialIdentifier: String,
        val registeredIssuer: RegisteredIssuer?,
        val displayName: MultiLangString?
    )

    companion object {
        private const val DEFAULT_LANG = "en"

        /** Fallback reason when a deletion fails without a usable error message. */
        private const val REASON_DELETION_FAILED = "Deletion did not complete"
    }
}
