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

package eu.europa.ec.eudi.iso18013.transfer.response.device

import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.multipaz.cbor.Tstr
import org.multipaz.claim.MdocClaim
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.mdoc.request.DocRequest
import org.multipaz.presentment.CredentialMatchSourceIso18013
import org.multipaz.request.MdocRequestedClaim
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for [DocRequest.toCredentialPresentmentSet].
 *
 * Focus is the soft-matching policy: a wallet credential is included in the resulting set
 * if it has **at least one** of the verifier's requested data elements. Missing elements
 * are simply omitted from the [match.claims][org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch.claims]
 * map — the response generator will only sign over what the user has actually agreed to
 * disclose. This restores the pre-refactor behaviour after the brief hard-fail period.
 *
 * Mocking strategy:
 *  - [DocumentManager.getDocuments] is stubbed at the entry point of the
 *    `getValidIssuedMsoMdocDocuments` extension chain, returning concrete [IssuedDocument]
 *    mocks that satisfy the format/credential filters.
 *  - [MdocCredential] mocks expose a real `List<MdocClaim>` via `getClaims`; the
 *    `findMatchingClaim` extension in multipaz then does the namespace/element comparison
 *    against the request — exercising the actual matching code path.
 *  - [DocRequest] is mocked because its constructor is `internal`.
 */
class DocRequestExtensionsTest {

    @Test
    fun `returns null when documentManager has no candidates of the requested docType`() = runBlocking {
        // Wallet has a single document of a *different* docType — getValidIssuedMsoMdocDocuments
        // will filter it out and the extension sees an empty candidate list.
        val otherDoc = mockMdocIssuedDocument(
            docType = "eu.europa.ec.eudi.pid.1",
            claims = listOf(mdocClaim("given_name")),
        )
        val documentManager = mockDocumentManager(documents = listOf(otherDoc))
        val request = mockDocRequest(
            docType = ISO_MDL_DOC_TYPE,
            nameSpaces = mapOf(ISO_MDL_NAMESPACE to mapOf("given_name" to false)),
        )

        val result = request.toCredentialPresentmentSet(documentManager)

        assertNull(result)
    }

    @Test
    fun `returns null when every candidate has zero overlap with the requested claims`() = runBlocking {
        // Credential exposes only family_name; request asks for given_name + birth_date.
        val doc = mockMdocIssuedDocument(claims = listOf(mdocClaim("family_name")))
        val documentManager = mockDocumentManager(documents = listOf(doc))
        val request = mockDocRequest(
            nameSpaces = mapOf(
                ISO_MDL_NAMESPACE to mapOf(
                    "given_name" to false,
                    "birth_date" to false,
                ),
            ),
        )

        val result = request.toCredentialPresentmentSet(documentManager)

        assertNull(result)
    }

    @Test
    fun `returns soft match with only the available subset when credential has partial overlap`() = runBlocking {
        // Request asks for [given_name, family_name, birth_date]; credential has only the first two.
        val doc = mockMdocIssuedDocument(
            claims = listOf(
                mdocClaim("given_name"),
                mdocClaim("family_name"),
            ),
        )
        val documentManager = mockDocumentManager(documents = listOf(doc))
        val request = mockDocRequest(
            nameSpaces = mapOf(
                ISO_MDL_NAMESPACE to mapOf(
                    "given_name" to false,
                    "family_name" to false,
                    "birth_date" to false,
                ),
            ),
        )

        val set = request.toCredentialPresentmentSet(documentManager)

        assertNotNull(set)
        val match = set.options.single().members.single().matches.single()
        val claimNames = match.claims.keys
            .filterIsInstance<MdocRequestedClaim>()
            .map { it.dataElementName }
            .toSet()
        assertEquals(setOf("given_name", "family_name"), claimNames)
    }

    @Test
    fun `returns full match when credential covers every requested claim`() = runBlocking {
        val doc = mockMdocIssuedDocument(
            claims = listOf(
                mdocClaim("given_name"),
                mdocClaim("family_name"),
            ),
        )
        val documentManager = mockDocumentManager(documents = listOf(doc))
        val request = mockDocRequest(
            nameSpaces = mapOf(
                ISO_MDL_NAMESPACE to mapOf(
                    "given_name" to false,
                    "family_name" to false,
                ),
            ),
        )

        val set = request.toCredentialPresentmentSet(documentManager)

        assertNotNull(set)
        val match = set.options.single().members.single().matches.single()
        assertEquals(2, match.claims.size)
    }

    @Test
    fun `skips candidates that have no credential`() = runBlocking {
        // First doc has no credential (findCredential returns null); second is a real mdoc match.
        val docWithoutCred = mockk<IssuedDocument> {
            every { format } returns MsoMdocFormat(docType = ISO_MDL_DOC_TYPE)
            coEvery { findCredential(any()) } returns null
        }
        val docWithCred = mockMdocIssuedDocument(claims = listOf(mdocClaim("given_name")))
        val documentManager = mockDocumentManager(documents = listOf(docWithoutCred, docWithCred))
        val request = mockDocRequest(
            nameSpaces = mapOf(ISO_MDL_NAMESPACE to mapOf("given_name" to false)),
        )

        val set = request.toCredentialPresentmentSet(documentManager)

        // Note: getValidIssuedMsoMdocDocuments pre-filters out docs whose findCredential() is null,
        // so the second filter inside the extension is defensive. Either way, we expect one match.
        assertNotNull(set)
        assertEquals(1, set.options.single().members.single().matches.size)
    }

    @Test
    fun `skips candidates whose credential is not an MdocCredential`() = runBlocking {
        // findCredential returns a SecureAreaBoundCredential that is NOT MdocCredential.
        val nonMdocCred = mockk<SecureAreaBoundCredential>()
        val docWithWrongCredType = mockk<IssuedDocument> {
            every { format } returns MsoMdocFormat(docType = ISO_MDL_DOC_TYPE)
            coEvery { findCredential(any()) } returns nonMdocCred
        }
        val documentManager = mockDocumentManager(documents = listOf(docWithWrongCredType))
        val request = mockDocRequest(
            nameSpaces = mapOf(ISO_MDL_NAMESPACE to mapOf("given_name" to false)),
        )

        val result = request.toCredentialPresentmentSet(documentManager)

        assertNull(result)
    }

    @Test
    fun `skips candidates whose getClaims throws`() = runBlocking {
        // The extension wraps getClaims in runCatching — a thrown exception (e.g. decoding
        // failure) must not abort the whole matching, just the offending document.
        val brokenCred = mockk<MdocCredential>()
        coEvery { brokenCred.getClaims(any()) } throws IllegalStateException("decoding failed")
        val brokenDoc = mockk<IssuedDocument> {
            every { format } returns MsoMdocFormat(docType = ISO_MDL_DOC_TYPE)
            coEvery { findCredential(any()) } returns brokenCred
        }
        val healthyDoc = mockMdocIssuedDocument(claims = listOf(mdocClaim("given_name")))
        val documentManager = mockDocumentManager(documents = listOf(brokenDoc, healthyDoc))
        val request = mockDocRequest(
            nameSpaces = mapOf(ISO_MDL_NAMESPACE to mapOf("given_name" to false)),
        )

        val set = request.toCredentialPresentmentSet(documentManager)

        // Broken candidate is skipped; healthy one survives → 1 match in the set.
        assertNotNull(set)
        assertEquals(1, set.options.single().members.single().matches.size)
    }

    @Test
    fun `propagates CancellationException from getClaims instead of skipping the candidate`() = runBlocking {
        // Companion to `skips candidates whose getClaims throws`: ordinary exceptions are
        // absorbed and the matching loop moves on, but CancellationException must propagate
        // so the parent coroutine's cancellation is honoured. Regression-guards the explicit
        // `catch (CancellationException) { throw e }` that replaced the prior `runCatching`.
        val brokenCred = mockk<MdocCredential>()
        coEvery { brokenCred.getClaims(any()) } throws CancellationException("scope cancelled")
        val brokenDoc = mockk<IssuedDocument> {
            every { format } returns MsoMdocFormat(docType = ISO_MDL_DOC_TYPE)
            coEvery { findCredential(any()) } returns brokenCred
        }
        // Add a healthy second candidate to prove the loop is NOT given the chance to
        // continue: if cancellation were swallowed, the healthy match would survive and
        // the call would return a non-null set instead of throwing.
        val healthyDoc = mockMdocIssuedDocument(claims = listOf(mdocClaim("given_name")))
        val documentManager = mockDocumentManager(documents = listOf(brokenDoc, healthyDoc))
        val request = mockDocRequest(
            nameSpaces = mapOf(ISO_MDL_NAMESPACE to mapOf("given_name" to false)),
        )

        val thrown = assertFailsWith<CancellationException> {
            request.toCredentialPresentmentSet(documentManager)
        }
        assertEquals("scope cancelled", thrown.message)
    }

    @Test
    fun `returns multiple matches when multiple candidates of the same docType exist`() = runBlocking {
        // Two mDL documents (e.g. issued at different times) — both must surface as separate
        // matches within the same member, so the UI / consent layer can offer a picker.
        val doc1 = mockMdocIssuedDocument(claims = listOf(mdocClaim("given_name")))
        val doc2 = mockMdocIssuedDocument(claims = listOf(mdocClaim("given_name")))
        val documentManager = mockDocumentManager(documents = listOf(doc1, doc2))
        val request = mockDocRequest(
            nameSpaces = mapOf(ISO_MDL_NAMESPACE to mapOf("given_name" to false)),
        )

        val set = request.toCredentialPresentmentSet(documentManager)

        assertNotNull(set)
        assertEquals(2, set.options.single().members.single().matches.size)
    }

    @Test
    fun `preserves intentToRetain on the generated MdocRequestedClaim`() = runBlocking {
        // The verifier signals retain=true on portrait, retain=false on given_name.
        val doc = mockMdocIssuedDocument(
            claims = listOf(
                mdocClaim("given_name"),
                mdocClaim("portrait"),
            ),
        )
        val documentManager = mockDocumentManager(documents = listOf(doc))
        val request = mockDocRequest(
            nameSpaces = mapOf(
                ISO_MDL_NAMESPACE to mapOf(
                    "given_name" to false,
                    "portrait" to true,
                ),
            ),
        )

        val set = request.toCredentialPresentmentSet(documentManager)

        assertNotNull(set)
        val match = set.options.single().members.single().matches.single()
        val byName = match.claims.keys
            .filterIsInstance<MdocRequestedClaim>()
            .associateBy { it.dataElementName }
        assertEquals(false, byName.getValue("given_name").intentToRetain)
        assertEquals(true, byName.getValue("portrait").intentToRetain)
    }

    @Test
    fun `uses CredentialMatchSourceIso18013 with the original DocRequest as source`() = runBlocking {
        // The match must reference back to the originating DocRequest so the response generator
        // can route the disclosed claims to the correct response slot.
        val doc = mockMdocIssuedDocument(claims = listOf(mdocClaim("given_name")))
        val documentManager = mockDocumentManager(documents = listOf(doc))
        val request = mockDocRequest(
            nameSpaces = mapOf(ISO_MDL_NAMESPACE to mapOf("given_name" to false)),
        )

        val set = request.toCredentialPresentmentSet(documentManager)

        assertNotNull(set)
        val match = set.options.single().members.single().matches.single()
        val source = assertIs<CredentialMatchSourceIso18013>(match.source)
        assertSame(source.docRequest, request, "Source must hold the exact DocRequest reference")
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private companion object {
        const val ISO_MDL_DOC_TYPE = "org.iso.18013.5.1.mDL"
        const val ISO_MDL_NAMESPACE = "org.iso.18013.5.1"
    }

    /** A real [MdocClaim] fixture — the matcher compares by namespace + element name. */
    private fun mdocClaim(
        elementName: String,
        namespace: String = ISO_MDL_NAMESPACE,
        docType: String = ISO_MDL_DOC_TYPE,
    ): MdocClaim = MdocClaim(
        displayName = elementName,
        attribute = null,
        docType = docType,
        namespaceName = namespace,
        dataElementName = elementName,
        value = Tstr("dummy"),
    )

    /**
     * Mock an [IssuedDocument] of [docType] backed by an [MdocCredential] exposing [claims].
     */
    private fun mockMdocIssuedDocument(
        docType: String = ISO_MDL_DOC_TYPE,
        claims: List<MdocClaim>,
    ): IssuedDocument {
        val credential = mockk<MdocCredential>()
        coEvery { credential.getClaims(any()) } returns claims
        return mockk<IssuedDocument> {
            every { format } returns MsoMdocFormat(docType = docType)
            coEvery { findCredential(any()) } returns credential
        }
    }

    /** Mock a [DocumentManager] whose `getDocuments` returns [documents]. */
    private fun mockDocumentManager(documents: List<Document>): DocumentManager = mockk {
        every { getDocuments(any()) } returns documents
    }

    /** Mock a [DocRequest] with the given docType and namespace map. */
    private fun mockDocRequest(
        docType: String = ISO_MDL_DOC_TYPE,
        nameSpaces: Map<String, Map<String, Boolean>>,
    ): DocRequest = mockk {
        every { this@mockk.docType } returns docType
        every { this@mockk.nameSpaces } returns nameSpaces
    }
}
