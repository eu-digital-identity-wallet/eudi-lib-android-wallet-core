/*
 * Copyright (c) 2024-2025 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci.reissue

import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.ProofOfDeletion
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.multipaz.storage.Storage

/**
 * [DocumentManagerWithMetadataCleanup] is a wrapper around [DocumentManager] that cleans up
 * issuance metadata when documents are deleted.
 *
 * This ensures that when the wallet-ui (or any caller) deletes a document via
 * [deleteDocumentById], the corresponding issuance metadata is also removed.
 *
 * @property delegate The delegate [DocumentManager] instance.
 * @property issuanceMetadataStorage The storage containing issuance metadata.
 * @property logger Optional logger for logging events.
 */
internal class DocumentManagerWithMetadataCleanup(
    private val delegate: DocumentManager,
    private val issuanceMetadataStorage: Storage,
    private val logger: Logger? = null,
) : DocumentManager by delegate {

    override fun deleteDocumentById(documentId: DocumentId): Outcome<ProofOfDeletion?> {
        return delegate.deleteDocumentById(documentId).also {
            deleteIssuanceMetadata(documentId)
        }
    }

    private fun deleteIssuanceMetadata(documentId: DocumentId) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val table = issuanceMetadataStorage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
                table.delete(documentId)
                logger?.d(TAG, "Deleted issuance metadata for old document $documentId")
            }.onFailure { error ->
                logger?.d(TAG, "Failed to delete issuance metadata for $documentId: ${error.message}")
            }
        }
    }
}
