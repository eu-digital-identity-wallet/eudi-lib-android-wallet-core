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

package eu.europa.ec.eudi.iso18013.transfer.response.device

import android.util.Log
import eu.europa.ec.eudi.iso18013.transfer.DeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.KeyLockPassphrase
import eu.europa.ec.eudi.iso18013.transfer.createDocumentManager
import eu.europa.ec.eudi.iso18013.transfer.mockAndroidLog
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.mockito.MockedStatic
import org.multipaz.mdoc.response.DeviceResponseParser
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.securearea.software.SoftwareKeyUnlockData
import org.multipaz.util.Constants
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Integration tests for [DeviceRequestProcessor] using real sample mdoc credentials
 */
class DeviceRequestProcessorTest {

    private lateinit var mockLog: MockedStatic<Log>

    @BeforeTest
    fun setUp() {
        mockLog = mockAndroidLog()
    }

    @AfterTest
    fun tearDown() {
        mockLog.close()
    }

    @Test
    fun `process returns ProcessedDeviceRequest with matched mDL credential and the expected requested claims`() =
        runBlocking {
            val documentManager = createDocumentManager(keyLockPassphrase = null)
            val processor = DeviceRequestProcessor(documentManager)

            val processed = processor.process(DeviceRequest)

            val success = assertIs<ProcessedDeviceRequest>(processed)
            // One CredentialPresentmentSet per DocRequest (here: one) with one matching mDL.
            val match = success.presentmentData.credentialSets.single()
                .options.single().members.single().matches.single()

            val claimsByName = match.claims.keys
                .filterIsInstance<MdocRequestedClaim>()
                .map { it.namespaceName to it.dataElementName }
                .toSet()

            // Soft matching: the verifier asked for five elements but the sample mDL doesn't
            // carry the `Utopia/UtopiaID` element — so it's omitted from the match.
            assertEquals(
                setOf(
                    "org.iso.18013.5.1" to "given_name",
                    "org.iso.18013.5.1" to "birth_date",
                    "org.iso.18013.5.1" to "issue_date",
                    "org.iso.18013.5.1" to "portrait",
                ),
                claimsByName,
            )
        }

    @Test
    fun `process returns Failure when the request is not a DeviceRequest`(): Unit = runBlocking {
        val documentManager = createDocumentManager(keyLockPassphrase = null)
        val processor = DeviceRequestProcessor(documentManager)
        val unknownRequest = mockk<Request>()

        val processed = processor.process(unknownRequest)

        assertIs<RequestProcessor.ProcessedRequest.Failure>(processed)
    }

    @Test
    fun `process plus generateResponse produces a valid signed DeviceResponse end-to-end`() =
        runBlocking {
            val documentManager = createDocumentManager(keyLockPassphrase = null)
            val processor = DeviceRequestProcessor(documentManager)
            val processed = assertIs<ProcessedDeviceRequest>(processor.process(DeviceRequest))
            val match = processed.firstMatch()

            // Full disclosure: the user confirms every matched claim.
            val selection = CredentialPresentmentSelection(matches = listOf(match))
            val result = processed.generateResponse(
                selection = selection,
                keyUnlockData = emptyMap(),
                signatureAlgorithm = null,
            )

            val success = assertIs<ResponseResult.Success>(result)
            val response = assertIs<DeviceResponse>(success.response)
            assertEquals(1, response.documentIds.size)

            val parsed = DeviceResponseParser(
                encodedDeviceResponse = response.deviceResponseBytes,
                encodedSessionTranscript = response.sessionTranscriptBytes,
            ).parse()
            assertEquals(Constants.DEVICE_RESPONSE_STATUS_OK, parsed.status)
            assertEquals(1, parsed.documents.size)
            val doc = parsed.documents.single()
            assertEquals("org.iso.18013.5.1.mDL", doc.docType)
        }

    @Test
    fun `generateResponse includes only the user-selected subset of claims in the issuer-signed payload`() =
        runBlocking {
            // The user confirms only a *subset* of the matched claims (1 of 4). The response
            // must include exactly that subset on the wire — none of the un-confirmed claims
            // may leak through. This is the new-API equivalent of the old `DisclosedDocument`
            // constructor + filtering behaviour: building a selection with a narrower
            // `match.claims` map and verifying it reaches the issuer-signed output.
            val documentManager = createDocumentManager(keyLockPassphrase = null)
            val processor = DeviceRequestProcessor(documentManager)
            val processed = assertIs<ProcessedDeviceRequest>(processor.process(DeviceRequest))
            val fullMatch = processed.firstMatch()

            // Narrow the user's confirmed claims down to `given_name` only.
            val narrowedMatch = fullMatch.copy(
                claims = fullMatch.claims.filterKeys { req ->
                    req is MdocRequestedClaim && req.dataElementName == "given_name"
                },
            )
            val result = processed.generateResponse(
                selection = CredentialPresentmentSelection(matches = listOf(narrowedMatch)),
                keyUnlockData = emptyMap(),
                signatureAlgorithm = null,
            )

            val response = assertIs<DeviceResponse>(assertIs<ResponseResult.Success>(result).response)
            val parsed = DeviceResponseParser(
                encodedDeviceResponse = response.deviceResponseBytes,
                encodedSessionTranscript = response.sessionTranscriptBytes,
            ).parse()
            val disclosed = parsed.documents.single().run {
                issuerNamespaces.flatMap { ns -> getIssuerEntryNames(ns).map { ns to it } }.toSet()
            }
            assertEquals(setOf("org.iso.18013.5.1" to "given_name"), disclosed)
        }

    @Test
    fun `generateResponse with a PIN-locked credential succeeds when matching KeyUnlockData is provided`() =
        runBlocking {
            val documentManager = createDocumentManager(keyLockPassphrase = KeyLockPassphrase)
            val processor = DeviceRequestProcessor(documentManager)
            val processed = assertIs<ProcessedDeviceRequest>(processor.process(DeviceRequest))
            val match = processed.firstMatch()

            // Provide the correct unlock data keyed by the credential's own identifier —
            // the wallet routes the per-credential entry to SecureArea.sign during signing.
            val keyUnlockData = mapOf(
                match.credential.identifier to SoftwareKeyUnlockData(KeyLockPassphrase),
            )

            val result = processed.generateResponse(
                selection = CredentialPresentmentSelection(matches = listOf(match)),
                keyUnlockData = keyUnlockData,
                signatureAlgorithm = null,
            )

            val response = assertIs<DeviceResponse>(assertIs<ResponseResult.Success>(result).response)
            assertEquals(
                response.documentIds.size,
                1,
                "Locked-key signing must produce one signed document"
            )
            val parsed = DeviceResponseParser(
                encodedDeviceResponse = response.deviceResponseBytes,
                encodedSessionTranscript = response.sessionTranscriptBytes,
            ).parse()
            assertEquals(Constants.DEVICE_RESPONSE_STATUS_OK, parsed.status)
        }

    private fun ProcessedDeviceRequest.firstMatch(): CredentialPresentmentSetOptionMemberMatch =
        presentmentData.credentialSets.single()
            .options.single().members.single().matches.single()
}