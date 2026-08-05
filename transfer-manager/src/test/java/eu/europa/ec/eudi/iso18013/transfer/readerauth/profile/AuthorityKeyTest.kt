/*
 * Copyright (c) 2023-2026 European Commission
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
package eu.europa.ec.eudi.iso18013.transfer.readerauth.profile

import android.util.Log
import eu.europa.ec.eudi.iso18013.transfer.mockAndroidLog
import eu.europa.ec.eudi.iso18013.transfer.readerauth.loadCert
import eu.europa.ec.eudi.iso18013.transfer.readerauth.loadInvalidCert
import eu.europa.ec.eudi.iso18013.transfer.readerauth.loadThreeCertChainWithIntermediateAkiMismatch
import eu.europa.ec.eudi.iso18013.transfer.readerauth.loadTrustCert
import eu.europa.ec.eudi.iso18013.transfer.readerauth.loadValidThreeCertChain
import org.junit.After
import org.junit.Before
import org.mockito.MockedStatic
import java.security.cert.X509Certificate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthorityKeyTest {

    private lateinit var readerAuthCertificate: List<X509Certificate>
    private lateinit var trustCA: X509Certificate

    private lateinit var validation: ProfileValidation

    private lateinit var mockLog: MockedStatic<Log>

    @Before
    fun setup() {
        readerAuthCertificate = listOf(loadCert())
        trustCA = loadTrustCert()
        validation = AuthorityKey()

        mockLog = mockAndroidLog()
    }

    @Test
    fun testVerify_Valid() {
        // Call the method under test
        val result = validation.validate(readerAuthCertificate, trustCA)

        // Assert the result
        assertTrue(result)
    }

    @Test
    fun testVerify_Invalid() {
        val invalidCert = listOf(loadInvalidCert())

        // Call the method under test
        val result = validation.validate(invalidCert, trustCA)

        // Assert the result
        assertFalse(result)
    }

    // Issue 114: chains longer than two must have every intermediate's AKI
    // validated against its issuer's SKI, not just the leaf.
    @Test
    fun testVerify_ThreeCertChain_AllMatch_ReturnsTrue() {
        val chain = loadValidThreeCertChain()

        val result = validation.validate(chain, trustCA)

        assertTrue(result)
    }

    @Test
    fun testVerify_ThreeCertChain_IntermediateAkiMismatch_ReturnsFalse() {
        val chain = loadThreeCertChainWithIntermediateAkiMismatch()

        val result = validation.validate(chain, trustCA)

        assertFalse(result)
    }

    @After
    fun close() {
        mockLog.close()
    }
}
