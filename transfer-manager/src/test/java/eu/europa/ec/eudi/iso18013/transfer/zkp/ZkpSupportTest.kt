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

package eu.europa.ec.eudi.iso18013.transfer.zkp

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.multipaz.mdoc.request.DocRequest
import org.multipaz.mdoc.request.DocRequestInfo
import org.multipaz.mdoc.request.ZkRequest
import org.multipaz.mdoc.zkp.ZkSystem
import org.multipaz.mdoc.zkp.ZkSystemRepository
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.RequestedClaim
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [matchZkSystem].
 *
 * The function returns a [MatchedZkSystem] if:
 *  1. a [ZkSystemRepository] is configured,
 *  2. the verifier's [DocRequest] carries a `zkRequest` with at least one [ZkSystemSpec],
 *  3. the repository knows a [ZkSystem] for at least one of those specs, AND
 *  4. that system's [ZkSystem.getMatchingSystemSpec] returns a non-null spec for the
 *     disclosed (mdoc-only) claims.
 *
 * Otherwise, the function returns `null`.
 */
class ZkpSupportTest {

    @Test
    fun `matchZkSystem returns null when zkSystemRepository is null`() {
        val docRequest = mockk<DocRequest>(relaxed = true)
        val claims = setOf(mdocClaim())

        val result = matchZkSystem(
            zkSystemRepository = null,
            docRequest = docRequest,
            disclosedClaims = claims,
        )

        assertNull(result)
        // Short-circuit guard: should not even peek at the DocRequest.
        verify(exactly = 0) { docRequest.docRequestInfo }
    }

    @Test
    fun `matchZkSystem returns null when docRequest has no docRequestInfo`() {
        val repo = mockk<ZkSystemRepository>()
        val docRequest = mockk<DocRequest> {
            every { docRequestInfo } returns null
        }

        val result = matchZkSystem(
            zkSystemRepository = repo,
            docRequest = docRequest,
            disclosedClaims = setOf(mdocClaim()),
        )

        assertNull(result)
    }

    @Test
    fun `matchZkSystem returns null when docRequest's zkRequest is null`() {
        val repo = mockk<ZkSystemRepository>()
        val docRequest = docRequestWith(zkRequest = null)

        val result = matchZkSystem(
            zkSystemRepository = repo,
            docRequest = docRequest,
            disclosedClaims = setOf(mdocClaim()),
        )

        assertNull(result)
    }

    @Test
    fun `matchZkSystem returns null when systemSpecs is empty`() {
        val repo = mockk<ZkSystemRepository>()
        val docRequest = docRequestWithSpecs(emptyList())

        val result = matchZkSystem(
            zkSystemRepository = repo,
            docRequest = docRequest,
            disclosedClaims = setOf(mdocClaim()),
        )

        assertNull(result)
    }

    @Test
    fun `matchZkSystem returns null when repository has no matching system for any spec`() {
        val spec = ZkSystemSpec(id = "spec-1", system = "unknown-zk")
        val repo = mockk<ZkSystemRepository> {
            every { lookup("unknown-zk") } returns null
        }
        val docRequest = docRequestWithSpecs(listOf(spec))

        val result = matchZkSystem(
            zkSystemRepository = repo,
            docRequest = docRequest,
            disclosedClaims = setOf(mdocClaim()),
        )

        assertNull(result)
    }

    @Test
    fun `matchZkSystem returns null when system exists but no spec is compatible`() {
        val spec = ZkSystemSpec(id = "spec-1", system = "longfellow")
        val zkSystem = mockk<ZkSystem> {
            every { getMatchingSystemSpec(any(), any()) } returns null
        }
        val repo = mockk<ZkSystemRepository> {
            every { lookup("longfellow") } returns zkSystem
        }
        val docRequest = docRequestWithSpecs(listOf(spec))

        val result = matchZkSystem(
            zkSystemRepository = repo,
            docRequest = docRequest,
            disclosedClaims = setOf(mdocClaim()),
        )

        assertNull(result)
    }

    @Test
    fun `matchZkSystem returns MatchedZkSystem when both system and spec are compatible`() {
        val spec = ZkSystemSpec(id = "spec-1", system = "longfellow")
        val matchedSpec = ZkSystemSpec(id = "spec-1-matched", system = "longfellow")
        val zkSystem = mockk<ZkSystem> {
            every { getMatchingSystemSpec(any(), any()) } returns matchedSpec
        }
        val repo = mockk<ZkSystemRepository> {
            every { lookup("longfellow") } returns zkSystem
        }
        val docRequest = docRequestWithSpecs(listOf(spec))

        val result = matchZkSystem(
            zkSystemRepository = repo,
            docRequest = docRequest,
            disclosedClaims = setOf(mdocClaim()),
        )

        assertEquals(MatchedZkSystem(zkSystem, matchedSpec), result)
    }

    @Test
    fun `matchZkSystem returns the first compatible match when multiple specs are available`() {
        // Both specs would match — verify firstOrNull() semantics: pick the earlier one.
        val firstSpec = ZkSystemSpec(id = "spec-a", system = "system-a")
        val secondSpec = ZkSystemSpec(id = "spec-b", system = "system-b")
        val systemA = mockk<ZkSystem> {
            every { getMatchingSystemSpec(any(), any()) } returns firstSpec
        }
        val systemB = mockk<ZkSystem> {
            every { getMatchingSystemSpec(any(), any()) } returns secondSpec
        }
        val repo = mockk<ZkSystemRepository> {
            every { lookup("system-a") } returns systemA
            every { lookup("system-b") } returns systemB
        }
        val docRequest = docRequestWithSpecs(listOf(firstSpec, secondSpec))

        val result = matchZkSystem(
            zkSystemRepository = repo,
            docRequest = docRequest,
            disclosedClaims = setOf(mdocClaim()),
        )

        assertEquals(MatchedZkSystem(systemA, firstSpec), result)
    }

    @Test
    fun `matchZkSystem skips uncompatible specs and returns the next compatible one`() {
        // First spec's system is unknown → null lookup; second is compatible.
        val firstSpec = ZkSystemSpec(id = "missing", system = "unknown")
        val secondSpec = ZkSystemSpec(id = "spec-b", system = "longfellow")
        val matched = ZkSystemSpec(id = "spec-b-matched", system = "longfellow")
        val zkSystem = mockk<ZkSystem> {
            every { getMatchingSystemSpec(any(), any()) } returns matched
        }
        val repo = mockk<ZkSystemRepository> {
            every { lookup("unknown") } returns null
            every { lookup("longfellow") } returns zkSystem
        }
        val docRequest = docRequestWithSpecs(listOf(firstSpec, secondSpec))

        val result = matchZkSystem(
            zkSystemRepository = repo,
            docRequest = docRequest,
            disclosedClaims = setOf(mdocClaim()),
        )

        assertEquals(MatchedZkSystem(zkSystem, matched), result)
    }

    @Test
    fun `matchZkSystem passes only MdocRequestedClaim entries to getMatchingSystemSpec`() {
        // Verify the filterIsInstance<MdocRequestedClaim>() gate: non-mdoc entries must
        // be filtered before delegating to the ZK system
        val spec = ZkSystemSpec(id = "spec-1", system = "longfellow")
        val matched = ZkSystemSpec(id = "spec-1-matched", system = "longfellow")
        val capturedClaims = slot<List<MdocRequestedClaim>>()
        val zkSystem = mockk<ZkSystem> {
            every {
                getMatchingSystemSpec(
                    zkSystemSpecs = any(),
                    requestedClaims = capture(capturedClaims),
                )
            } returns matched
        }
        val repo = mockk<ZkSystemRepository> {
            every { lookup("longfellow") } returns zkSystem
        }
        val docRequest = docRequestWithSpecs(listOf(spec))

        val mdocClaimInstance = mdocClaim()
        matchZkSystem(
            zkSystemRepository = repo,
            docRequest = docRequest,
            disclosedClaims = setOf<RequestedClaim>(
                mdocClaimInstance,
                mockk(), // non-mdoc — must be filtered out
                mockk(), // non-mdoc — must be filtered out
            ),
        )

        assertEquals(listOf(mdocClaimInstance), capturedClaims.captured)
    }

    private fun mdocClaim(): MdocRequestedClaim = MdocRequestedClaim(
        docType = "org.iso.18013.5.1.mDL",
        namespaceName = "org.iso.18013.5.1",
        dataElementName = "given_name",
        intentToRetain = false,
    )

    private fun docRequestWithSpecs(specs: List<ZkSystemSpec>): DocRequest =
        docRequestWith(zkRequest = ZkRequest(systemSpecs = specs, zkRequired = false))

    private fun docRequestWith(zkRequest: ZkRequest?): DocRequest = mockk {
        every { docRequestInfo } returns DocRequestInfo(zkRequest = zkRequest)
    }
}
