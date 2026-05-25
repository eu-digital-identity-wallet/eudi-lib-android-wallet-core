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

import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.multipaz.credential.Credential
import org.multipaz.crypto.X509CertChain
import org.multipaz.mdoc.response.DeviceResponseParser
import org.multipaz.presentment.CredentialMatchSourceIso18013
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.presentment.CredentialPresentmentSet
import org.multipaz.presentment.CredentialPresentmentSetOption
import org.multipaz.presentment.CredentialPresentmentSetOptionMember
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.request.Requester
import org.multipaz.trustmanagement.TrustMetadata
import org.multipaz.util.Constants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Unit tests for [ProcessedDeviceRequest.generateResponse], focusing on:
 *
 *  - **Policy gate**: when [ReaderAuthPolicy] demands trust that is missing,
 *    [generateResponse] must return an empty `STATUS_GENERAL_ERROR` device response and
 *    **not** sign any documents. This regression-guards against an earlier bug where the
 *    short-circuit branch was missing a `return`, silently falling through to the normal
 *    document loop even when policy said to skip.
 *
 *  - **Per-match iteration**: matches whose source is not [CredentialMatchSourceIso18013]
 *    (e.g. a leaked OpenID4VP match) are skipped silently — the loop continues without
 *    surfacing an error.
 *
 * Strategy: we construct [ProcessedDeviceRequest] directly with controllable `requester` /
 * `trustMetadata` / `readerAuthPolicy`, then exercise [generateResponse] with an empty
 * selection (or a single non-Iso18013 match) so that no real credentials or signing are
 * needed. The resulting response bytes are decoded via [DeviceResponseParser] to assert
 * on the wire-level status code (`STATUS_OK` vs `STATUS_GENERAL_ERROR`) — `documentIds`
 * alone is insufficient because it's empty in both the "skipped by policy" and "normal
 * path with no matches" outcomes.
 */
class ProcessedDeviceRequestTest {

    // ── ReaderAuthPolicy.AlwaysRequire ─────────────────────────────────────────

    @Test
    fun `AlwaysRequire returns STATUS_GENERAL_ERROR when trustMetadata is null`() = runBlocking {
        // Verifier is not trust-verified → AlwaysRequire must short-circuit.
        // This is the regression test for the missing-return skipAllByPolicy bug.
        val processed = buildProcessedRequest(
            trustMetadata = null,
            readerAuthPolicy = ReaderAuthPolicy.AlwaysRequire,
        )

        val response = processed.generateEmptyResponse()

        assertEquals(emptyList(), response.documentIds)
        assertEquals(Constants.DEVICE_RESPONSE_STATUS_GENERAL_ERROR, parseStatus(response))
    }

    @Test
    fun `AlwaysRequire produces STATUS_OK response when trustMetadata is set`() = runBlocking {
        val processed = buildProcessedRequest(
            trustMetadata = TrustMetadata(displayName = "Trusted Verifier"),
            readerAuthPolicy = ReaderAuthPolicy.AlwaysRequire,
        )

        val response = processed.generateEmptyResponse()

        assertEquals(emptyList(), response.documentIds)
        assertEquals(Constants.DEVICE_RESPONSE_STATUS_OK, parseStatus(response))
    }

    // ── ReaderAuthPolicy.EnforceIfPresent ──────────────────────────────────────

    @Test
    fun `EnforceIfPresent returns STATUS_GENERAL_ERROR when readerAuth is present but not trust-verified`() = runBlocking {
        // The verifier presented a cert chain but it's not in the trust store (or signature
        // failed). EnforceIfPresent must skip.
        val processed = buildProcessedRequest(
            trustMetadata = null,
            requester = Requester(certChain = mockk<X509CertChain>()),
            readerAuthPolicy = ReaderAuthPolicy.EnforceIfPresent,
        )

        val response = processed.generateEmptyResponse()

        assertEquals(emptyList(), response.documentIds)
        assertEquals(Constants.DEVICE_RESPONSE_STATUS_GENERAL_ERROR, parseStatus(response))
    }

    @Test
    fun `EnforceIfPresent produces STATUS_OK response when no readerAuth is present`() = runBlocking {
        // No cert chain at all — there's nothing to enforce, so we pass.
        val processed = buildProcessedRequest(
            trustMetadata = null,
            requester = Requester(certChain = null),
            readerAuthPolicy = ReaderAuthPolicy.EnforceIfPresent,
        )

        val response = processed.generateEmptyResponse()

        assertEquals(emptyList(), response.documentIds)
        assertEquals(Constants.DEVICE_RESPONSE_STATUS_OK, parseStatus(response))
    }

    @Test
    fun `EnforceIfPresent produces STATUS_OK response when readerAuth is present and trust-verified`() = runBlocking {
        val processed = buildProcessedRequest(
            trustMetadata = TrustMetadata(displayName = "Trusted"),
            requester = Requester(certChain = mockk<X509CertChain>()),
            readerAuthPolicy = ReaderAuthPolicy.EnforceIfPresent,
        )

        val response = processed.generateEmptyResponse()

        assertEquals(emptyList(), response.documentIds)
        assertEquals(Constants.DEVICE_RESPONSE_STATUS_OK, parseStatus(response))
    }

    // ── ReaderAuthPolicy.DoNotEnforce ──────────────────────────────────────────

    @Test
    fun `DoNotEnforce produces STATUS_OK response regardless of trust state`() = runBlocking {
        // Cert chain present, no trust, no enforcement → still pass.
        val processed = buildProcessedRequest(
            trustMetadata = null,
            requester = Requester(certChain = mockk<X509CertChain>()),
            readerAuthPolicy = ReaderAuthPolicy.DoNotEnforce,
        )

        val response = processed.generateEmptyResponse()

        assertEquals(emptyList(), response.documentIds)
        assertEquals(Constants.DEVICE_RESPONSE_STATUS_OK, parseStatus(response))
    }

    // ── Per-match iteration ────────────────────────────────────────────────────

    @Test
    fun `non-Iso18013 match sources are skipped silently during iteration`() = runBlocking {
        // A match with an OpenID4VP source must be ignored (defensive — shouldn't happen in
        // practice for an mdoc processor, but we don't want it to throw). The result is the
        // same as if the selection were empty: STATUS_OK with no documents.
        val openId4VpMatch = mockk<CredentialPresentmentSetOptionMemberMatch> {
            every { source } returns CredentialMatchSourceOpenID4VP(credentialQuery = mockk())
        }
        val processed = buildProcessedRequest(
            trustMetadata = TrustMetadata(displayName = "Trusted"),
            readerAuthPolicy = ReaderAuthPolicy.DoNotEnforce,
        )

        val result = processed.generateResponse(
            selection = CredentialPresentmentSelection(matches = listOf(openId4VpMatch)),
            keyUnlockData = emptyMap(),
        )

        val success = assertIs<ResponseResult.Success>(result)
        val response = assertIs<DeviceResponse>(success.response)
        assertEquals(emptyList(), response.documentIds)
        assertEquals(Constants.DEVICE_RESPONSE_STATUS_OK, parseStatus(response))
    }

    @Test
    fun `generateResponse propagates CancellationException from suspending work instead of returning Failure`() = runBlocking {
        // The body of generateResponse suspends inside the per-match disclosure path. The
        // first suspending call in the loop is match.credential.requireIssuedDocument(...),
        // which delegates to documentManager.getDocumentById — we throw CancellationException
        // there to simulate cancellation while signing is in progress.
        //
        // The outer `catch (Exception)` must NOT swallow CancellationException; the explicit
        // `catch (CancellationException) { throw e }` placed above it is what this test
        // regression-guards.
        val documentManager = mockk<DocumentManager> {
            every { getDocumentById(any()) } throws CancellationException("scope cancelled")
        }
        val match = mockk<CredentialPresentmentSetOptionMemberMatch> {
            every { source } returns CredentialMatchSourceIso18013(docRequest = mockk(relaxed = true))
            every { credential } returns mockk<Credential>(relaxed = true)
            every { claims } returns emptyMap()
        }
        val processed = ProcessedDeviceRequest(
            documentManager = documentManager,
            sessionTranscript = SESSION_TRANSCRIPT,
            presentmentData = CredentialPresentmentData(emptyList()),
            requester = Requester(certChain = null),
            // Trusted + DoNotEnforce so the policy gate doesn't short-circuit before the loop.
            trustMetadata = TrustMetadata(displayName = "Trusted"),
            readerAuthPolicy = ReaderAuthPolicy.DoNotEnforce,
        )

        val thrown = assertFailsWith<CancellationException> {
            processed.generateResponse(
                selection = CredentialPresentmentSelection(matches = listOf(match)),
                keyUnlockData = emptyMap(),
            )
        }
        assertEquals("scope cancelled", thrown.message)
    }

    // ── presentmentSelections ──────────────────────────────────────────────
    //
    // Asserts that ProcessedDeviceRequest exposes one selection containing every
    // available match across the request, regardless of the tree shape.

    @Test
    fun `presentmentSelections is empty selection when presentmentData has no sets`() {
        val processed = buildProcessedRequest(
            trustMetadata = null,
            presentmentData = CredentialPresentmentData(credentialSets = emptyList()),
        )

        val combinations = processed.presentmentSelections

        assertEquals(1, combinations.size, "expected exactly one combination even for an empty tree")
        assertEquals(emptyList(), combinations.single().matches)
    }

    @Test
    fun `presentmentSelections bundles the single available match`() {
        val match = mockMatch()
        val data = oneSetOneOptionOneMember(matches = listOf(match))

        val combinations = buildProcessedRequest(
            trustMetadata = null,
            presentmentData = data,
        ).presentmentSelections

        assertEquals(1, combinations.size)
        assertEquals(listOf(match), combinations.single().matches)
    }

    @Test
    fun `presentmentSelections bundles every match of the same member into one selection`() {
        // Two stored credentials of the same docType (e.g. two PIDs) appear together in
        // one selection — the consent UI presents both on one screen.
        val mdlA = mockMatch()
        val mdlB = mockMatch()
        val data = oneSetOneOptionOneMember(matches = listOf(mdlA, mdlB))

        val combinations = buildProcessedRequest(
            trustMetadata = null,
            presentmentData = data,
        ).presentmentSelections

        assertEquals(1, combinations.size, "expected one combination, got ${combinations.size}")
        assertEquals(listOf(mdlA, mdlB), combinations.single().matches)
    }

    @Test
    fun `presentmentSelections combines matches across multiple credential sets`() {
        val pidA = mockMatch()
        val mdlA = mockMatch()
        val data = CredentialPresentmentData(
            credentialSets = listOf(
                set(optional = false, members = listOf(listOf(pidA))),
                set(optional = false, members = listOf(listOf(mdlA))),
            ),
        )

        val combinations = buildProcessedRequest(
            trustMetadata = null,
            presentmentData = data,
        ).presentmentSelections

        assertEquals(1, combinations.size)
        assertEquals(listOf(pidA, mdlA), combinations.single().matches)
    }

    @Test
    fun `presentmentSelections picks only the first option in a multi-option set`() {
        // When the verifier offers alternatives ("either A or B"), only the first
        // option's matches are surfaced — the result stays consistent with the
        // verifier's either/or intent.
        val optionAMatch = mockMatch()
        val optionBMatch = mockMatch()
        val data = CredentialPresentmentData(
            credentialSets = listOf(
                CredentialPresentmentSet(
                    optional = false,
                    options = listOf(
                        CredentialPresentmentSetOption(
                            members = listOf(CredentialPresentmentSetOptionMember(matches = listOf(optionAMatch))),
                        ),
                        CredentialPresentmentSetOption(
                            members = listOf(CredentialPresentmentSetOptionMember(matches = listOf(optionBMatch))),
                        ),
                    ),
                ),
            ),
        )

        val combinations = buildProcessedRequest(
            trustMetadata = null,
            presentmentData = data,
        ).presentmentSelections

        assertEquals(1, combinations.size)
        assertEquals(listOf(optionAMatch), combinations.single().matches)
    }

    @Test
    fun `presentmentSelections includes matches from optional sets when available`() {
        // Optional sets contribute their matches as-is; any per-set skip affordance is
        // the consent UI's concern.
        val requiredMatch = mockMatch()
        val optionalMatch = mockMatch()
        val data = CredentialPresentmentData(
            credentialSets = listOf(
                set(optional = false, members = listOf(listOf(requiredMatch))),
                set(optional = true, members = listOf(listOf(optionalMatch))),
            ),
        )

        val combinations = buildProcessedRequest(
            trustMetadata = null,
            presentmentData = data,
        ).presentmentSelections

        assertEquals(1, combinations.size)
        assertEquals(listOf(requiredMatch, optionalMatch), combinations.single().matches)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Run `generateResponse` with an empty [CredentialPresentmentSelection] so that no
     * documents are signed and the test depends only on the policy gate.
     */
    private suspend fun ProcessedDeviceRequest.generateEmptyResponse(): DeviceResponse {
        val result = generateResponse(
            selection = CredentialPresentmentSelection(matches = emptyList()),
            keyUnlockData = emptyMap(),
        )
        val success = assertIs<ResponseResult.Success>(result)
        return assertIs<DeviceResponse>(success.response)
    }

    /** Decode the top-level `DeviceResponse.status` from the wire bytes. */
    private suspend fun parseStatus(response: DeviceResponse): Long =
        DeviceResponseParser(
            encodedDeviceResponse = response.deviceResponseBytes,
            encodedSessionTranscript = response.sessionTranscriptBytes,
        ).parse().status

    private fun buildProcessedRequest(
        trustMetadata: TrustMetadata?,
        requester: Requester = Requester(certChain = null),
        readerAuthPolicy: ReaderAuthPolicy = ReaderAuthPolicy.EnforceIfPresent,
        presentmentData: CredentialPresentmentData = CredentialPresentmentData(emptyList()),
    ): ProcessedDeviceRequest = ProcessedDeviceRequest(
        documentManager = mockk<DocumentManager>(relaxed = true),
        sessionTranscript = SESSION_TRANSCRIPT,
        presentmentData = presentmentData,
        requester = requester,
        trustMetadata = trustMetadata,
        readerAuthPolicy = readerAuthPolicy,
    )

    /** Mock a [CredentialPresentmentSetOptionMemberMatch] with no behaviour — used as an
     *  opaque identity token in the selection-shape assertions. */
    private fun mockMatch(): CredentialPresentmentSetOptionMemberMatch = mockk()

    /**
     * Shorthand for the typical one-set / one-option / one-member shape that
     * [DeviceRequestProcessor.toCredentialPresentmentSet] emits — the matches list models
     * the wallet's stored credentials for the requested docType.
     */
    private fun oneSetOneOptionOneMember(
        matches: List<CredentialPresentmentSetOptionMemberMatch>,
        optional: Boolean = false,
    ): CredentialPresentmentData = CredentialPresentmentData(
        credentialSets = listOf(set(optional = optional, members = listOf(matches))),
    )

    /**
     * Build a [CredentialPresentmentSet] with one option whose members are constructed from
     * the per-member match lists. Used to assemble selection-shape fixtures in the tests
     * without repeating the nested-constructor boilerplate.
     */
    private fun set(
        optional: Boolean,
        members: List<List<CredentialPresentmentSetOptionMemberMatch>>,
    ): CredentialPresentmentSet = CredentialPresentmentSet(
        optional = optional,
        options = listOf(
            CredentialPresentmentSetOption(
                members = members.map { CredentialPresentmentSetOptionMember(matches = it) },
            ),
        ),
    )

    private companion object {
        /**
         * Minimal valid CBOR (a zero unsigned integer). The session transcript is opaque to
         * the response generator we exercise here — no documents to sign means it's only
         * threaded through to the resulting [DeviceResponse], not interpreted as CBOR by
         * any signing routine.
         */
        val SESSION_TRANSCRIPT: ByteArray = byteArrayOf(0)
    }
}
