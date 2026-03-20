/*
 * Copyright (c) 2026 European Commission
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

import eu.europa.ec.eudi.wallet.document.DocumentManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.ByteString
import org.multipaz.storage.ephemeral.EphemeralStorage
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class DocumentManagerWithMetadataCleanupTest {

    @Test
    fun `deleteDocumentById delegates to wrapped DocumentManager and returns its result`() {
        val expectedResult = mockk<eu.europa.ec.eudi.wallet.document.Outcome<eu.europa.ec.eudi.wallet.document.ProofOfDeletion?>>()
        val delegate = mockk<DocumentManager>(relaxed = true) {
            every { deleteDocumentById("doc-123") } returns expectedResult
        }
        val storage = mockk<org.multipaz.storage.Storage>(relaxed = true)

        val wrapper = DocumentManagerWithMetadataCleanup(delegate, storage)
        val result = wrapper.deleteDocumentById("doc-123")

        verify(exactly = 1) { delegate.deleteDocumentById("doc-123") }
        assertSame(expectedResult, result)
    }

    @Test
    fun `deleteDocumentById also deletes issuance metadata from storage`() = runTest {
        val storage = EphemeralStorage()
        val table = storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)

        // Pre-populate storage with metadata for the document
        table.insert("doc-123", ByteString(byteArrayOf(1, 2, 3)))
        assertNotNull(table.get("doc-123"))

        val delegate = mockk<DocumentManager>(relaxed = true)
        val wrapper = DocumentManagerWithMetadataCleanup(delegate, storage)

        wrapper.deleteDocumentById("doc-123")

        // Wait for async metadata deletion (runs on Dispatchers.IO)
        Thread.sleep(500)

        assertNull(table.get("doc-123"))
    }

    @Test
    fun `deleteDocumentById succeeds even when no metadata exists for the document`() = runTest {
        val storage = EphemeralStorage()
        val delegate = mockk<DocumentManager>(relaxed = true)
        val wrapper = DocumentManagerWithMetadataCleanup(delegate, storage)

        // Should not throw even though "unknown-doc" has no metadata
        wrapper.deleteDocumentById("unknown-doc")

        // Wait for async operation to complete
        Thread.sleep(500)

        verify(exactly = 1) { delegate.deleteDocumentById("unknown-doc") }
    }
}
