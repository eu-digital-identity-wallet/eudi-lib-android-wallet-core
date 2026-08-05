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

package eu.europa.ec.eudi.wallet.document.credential

import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkConstructor
import kotlinx.io.bytestring.ByteString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.document.NameSpacedData
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.mdoc.mso.StaticAuthDataParser
import org.multipaz.sdjwt.credential.KeyBoundSdJwtVcCredential
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [CredentialIssuedData] and its extension functions.
 */
class CredentialIssuedDataTest {

    // Mock objects to be used in tests
    private lateinit var mockMdocCredential: MdocCredential
    private lateinit var mockSdJwtVcCredential: KeyBoundSdJwtVcCredential
    private lateinit var mockUnsupportedCredential: SecureAreaBoundCredential
    private lateinit var mockNameSpacedData: NameSpacedData
    private lateinit var mockStaticAuthData: StaticAuthDataParser.StaticAuthData
    private lateinit var mockSdJwt: SdJwt<JwtAndClaims>

    // Sample test data
    private val sampleIssuerProvidedData = byteArrayOf(1, 2, 3, 4) // Simple byte array for testing
    private val sampleSdJwtString =
        "eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK3NkLWp3dCJ9.eyJpYXQiOjE1MTYyMzkwMjIsInN1YiI6IjEyMzQ1Njc4OTAifQ.signature"

    @Before
    fun setUp() {
        // Initialize mocks
        mockMdocCredential = mockk(relaxed = true)
        mockSdJwtVcCredential = mockk(relaxed = true)
        mockUnsupportedCredential = mockk(relaxed = true)
        mockNameSpacedData = mockk(relaxed = true)
        mockStaticAuthData = mockk(relaxed = true)
        mockSdJwt = mockk(relaxed = true)

        // Set up common behavior for mocks to avoid "This credential is not yet certified" error
        every { mockMdocCredential.issuerProvidedData } returns ByteString(sampleIssuerProvidedData)
        every { mockSdJwtVcCredential.issuerProvidedData } returns ByteString(sampleSdJwtString.toByteArray())

        // Mock NameSpacedData.fromIssuerProvidedData extension function
        mockkStatic(NameSpacedData.Companion::fromIssuerProvidedData)
        with(NameSpacedData.Companion) {
            every { fromIssuerProvidedData(any()) } returns mockNameSpacedData
        }
        // Mock StaticAuthDataParser
        mockkConstructor(StaticAuthDataParser::class)
        every { anyConstructed<StaticAuthDataParser>().parse() } returns mockStaticAuthData

        // Mock DefaultSdJwtOps unverifiedIssuanceFrom
        mockkObject(DefaultSdJwtOps)
        every { DefaultSdJwtOps.unverifiedIssuanceFrom(any()) } returns Result.success(mockSdJwt)
    }

    @After
    fun tearDown() {
        unmockkConstructor()
        unmockkAll()
    }

    @Test
    fun `test MsoMdoc data class`() {
        // Create an instance of MsoMdoc
        val msoMdoc = CredentialIssuedData.MsoMdoc(
            nameSpacedData = mockNameSpacedData,
            staticAuthData = mockStaticAuthData
        )

        // Verify properties are set correctly
        assertEquals(mockNameSpacedData, msoMdoc.nameSpacedData)
        assertEquals(mockStaticAuthData, msoMdoc.staticAuthData)
    }

    @Test
    fun `test SdJwtVc data class`() {
        // Create an instance of SdJwtVc
        val sdJwtVc = CredentialIssuedData.SdJwtVc(
            issuedSdJwt = mockSdJwt
        )

        // Verify property is set correctly
        assertEquals(mockSdJwt, sdJwtVc.issuedSdJwt)
    }

    @Test
    fun `test getIssuedData with MdocCredential`() {
        // Test getting data from MdocCredential
        val result = mockMdocCredential.getIssuedData<CredentialIssuedData.MsoMdoc>()

        // Verify result is successful
        assertTrue(result.isSuccess)

        // Verify the result contains expected data
        result.onSuccess { data ->
            assertEquals(mockNameSpacedData, data.nameSpacedData)
            assertEquals(mockStaticAuthData, data.staticAuthData)
        }
    }

    @Test
    fun `test getIssuedData with KeyBoundSdJwtVcCredential`() {

        // Test getting data from KeyBoundSdJwtVcCredential
        val result = mockSdJwtVcCredential.getIssuedData<CredentialIssuedData.SdJwtVc>()

        // Verify result is successful
        assertTrue(result.isSuccess)

        // Verify the result contains expected data
        result.onSuccess { data ->
            assertEquals(mockSdJwt, data.issuedSdJwt)
        }
    }

    @Test
    fun `test getIssuedData with unsupported credential type`() {
        // Create a relaxed mock with direct return value for getIssuerProvidedData
        val unsupportedCredential: SecureAreaBoundCredential = mockk(relaxed = true)

        // Test getting data from an unsupported credential type
        val result = unsupportedCredential.getIssuedData<CredentialIssuedData>()

        // Verify result is failure
        assertTrue(result.isFailure)

        // Verify exception is IllegalArgumentException with correct message
        result.onFailure { exception ->
            assertTrue(exception is IllegalArgumentException)
            assertEquals("Unsupported credential type", exception.message)
        }
    }

    @Test
    fun `test getIssuedData with wrong type cast`() {

        // Try to get SdJwtVc from MdocCredential (incorrect type)
        val result = mockMdocCredential.getIssuedData<CredentialIssuedData.SdJwtVc>()

        // Verify result is failure
        assertTrue(result.isFailure)

        // Verify exception is ClassCastException
        result.onFailure { exception ->
            assertTrue(exception is ClassCastException)
        }
    }
}
