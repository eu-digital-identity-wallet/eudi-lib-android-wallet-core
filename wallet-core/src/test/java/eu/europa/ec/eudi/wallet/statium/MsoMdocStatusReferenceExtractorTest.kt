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

package eu.europa.ec.eudi.wallet.statium

import COSE.Message
import COSE.MessageTag
import COSE.Sign1Message
import com.android.identity.mdoc.mso.StaticAuthDataParser
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.statium.StatusReference
import eu.europa.ec.eudi.statium.TokenStatusListSpec.IDX
import eu.europa.ec.eudi.statium.TokenStatusListSpec.STATUS
import eu.europa.ec.eudi.statium.TokenStatusListSpec.STATUS_LIST
import eu.europa.ec.eudi.statium.TokenStatusListSpec.URI
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.DocumentData
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs

class MsoMdocStatusReferenceExtractorTest {

    private lateinit var statusListExtractor: MsoMdocStatusReferenceExtractor
    private lateinit var mockDocument: IssuedDocument

    @Before
    fun setUp() {
        statusListExtractor = MsoMdocStatusReferenceExtractor

        // Mock the IssuedDocument
        mockDocument = mockk(relaxed = true)
        val mockDocFormat = MsoMdocFormat(docType = "eu.europa.ec.eudi.pid.1")
        val mockDocData = mockk<DocumentData> {
            every { format } returns mockDocFormat
        }

        every { mockDocument.format } returns mockDocFormat
        every { mockDocument.data } returns mockDocData
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // Tests for parseMso method

    @Test
    fun `parseMso correctly parses valid MSO data`() {
        // Setup
        val mockFormat = MsoMdocFormat(docType = "eu.europa.ec.eudi.pid.1")
        val expectedCborMap = createMockCborMapWithoutStatus()
        val mockDocument = mockk<IssuedDocument> {
            every { format } returns mockFormat
            every { issuerProvidedData } returns ByteArray(10)
        }

        // Mock StaticAuthDataParser and related classes
        val mockStaticAuth = mockk<StaticAuthDataParser.StaticAuthData> {
            every { issuerAuth } returns ByteArray(5)
        }

        mockkConstructor(StaticAuthDataParser::class)
        every { anyConstructed<StaticAuthDataParser>().parse() } returns mockStaticAuth

        // Mock COSE Message
        val mockSign1Message = mockk<Sign1Message> {
            every { GetContent() } returns ByteArray(3)
        }

        mockkStatic(Message::class)
        every { Message.DecodeFromBytes(any(), MessageTag.Sign1) } returns mockSign1Message

        // Mock CBOR decoding
        mockkStatic(CBORObject::class)
        every { CBORObject.DecodeFromBytes(any()) } returnsMany listOf(
            CBORObject.FromObject(ByteArray(2)),
            expectedCborMap
        )

        // Execute
        val result = statusListExtractor.parseMso(mockDocument)

        // Verify
        assertEquals(expectedCborMap, result)
        verify { StaticAuthDataParser(mockDocument.issuerProvidedData) }
        verify { Message.DecodeFromBytes(any(), MessageTag.Sign1) }
        verify(exactly = 2) { CBORObject.DecodeFromBytes(any()) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseMso throws exception for non-MsoMdoc format`() {
        // Setup document with SdJwtVcFormat
        val wrongFormatDoc = mockk<IssuedDocument> {
            every { format } returns SdJwtVcFormat(vct = "TestVct")
        }

        // This should throw IllegalArgumentException
        statusListExtractor.parseMso(wrongFormatDoc)
    }

    @Test(expected = Exception::class)
    fun `parseMso handles StaticAuthDataParser exceptions`() {
        // Setup
        val mockDocument = mockk<IssuedDocument> {
            every { format } returns MsoMdocFormat(docType = "eu.europa.ec.eudi.pid.1")
            every { issuerProvidedData } returns ByteArray(10)
        }

        // Mock StaticAuthDataParser to throw exception
        mockkStatic(StaticAuthDataParser::class)
        every { StaticAuthDataParser(any()) } throws RuntimeException("Parser error")

        // This should propagate the exception
        statusListExtractor.parseMso(mockDocument)
    }

    @Test(expected = Exception::class)
    fun `parseMso handles COSE Message decoding exceptions`() {
        // Setup
        val mockDocument = mockk<IssuedDocument> {
            every { format } returns MsoMdocFormat(docType = "eu.europa.ec.eudi.pid.1")
            every { issuerProvidedData } returns ByteArray(10)
        }

        // Mock StaticAuthDataParser
        val mockStaticAuth = mockk<StaticAuthDataParser> {
            every { parse().issuerAuth } returns ByteArray(5)
        }
        mockkStatic(StaticAuthDataParser::class)
        every { StaticAuthDataParser(any()) } returns mockStaticAuth

        // Mock COSE Message to throw exception
        mockkStatic(Message::class)
        every { Message.DecodeFromBytes(any(), MessageTag.Sign1) } throws RuntimeException("COSE error")

        // This should propagate the exception
        statusListExtractor.parseMso(mockDocument)
    }

    // Original tests for extractRevocationStatusData

    @Test
    fun `extractRevocationStatusData returns status list when valid data is present`() = runTest {
        // Setup CBOR data with valid status info
        val statusUri = "https://example.com/status"
        val statusIdx = 42
        val mockCborMap = createMockCborMapWithStatus(statusUri, statusIdx)

        // Use spy to avoid mocking complex dependencies
        val spiedExtractor = spyk(statusListExtractor)
        coEvery { spiedExtractor.parseMso(mockDocument) } returns mockCborMap

        // Execute test
        val result = spiedExtractor.extractStatusReference(mockDocument)

        // Verify result
        assertTrue(result.isSuccess)
        val statusData = result.getOrNull()
        requireNotNull(statusData)
        assertIs<StatusReference>(statusData)
        assertEquals(statusUri, statusData.uri)
        assertEquals(statusIdx, statusData.index.value)
    }

    @Test
    fun `extractRevocationStatusData returns failure when status is missing`() = runTest {
        // Setup CBOR data without status info
        val mockCborMap = createMockCborMapWithoutStatus()

        // Use spy to avoid mocking complex dependencies
        val spiedExtractor = spyk(statusListExtractor)
        coEvery { spiedExtractor.parseMso(mockDocument) } returns mockCborMap

        // Execute test
        val result = spiedExtractor.extractStatusReference(mockDocument)

        // Verify result
        assertTrue(result.isFailure)
    }

    @Test
    fun `extractRevocationStatusData returns failure when status URI is missing`() = runTest {
        // Setup CBOR data with missing URI
        val statusIdx = 42
        val mockCborMap = createMockCborMapWithMissingUri(statusIdx)

        // Use spy to avoid mocking complex dependencies  
        val spiedExtractor = spyk(statusListExtractor)
        coEvery { spiedExtractor.parseMso(mockDocument) } returns mockCborMap

        // Execute test
        val result = spiedExtractor.extractStatusReference(mockDocument)

        // Verify result
        assertTrue(result.isFailure)
    }

    @Test
    fun `extractRevocationStatusData returns failure when status index is missing`() = runTest {
        // Setup CBOR data with missing index
        val statusUri = "https://example.com/status"
        val mockCborMap = createMockCborMapWithMissingIndex(statusUri)

        // Use spy to avoid mocking complex dependencies
        val spiedExtractor = spyk(statusListExtractor)
        coEvery { spiedExtractor.parseMso(mockDocument) } returns mockCborMap

        // Execute test
        val result = spiedExtractor.extractStatusReference(mockDocument)

        // Verify result
        assertTrue(result.isFailure)
    }

    @Test
    fun `extractRevocationStatusData returns failure for wrong document format`() = runTest {
        // Mock document with wrong format
        val wrongFormatDoc = mockk<IssuedDocument> {
            every { format } returns mockk<DocumentFormat>() // Not MsoMdocFormat
        }

        // Execute test
        val result = statusListExtractor.extractStatusReference(wrongFormatDoc)

        // Verify result
        assertTrue(result.isFailure)
    }

    // Helper methods to create test data

    private fun createMockCborMapWithStatus(statusUri: String, statusIdx: Int): CBORObject {
        return CBORObject.NewMap()
            .Add(
                STATUS, CBORObject.NewMap()
                    .Add(
                        STATUS_LIST, CBORObject.NewMap()
                            .Add(URI, statusUri)
                            .Add(IDX, statusIdx)
                    )
            )
    }

    private fun createMockCborMapWithoutStatus(): CBORObject {
        return CBORObject.NewMap()
            .Add("other_field", "value")
    }

    private fun createMockCborMapWithMissingUri(statusIdx: Int): CBORObject {
        return CBORObject.NewMap()
            .Add(
                STATUS, CBORObject.NewMap()
                    .Add(
                        STATUS_LIST, CBORObject.NewMap()
                            .Add(IDX, statusIdx)
                    )
            )
    }

    private fun createMockCborMapWithMissingIndex(statusUri: String): CBORObject {
        return CBORObject.NewMap()
            .Add(
                STATUS, CBORObject.NewMap()
                    .Add(
                        STATUS_LIST, CBORObject.NewMap()
                            .Add(URI, statusUri)
                    )
            )
    }
}

