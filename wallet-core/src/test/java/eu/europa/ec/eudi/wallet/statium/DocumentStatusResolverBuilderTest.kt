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

import eu.europa.ec.eudi.statium.VerifyStatusListTokenSignature
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class DocumentStatusResolverBuilderTest {

    private lateinit var mockVerifySignature: VerifyStatusListTokenSignature
    private lateinit var mockHttpClient: HttpClient
    private lateinit var mockHttpClientFactory: () -> HttpClient
    private lateinit var mockExtractor: StatusReferenceExtractor

    @Before
    fun setup() {
        mockVerifySignature = mockk()
        mockHttpClient = mockk()
        mockHttpClientFactory = { mockHttpClient }
        mockExtractor = mockk()
    }

    @Test
    fun `builder should initialize with default values`() {
        val builder = DocumentStatusResolver.Builder()

        // Assert default values
        assertEquals(VerifyStatusListTokenSignature.x5c::class, builder.verifySignature::class)
        assertEquals(Duration.ZERO, builder.allowedClockSkew)
        assertEquals(DefaultStatusReferenceExtractor, builder.extractor)
    }

    @Test
    fun `builder should apply custom values using individual setters`() {
        val builder = DocumentStatusResolver.Builder()

        // Apply custom values using individual setters
        builder.verifySignature = mockVerifySignature
        builder.ktorHttpClientFactory = mockHttpClientFactory
        builder.allowedClockSkew = 5.minutes
        builder.extractor = mockExtractor

        // Assert custom values
        assertSame(mockVerifySignature, builder.verifySignature)
        assertSame(mockHttpClientFactory, builder.ktorHttpClientFactory)
        assertEquals(5.minutes, builder.allowedClockSkew)
        assertSame(mockExtractor, builder.extractor)
    }

    @Test
    fun `builder should apply custom values using fluent API`() {
        val builder = DocumentStatusResolver.Builder()
            .withVerifySignature(mockVerifySignature)
            .withKtorHttpClientFactory(mockHttpClientFactory)
            .withAllowedClockSkew(10.minutes)
            .withExtractor(mockExtractor)

        // Assert custom values
        assertSame(mockVerifySignature, builder.verifySignature)
        assertSame(mockHttpClientFactory, builder.ktorHttpClientFactory)
        assertEquals(10.minutes, builder.allowedClockSkew)
        assertSame(mockExtractor, builder.extractor)
    }

    @Test
    fun `build should create DocumentStatusResolverImpl with correct parameters`() {
        val resolver = DocumentStatusResolver.Builder()
            .withVerifySignature(mockVerifySignature)
            .withKtorHttpClientFactory(mockHttpClientFactory)
            .withAllowedClockSkew(15.minutes)
            .withExtractor(mockExtractor)
            .build()

        // Assert that the resolver is of the correct type
        assertIs<DocumentStatusResolverImpl>(resolver)

        // Verify that the fields were set correctly
        assertSame(mockVerifySignature, resolver.verifySignature)
        assertSame(mockHttpClientFactory, resolver.ktorHttpClientFactory)
        assertEquals(15.minutes, resolver.allowedClockSkew)
        assertSame(mockExtractor, resolver.extractor)
    }

    @Test
    fun `invoke with lambda should create properly configured resolver`() = runTest {
        val resolver = DocumentStatusResolver {
            verifySignature = mockVerifySignature
            ktorHttpClientFactory = mockHttpClientFactory
            allowedClockSkew = 5.minutes
            extractor = mockExtractor
        }

        // Assert that the resolver is of the correct type
        assertIs<DocumentStatusResolverImpl>(resolver)

        // Verify that the fields were set correctly
        assertSame(mockVerifySignature, resolver.verifySignature)
        assertSame(mockHttpClientFactory, resolver.ktorHttpClientFactory)
        assertEquals(5.minutes, resolver.allowedClockSkew)
        assertSame(mockExtractor, resolver.extractor)
    }

    @Test
    fun `invoke factory method should create resolver with default values`() {
        val resolver = DocumentStatusResolver()

        // Assert that the resolver is of the correct type
        assert(resolver is DocumentStatusResolverImpl)
    }

    @Test
    fun `invoke factory method should create resolver with custom values`() {
        val resolver = DocumentStatusResolver(
            verifySignature = mockVerifySignature,
            ktorHttpClientFactory = mockHttpClientFactory,
            allowedClockSkew = 3.minutes
        )

        // Assert that the resolver is of the correct type
        assertIs<DocumentStatusResolverImpl>(resolver)

        // Verify that the fields were set correctly
        assertSame(mockVerifySignature, resolver.verifySignature)
        assertSame(mockHttpClientFactory, resolver.ktorHttpClientFactory)
        assertEquals(3.minutes, resolver.allowedClockSkew)
        assertSame(DefaultStatusReferenceExtractor, resolver.extractor)
    }
}
