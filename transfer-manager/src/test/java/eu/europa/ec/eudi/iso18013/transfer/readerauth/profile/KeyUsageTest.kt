/*
 * Copyright (c) 2023-2025 European Commission
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
import eu.europa.ec.eudi.iso18013.transfer.readerauth.loadTrustCert
import org.junit.After
import org.junit.Before
import org.mockito.MockedStatic
import java.security.cert.X509Certificate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyUsageTest {

    private lateinit var readerAuthCertificate: List<X509Certificate>
    private lateinit var trustCA: X509Certificate

    private lateinit var validation: ProfileValidation

    private lateinit var mockLog: MockedStatic<Log>

    @Before
    fun setup() {
        readerAuthCertificate = listOf(loadCert())
        trustCA = loadTrustCert()
        validation = KeyUsage()

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

    @After
    fun close() {
        mockLog.close()
    }
}
