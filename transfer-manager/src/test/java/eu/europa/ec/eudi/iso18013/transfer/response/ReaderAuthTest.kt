/*
 * Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer.response

import io.mockk.mockk
import java.security.cert.X509Certificate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReaderAuthTest {

    private lateinit var readerAuth: ByteArray
    private lateinit var readerCertificateChain: List<X509Certificate>

    @BeforeTest
    fun setUp() {
        readerAuth = byteArrayOf(0x01, 0x02)
        readerCertificateChain = listOf(mockk())
    }

    @Test
    fun isVerifiedShouldReturnTrueWhenSignIsValidAndCertIsTrusted() {
        val readerAuthInstance = ReaderAuth(
            readerAuth,
            readerSignIsValid = true,
            readerCertificateChain = readerCertificateChain,
            readerCertificatedIsTrusted = true,
            readerCommonName = "Test CN"
        )

        assertTrue(readerAuthInstance.isVerified)
    }

    @Test
    fun isVerifiedShouldReturnFalseWhenSignIsInvalid() {
        val readerAuthInstance = ReaderAuth(
            readerAuth,
            readerSignIsValid = false,
            readerCertificateChain = readerCertificateChain,
            readerCertificatedIsTrusted = true,
            readerCommonName = "Test CN"
        )

        assertFalse(readerAuthInstance.isVerified)
    }

    @Test
    fun isVerifiedShouldReturnFalseWhenCertIsNotTrusted() {
        val readerAuthInstance = ReaderAuth(
            readerAuth,
            readerSignIsValid = true,
            readerCertificateChain = readerCertificateChain,
            readerCertificatedIsTrusted = false,
            readerCommonName = "Test CN"
        )

        assertFalse(readerAuthInstance.isVerified)
    }

    @Test
    fun isVerifiedShouldReturnFalseWhenBothSignAndCertAreInvalid() {
        val readerAuthInstance = ReaderAuth(
            readerAuth,
            readerSignIsValid = false,
            readerCertificateChain = readerCertificateChain,
            readerCertificatedIsTrusted = false,
            readerCommonName = "Test CN"
        )

        assertFalse(readerAuthInstance.isVerified)
    }
}