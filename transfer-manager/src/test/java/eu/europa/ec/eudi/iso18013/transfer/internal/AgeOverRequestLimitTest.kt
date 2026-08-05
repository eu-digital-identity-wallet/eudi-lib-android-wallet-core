/*
 * Copyright (c) 2024-2026 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer.internal

import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertThrows
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.RequestedClaim
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Unit tests for [assertAgeOverRequestLimitForIso18013].
 *
 * ISO 18013-5 §7.2.5 caps the number of `age_over_NN` elements (from the
 * `org.iso.18013.5.1` namespace) that a Device Response is allowed to disclose at **two**.
 * The helper enforces this *defensively* on the wallet side — the request must already
 * have been narrowed down to the user's confirmed disclosure set when calling.
 *
 * The function:
 *  - is a no-op for documents that are not mDL (different docType or non-mdoc format);
 *  - counts only [MdocRequestedClaim] entries in the `org.iso.18013.5.1` namespace whose
 *    element name starts with `age_over_`;
 *  - throws [IllegalArgumentException] when the count exceeds 2;
 *  - otherwise returns the receiver unchanged (so the call site can chain).
 */
class AgeOverRequestLimitTest {

    /** A wallet-side mDL document — the only docType subject to the limit. */
    private lateinit var mdlDocument: IssuedDocument

    @BeforeTest
    fun setup() {
        mdlDocument = mockk {
            every { format } returns MsoMdocFormat(docType = ISO_MDL_DOC_TYPE)
        }
    }

    @Test
    fun `passes and returns the same document when a single age_over claim is present`() {
        val claims = setOf(ageOver(18))

        val result = mdlDocument.assertAgeOverRequestLimitForIso18013(claims)

        assertSame(mdlDocument, result)
    }

    @Test
    fun `passes when exactly two age_over claims are present (at the limit)`() {
        val claims = setOf(ageOver(18), ageOver(21))

        val result = mdlDocument.assertAgeOverRequestLimitForIso18013(claims)

        assertSame(mdlDocument, result)
    }

    @Test
    fun `throws when three age_over claims are present (one over the limit)`() {
        val claims = setOf(ageOver(18), ageOver(21), ageOver(65))

        val throwable = assertThrows(IllegalArgumentException::class.java) {
            mdlDocument.assertAgeOverRequestLimitForIso18013(claims)
        }

        assertEquals(
            "Device Response is not allowed to have more than two age_over_NN elements",
            throwable.message
        )
    }

    @Test
    fun `non age_over mdoc claims do not count toward the limit`() {
        // Two age_over plus several unrelated claims — total of five claims, only two count.
        val claims = setOf(
            ageOver(18),
            ageOver(21),
            mdocClaim("given_name"),
            mdocClaim("family_name"),
            mdocClaim("birth_date"),
        )

        val result = mdlDocument.assertAgeOverRequestLimitForIso18013(claims)

        assertSame(mdlDocument, result)
    }

    @Test
    fun `age_over claims in a different namespace are not counted`() {
        // Only the one in the ISO 18013-5 namespace counts.
        val claims = setOf(
            ageOver(18),
            MdocRequestedClaim(
                docType = ISO_MDL_DOC_TYPE,
                namespaceName = "com.example.other",
                dataElementName = "age_over_18",
                intentToRetain = false,
            ),
            MdocRequestedClaim(
                docType = ISO_MDL_DOC_TYPE,
                namespaceName = "com.example.other",
                dataElementName = "age_over_21",
                intentToRetain = false,
            ),
        )

        val result = mdlDocument.assertAgeOverRequestLimitForIso18013(claims)

        assertSame(mdlDocument, result)
    }

    @Test
    fun `non-MdocRequestedClaim entries are ignored when counting`() {
        // Two age_over plus two non-mdoc entries (e.g. from a sd-jwt request) — the
        // filterIsInstance gate is exercised here.
        val claims = setOf<RequestedClaim>(
            ageOver(18),
            ageOver(21),
            mockk(),
            mockk(),
        )

        val result = mdlDocument.assertAgeOverRequestLimitForIso18013(claims)

        assertSame(mdlDocument, result)
    }

    @Test
    fun `is a no-op for documents whose format is not MsoMdocFormat`() {
        // sd-jwt-vc — the early return skips the entire check, even with many age_over claims.
        // Use a real SdJwtVcFormat instance (not a mock) so that `format as? MsoMdocFormat`
        // correctly resolves to null; mocking the sealed parent can pick MsoMdocFormat as
        // the concrete subtype, defeating the test.
        val sdJwtDoc = mockk<IssuedDocument> {
            every { format } returns SdJwtVcFormat(vct = "https://example.com/identity_credential")
        }
        val claims = (1..10).map { ageOver(it) }.toSet()

        val result = sdJwtDoc.assertAgeOverRequestLimitForIso18013(claims)

        assertSame(sdJwtDoc, result)
    }

    @Test
    fun `is a no-op for MsoMdoc documents whose docType is not the ISO mDL`() {
        // mdoc PID or any other mdoc credential — the constraint is mDL-specific.
        val pidDoc = mockk<IssuedDocument> {
            every { format } returns MsoMdocFormat(docType = "eu.europa.ec.eudi.pid.1")
        }
        val claims = (1..10).map { ageOver(it) }.toSet()

        val result = pidDoc.assertAgeOverRequestLimitForIso18013(claims)

        assertSame(pidDoc, result)
    }

    @Test
    fun `passes when claims are empty`() {
        val result = mdlDocument.assertAgeOverRequestLimitForIso18013(emptySet())

        assertSame(mdlDocument, result)
    }
    
    private companion object {
        const val ISO_MDL_DOC_TYPE = "org.iso.18013.5.1.mDL"
        const val ISO_MDL_NAMESPACE = "org.iso.18013.5.1"
    }

    /** Build an `age_over_NN` claim in the ISO 18013-5 mDL namespace. */
    private fun ageOver(n: Int): MdocRequestedClaim = MdocRequestedClaim(
        docType = ISO_MDL_DOC_TYPE,
        namespaceName = ISO_MDL_NAMESPACE,
        dataElementName = "age_over_$n",
        intentToRetain = false,
    )

    /** Build a non-age-over claim in the ISO 18013-5 mDL namespace. */
    private fun mdocClaim(name: String): MdocRequestedClaim = MdocRequestedClaim(
        docType = ISO_MDL_DOC_TYPE,
        namespaceName = ISO_MDL_NAMESPACE,
        dataElementName = name,
        intentToRetain = false,
    )
}