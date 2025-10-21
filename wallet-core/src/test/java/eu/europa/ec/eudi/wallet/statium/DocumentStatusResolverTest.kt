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

import eu.europa.ec.eudi.statium.GetStatus
import eu.europa.ec.eudi.statium.GetStatusListToken
import eu.europa.ec.eudi.statium.Status
import eu.europa.ec.eudi.statium.StatusIndex
import eu.europa.ec.eudi.statium.StatusReference
import eu.europa.ec.eudi.statium.VerifyStatusListTokenJwtSignature
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import io.ktor.client.HttpClient
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class DocumentStatusResolverTest {

    private lateinit var mockStatusReferenceExtractor: StatusReferenceExtractor
    private lateinit var mockDocument: IssuedDocument
    private lateinit var mockGetStatusListToken: GetStatusListToken
    private lateinit var mockGetStatus: GetStatus
    private val statusReference = StatusReference(
        uri = "https://example.com/status",
        index = StatusIndex(22)
    )
    private val verifySignature = VerifyStatusListTokenJwtSignature { _, _ ->
        Result.success(Unit)
    }
    private lateinit var mockHttpClientFactory: () -> HttpClient

    @Before
    fun setUp() {
        mockStatusReferenceExtractor = mockk<StatusReferenceExtractor>()
        mockDocument = mockk<IssuedDocument>()
        mockGetStatusListToken = mockk()
        mockGetStatus = mockk()
        mockHttpClientFactory = mockk()

        mockkObject(GetStatusListToken.Companion)
        mockkObject(GetStatus.Companion)
    }

    @After
    fun tearDown() {
        unmockkObject(GetStatusListToken.Companion)
        unmockkObject(GetStatus.Companion)
        unmockkAll()
    }


    @Test
    fun `resolveStatus fails when status reference extraction fails`() = runTest {
        // Given
        val extractionError = IllegalArgumentException("Could not extract status reference")
        coEvery { mockStatusReferenceExtractor.extractStatusReference(mockDocument) } returns Result.failure(
            extractionError
        )

        // Create resolver with mock extractor
        val resolver = DocumentStatusResolverImpl(
            verifySignature,
            0.minutes,
            mockHttpClientFactory,
            mockStatusReferenceExtractor,
        )

        // When
        val result = resolver.resolveStatus(mockDocument)

        // Then
        assertTrue(result.isFailure)
        assertEquals(extractionError.message, result.exceptionOrNull()?.message)
    }

    @Test
    fun `companion object creates resolver with default parameters`() {
        // When
        val resolver = DocumentStatusResolver()

        // Then - verify the resolver was created successfully
        assertTrue(resolver is DocumentStatusResolverImpl)
    }

    @Test
    fun `companion object creates resolver with custom parameters`() {
        // Given
        val customVerifySignature = VerifyStatusListTokenJwtSignature { _, _ ->
            Result.success(Unit)
        }

        // When
        val resolver = DocumentStatusResolver(
            verifySignature = customVerifySignature,
            ktorHttpClientFactory = mockHttpClientFactory
        )

        // Then - verify the resolver was created successfully
        assertTrue(resolver is DocumentStatusResolverImpl)
    }

    @Test
    fun `resolveStatus creates GetStatusListToken with correct parameters`() = runTest {
        // Given
        // Setup mock behavior
        coEvery { mockStatusReferenceExtractor.extractStatusReference(mockDocument) } returns Result.success(
            statusReference
        )

        every {
            GetStatusListToken.Companion.usingJwt(
                any(),  // clock
                mockHttpClientFactory(),
                verifySignature,
                any()   // allowedClockSkew
            )
        } returns mockGetStatusListToken

        every {
            GetStatus.Companion.invoke(mockGetStatusListToken)
        } returns mockGetStatus

        coEvery {
            with(mockGetStatus) {
                statusReference.status(at = null)
            }
        } returns Result.success(Status.Valid)

        // Create resolver with mock extractor
        val resolver = DocumentStatusResolverImpl(
            verifySignature,
            0.minutes,
            mockHttpClientFactory,
            mockStatusReferenceExtractor
        )

        // When
        resolver.resolveStatus(mockDocument)

        // Then
        verify(exactly = 1) {
            GetStatusListToken.Companion.usingJwt(
                any(),  // clock
                mockHttpClientFactory(),
                verifySignature,
                0.minutes
            )
        }
    }

    @Test
    fun `resolveStatus passes the correct allowedClockSkew parameter`() = runTest {
        // Given
        val customClockSkew = 10.minutes
        
        // Setup mock behavior
        coEvery { mockStatusReferenceExtractor.extractStatusReference(mockDocument) } returns Result.success(
            statusReference
        )

        every {
            GetStatusListToken.Companion.usingJwt(
                any(),  // clock
                mockHttpClientFactory(),
                verifySignature,
                any()   // allowedClockSkew
            )
        } returns mockGetStatusListToken

        every {
            GetStatus.Companion.invoke(mockGetStatusListToken)
        } returns mockGetStatus

        coEvery {
            with(mockGetStatus) {
                statusReference.currentStatus()
            }
        } returns Result.success(Status.Valid)

        // Create resolver with custom clock skew
        val resolver = DocumentStatusResolverImpl(
            verifySignature,
            customClockSkew,
            mockHttpClientFactory,
            mockStatusReferenceExtractor
        )

        // When
        resolver.resolveStatus(mockDocument)

        // Then
        verify(exactly = 1) {
            GetStatusListToken.Companion.usingJwt(
                any(),  // clock
                mockHttpClientFactory(),
                verifySignature,
                customClockSkew
            )
        }
    }

    @Test
    fun `companion object creates resolver with custom allowedClockSkew`() {
        // Given
        val customClockSkew = 5.minutes
        
        // When
        val resolver = DocumentStatusResolver(
            verifySignature = verifySignature,
            ktorHttpClientFactory = mockHttpClientFactory,
            allowedClockSkew = customClockSkew
        )

        // Then
        assertTrue(resolver is DocumentStatusResolverImpl)
        // We can't directly test the private property, but we'll verify it's created correctly
    }
}

