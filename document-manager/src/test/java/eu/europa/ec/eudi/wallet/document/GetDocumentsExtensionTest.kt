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

package eu.europa.ec.eudi.wallet.document

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GetDocumentsExtensionTest {

    @Test
    fun `test getDocuments extension returns the correct documents`() {
        val issued = mockk<IssuedDocument>()
        val deferred = mockk<DeferredDocument>()
        val unsigned = mockk<UnsignedDocument>()
        val documentManager = mockk<DocumentManager>()

        every {
            documentManager.getDocuments()
        } returns listOf(issued, deferred, unsigned)


        val issuedResult = documentManager.getDocuments<IssuedDocument>()
        assertEquals(1, issuedResult.size)
        assertSame(issued, issuedResult[0])

        val deferredResult = documentManager.getDocuments<DeferredDocument>()
        assertEquals(1, deferredResult.size)
        assertSame(deferred, deferredResult[0])

        val unsignedResult = documentManager.getDocuments<UnsignedDocument>()
        assertEquals(1, unsignedResult.size)
        assertTrue(unsignedResult.contains(unsigned))
    }
}