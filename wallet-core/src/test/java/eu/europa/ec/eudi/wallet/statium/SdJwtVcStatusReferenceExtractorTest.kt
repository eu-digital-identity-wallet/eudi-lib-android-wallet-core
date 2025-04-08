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

import com.android.identity.securearea.SecureArea
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.statium.StatusIndex
import eu.europa.ec.eudi.statium.StatusReference
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcData
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class SdJwtVcStatusReferenceExtractorTest {

    private val secureAreaMock = mockk<SecureArea>()

    @Before
    fun setup() {
        // Mock the static DefaultSdJwtOps object
        mockkObject(DefaultSdJwtOps)
    }

    @After
    fun tearDown() {
        // Clean up after tests
        unmockkObject(DefaultSdJwtOps)
    }

    @Test
    fun `test extract status reference success`() = runTest {
        // Given
        val expectedUri = "https://example.com/status-list"
        val expectedIndex = 42

        // Create mock claims with status information
        val claims = buildJsonObject {
            put("sub", "1234567890")
            put("name", "John Doe")
            putJsonObject("status") {
                putJsonObject("status_list") {
                    put("uri", expectedUri)
                    put("idx", expectedIndex)
                }
            }
        }

        // Mock the SdJwt result
        val mockSdJwt = createMockSdJwt(claims)

        // Setup the mock behavior
        every { DefaultSdJwtOps.unverifiedIssuanceFrom(any()) } returns Result.success(mockSdJwt)

        // Create document with dummy content (actual content doesn't matter since we mock the parsing)
        val document = createSdJwtDocument("dummy-content")

        // When
        val result = SdJwtStatusReferenceExtractor.extractStatusReference(document)

        // Then
        assertTrue(result.isSuccess)
        val statusReference = result.getOrNull()
        assertEquals(StatusReference(StatusIndex(expectedIndex), expectedUri), statusReference)
    }

    @Test
    fun `test extract fails with wrong document format`() = runTest {
        // Given
        val document = createMsoMdocDocument()

        // When
        val result = SdJwtStatusReferenceExtractor.extractStatusReference(document)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Document format is not SdJwtVcFormat") == true)
    }

    @Test
    fun `test extract fails when status field is missing`() = runTest {
        // Given
        val claims = buildJsonObject {
            put("sub", "1234567890")
            put("name", "John Doe")
            // No status field
        }

        // Mock the SdJwt result
        val mockSdJwt = createMockSdJwt(claims)

        // Setup the mock behavior
        every { DefaultSdJwtOps.unverifiedIssuanceFrom(any()) } returns Result.success(mockSdJwt)

        val document = createSdJwtDocument("dummy-content")

        // When
        val result = SdJwtStatusReferenceExtractor.extractStatusReference(document)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No status list found in SD-JWT VC") == true)
    }

    @Test
    fun `test extract fails when uri field is missing`() = runTest {
        // Given
        val claims = buildJsonObject {
            put("sub", "1234567890")
            putJsonObject("status") {
                putJsonObject("status_list") {
                    put("idx", 42)
                    // No URI field
                }
            }
        }

        // Mock the SdJwt result
        val mockSdJwt = createMockSdJwt(claims)

        // Setup the mock behavior
        every { DefaultSdJwtOps.unverifiedIssuanceFrom(any()) } returns Result.success(mockSdJwt)

        val document = createSdJwtDocument("dummy-content")

        // When
        val result = SdJwtStatusReferenceExtractor.extractStatusReference(document)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No URI found in status list") == true)
    }

    @Test
    fun `test extract fails when idx field is missing`() = runTest {
        // Given
        val claims = buildJsonObject {
            put("sub", "1234567890")
            putJsonObject("status") {
                putJsonObject("status_list") {
                    put("uri", "https://example.com/status-list")
                    // No idx field
                }
            }
        }

        // Mock the SdJwt result
        val mockSdJwt = createMockSdJwt(claims)

        // Setup the mock behavior
        every { DefaultSdJwtOps.unverifiedIssuanceFrom(any()) } returns Result.success(mockSdJwt)

        val document = createSdJwtDocument("dummy-content")

        // When
        val result = SdJwtStatusReferenceExtractor.extractStatusReference(document)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No index found in status list") == true)
    }

    @Test
    fun `test extract fails when parsing fails`() = runTest {
        // Given
        // Setup the mock to return a failure
        every { DefaultSdJwtOps.unverifiedIssuanceFrom(any()) } returns Result.failure(
            IllegalArgumentException("Invalid SD-JWT format")
        )

        val document = createSdJwtDocument("invalid-content")

        // When
        val result = SdJwtStatusReferenceExtractor.extractStatusReference(document)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    // Helper methods

    /**
     * Creates a mock SdJwt object with the given claims
     */
    private fun createMockSdJwt(claims: JsonObject): SdJwt<Pair<String, JsonObject>> {
        val mockJwt = Pair(
            "",
            claims
        )

        return SdJwt(jwt = mockJwt, disclosures = emptyList())
    }

    private fun createSdJwtDocument(sdJwtContent: String): IssuedDocument {
        val documentData = mockk<SdJwtVcData>().apply {
            every { format } returns SdJwtVcFormat(vct = "PID")
        }

        return IssuedDocument(
            id = "test-doc-id",
            name = "Test SD-JWT Doc",
            documentManagerId = "test-manager",
            isCertified = true,
            keyAlias = "test-key",
            secureArea = secureAreaMock,
            createdAt = Instant.now(),
            validFrom = Instant.now(),
            validUntil = Instant.now().plusSeconds(3600),
            issuedAt = Instant.now(),
            issuerProvidedData = sdJwtContent.toByteArray(Charsets.US_ASCII),
            data = documentData
        )
    }

    private fun createMsoMdocDocument(): IssuedDocument {
        val documentData = mockk<MsoMdocData>().apply {
            every { format } returns MsoMdocFormat(docType = "mDL")
        }

        return IssuedDocument(
            id = "test-mso-id",
            name = "Test MSO Doc",
            documentManagerId = "test-manager",
            isCertified = true,
            keyAlias = "test-key",
            secureArea = secureAreaMock,
            createdAt = Instant.now(),
            validFrom = Instant.now(),
            validUntil = Instant.now().plusSeconds(3600),
            issuedAt = Instant.now(),
            issuerProvidedData = byteArrayOf(1, 2, 3, 4),
            data = documentData
        )
    }
}
