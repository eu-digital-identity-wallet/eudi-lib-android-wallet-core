/*
 * Copyright (c) 2025-2026 European Commission
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

package eu.europa.ec.eudi.wallet.internal

import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.multipaz.credential.Credential
import kotlin.test.assertEquals
import org.multipaz.document.Document as ExternalDocument
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for the [Credential] → [IssuedDocument] bridge functions in
 * [eu.europa.ec.eudi.wallet.internal.Extensions]:
 *
 *  - [Credential.toIssuedDocument]: returns the [IssuedDocument] when the credential's
 *    underlying document identifier resolves to one; returns `null` when the document
 *    is missing or is not in the issued state (e.g. unsigned, deferred).
 *  - [Credential.requireIssuedDocument]: throws [IllegalStateException] on the same
 *    null cases, surfacing the credential's identifier in the error message for
 *    debuggability.
 */
class ExtensionsTest {

    /**
     * Happy path: the credential's document identifier resolves to an [IssuedDocument]
     * in the manager; the extension returns that exact instance.
     */
    @Test
    fun `toIssuedDocument returns the IssuedDocument when the manager resolves it`() {
        val documentId = "doc-id-1"
        val credential = mockCredentialWithDocumentId(documentId)
        val issued = mockk<IssuedDocument>()
        val documentManager = mockk<DocumentManager> {
            every { getDocumentById(documentId) } returns issued
        }

        val resolved = credential.toIssuedDocument(documentManager)

        assertSame(issued, resolved, "Extension must return the exact IssuedDocument returned by the manager")
    }

    /**
     * The manager knows the document but it isn't in the issued state — e.g. an
     * [UnsignedDocument] returned mid-issuance. The `as? IssuedDocument` cast filters
     * it out and the extension returns `null`. (Same predicate applies to [Document]
     * subtypes like `DeferredDocument`.)
     */
    @Test
    fun `toIssuedDocument returns null when the document is not an IssuedDocument`() {
        val documentId = "doc-id-2"
        val credential = mockCredentialWithDocumentId(documentId)
        val unsigned = mockk<UnsignedDocument>()
        val documentManager = mockk<DocumentManager> {
            every { getDocumentById(documentId) } returns unsigned
        }

        val resolved = credential.toIssuedDocument(documentManager)

        assertNull(resolved, "Unsigned/deferred documents must not surface as IssuedDocument")
    }

    /**
     * The document store doesn't know this identifier at all — `getDocumentById` returns
     * `null` and the extension must propagate that without throwing.
     */
    @Test
    fun `toIssuedDocument returns null when the document is missing from the manager`() {
        val documentId = "doc-id-missing"
        val credential = mockCredentialWithDocumentId(documentId)
        val documentManager = mockk<DocumentManager> {
            every { getDocumentById(documentId) } returns null
        }

        val resolved = credential.toIssuedDocument(documentManager)

        assertNull(resolved, "Missing document must produce null, not throw")
    }

    /**
     * Happy path: delegates to [Credential.toIssuedDocument] and returns the same
     * [IssuedDocument] when it resolves.
     */
    @Test
    fun `requireIssuedDocument returns the IssuedDocument on success`() {
        val documentId = "doc-id-3"
        val credential = mockCredentialWithDocumentId(documentId)
        val issued = mockk<IssuedDocument>()
        val documentManager = mockk<DocumentManager> {
            every { getDocumentById(documentId) } returns issued
        }

        assertSame(issued, credential.requireIssuedDocument(documentManager))
    }

    /**
     * Missing document → [IllegalStateException]. The thrown message must include the
     * credential's underlying document identifier, so logs are actionable without a
     * debugger.
     */
    @Test
    fun `requireIssuedDocument throws with identifier in the message when the document is missing`() {
        val documentId = "doc-id-missing-required"
        val credential = mockCredentialWithDocumentId(documentId)
        val documentManager = mockk<DocumentManager> {
            every { getDocumentById(documentId) } returns null
        }

        val thrown = assertFailsWith<IllegalStateException> {
            credential.requireIssuedDocument(documentManager)
        }
        assertEquals(
            thrown.message?.contains(documentId),
            true,
            "Error message must mention the credential's document identifier; was: ${thrown.message}"
        )
    }

    /**
     * Document is in the manager but not [IssuedDocument] — must throw with the same
     * surface as the missing case (caller can't disambiguate by exception type, only
     * by message — and that's intentional: both are "no issued credential to disclose").
     */
    @Test
    fun `requireIssuedDocument throws when the document is present but not an IssuedDocument`() {
        val documentId = "doc-id-unsigned"
        val credential = mockCredentialWithDocumentId(documentId)
        val unsigned = mockk<UnsignedDocument>()
        val documentManager = mockk<DocumentManager> {
            every { getDocumentById(documentId) } returns unsigned
        }

        assertFailsWith<IllegalStateException> {
            credential.requireIssuedDocument(documentManager)
        }
    }

    /** A [Credential] whose only relevant property is `document.identifier`. */
    private fun mockCredentialWithDocumentId(identifier: String): Credential {
        val mockedDocument = mockk<ExternalDocument> { every { this@mockk.identifier } returns identifier }
        return mockk<Credential> { every { document } returns mockedDocument }
    }
}