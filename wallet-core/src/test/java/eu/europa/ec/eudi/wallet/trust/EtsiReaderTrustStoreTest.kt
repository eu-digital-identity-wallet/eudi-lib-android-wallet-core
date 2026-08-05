/*
 * Copyright (c) 2026 European Commission
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
package eu.europa.ec.eudi.wallet.trust

import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class EtsiReaderTrustStoreTest {

    private val isChainTrusted =
        mockk<IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val chain = listOf(ecLeafSignedByIntermediateCertificate)
    private val trustAnchor = TrustAnchor(rsaTrustedRootCertificate, null)

    @Test
    fun validateCertificationTrustPathReturnsTrueWhenTrusted() {
        coEvery { isChainTrusted.invoke(any(), any()) } returns
                CertificationChainValidation.Trusted(trustAnchor)

        val store = EtsiReaderTrustStore(
            isChainTrusted = isChainTrusted,
            verificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
            coroutineContext = testDispatcher,
        )

        val result = store.validateCertificationTrustPath(chain)
        assertTrue(result)
    }

    @Test
    fun validateCertificationTrustPathReturnsFalseWhenNotTrusted() {
        coEvery { isChainTrusted.invoke(any(), any()) } returns
                CertificationChainValidation.NotTrusted(RuntimeException("not trusted"))

        val store = EtsiReaderTrustStore(
            isChainTrusted = isChainTrusted,
            verificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
            coroutineContext = testDispatcher,
        )

        val result = store.validateCertificationTrustPath(chain)
        assertFalse(result)
    }

    @Test
    fun validateCertificationTrustPathReturnsFalseWhenContextNotConfigured() {
        coEvery { isChainTrusted.invoke(any(), any()) } returns null

        val store = EtsiReaderTrustStore(
            isChainTrusted = isChainTrusted,
            verificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
            coroutineContext = testDispatcher,
        )

        val result = store.validateCertificationTrustPath(chain)
        assertFalse(result)
    }

    @Test
    fun createCertificationTrustPathReturnsChainPlusTrustAnchorWhenTrusted() {
        coEvery { isChainTrusted.invoke(any(), any()) } returns
                CertificationChainValidation.Trusted(trustAnchor)

        val store = EtsiReaderTrustStore(
            isChainTrusted = isChainTrusted,
            verificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
            coroutineContext = testDispatcher,
        )

        val result = store.createCertificationTrustPath(chain)
        assertEquals(chain + trustAnchor.trustedCert, result)
    }

    @Test
    fun createCertificationTrustPathReturnsNullWhenNotTrusted() {
        coEvery { isChainTrusted.invoke(any(), any()) } returns
                CertificationChainValidation.NotTrusted(RuntimeException("not trusted"))

        val store = EtsiReaderTrustStore(
            isChainTrusted = isChainTrusted,
            verificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
            coroutineContext = testDispatcher,
        )

        val result = store.createCertificationTrustPath(chain)
        assertNull(result)
    }

    @Test
    fun createCertificationTrustPathReturnsNullWhenContextNotConfigured() {
        coEvery { isChainTrusted.invoke(any(), any()) } returns null

        val store = EtsiReaderTrustStore(
            isChainTrusted = isChainTrusted,
            verificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
            coroutineContext = testDispatcher,
        )

        val result = store.createCertificationTrustPath(chain)
        assertNull(result)
    }

    @Test
    fun usesProvidedVerificationContext() {
        coEvery { isChainTrusted.invoke(any(), any()) } returns
                CertificationChainValidation.Trusted(trustAnchor)

        val store = EtsiReaderTrustStore(
            isChainTrusted = isChainTrusted,
            verificationContext = VerificationContext.PID,
            coroutineContext = testDispatcher,
        )

        store.validateCertificationTrustPath(chain)

        coVerify { isChainTrusted.invoke(chain, VerificationContext.PID) }
    }

    @Test
    fun usesProvidedCoroutineContext() {
        val dispatcher = UnconfinedTestDispatcher()
        coEvery { isChainTrusted.invoke(any(), any()) } returns
                CertificationChainValidation.Trusted(trustAnchor)

        val store = EtsiReaderTrustStore(
            isChainTrusted = isChainTrusted,
            verificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
            coroutineContext = dispatcher,
        )

        val result = store.validateCertificationTrustPath(chain)
        assertTrue(result)
    }

    @Test
    fun asReaderTrustStoreExtensionCreatesInstance() {
        coEvery { isChainTrusted.invoke(any(), any()) } returns
                CertificationChainValidation.Trusted(trustAnchor)

        val store = isChainTrusted.asReaderTrustStore()

        assertIs<ReaderTrustStore>(store)
        val result = store.validateCertificationTrustPath(chain)
        assertTrue(result)
    }

    @Test
    fun validateReturnsFalseWhenIsChainTrustedThrows() {
        coEvery { isChainTrusted.invoke(any(), any()) } throws RuntimeException("unexpected error")

        val store = EtsiReaderTrustStore(
            isChainTrusted = isChainTrusted,
            verificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
            coroutineContext = testDispatcher,
        )

        val result = store.validateCertificationTrustPath(chain)
        assertFalse(result)
    }

    @Test
    fun createReturnsNullWhenIsChainTrustedThrows() {
        coEvery { isChainTrusted.invoke(any(), any()) } throws RuntimeException("unexpected error")

        val store = EtsiReaderTrustStore(
            isChainTrusted = isChainTrusted,
            verificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
            coroutineContext = testDispatcher,
        )

        val result = store.createCertificationTrustPath(chain)
        assertNull(result)
    }

    @Test
    fun validateReturnsFalseForEmptyChain() {
        coEvery { isChainTrusted.invoke(any(), any()) } returns null

        val store = EtsiReaderTrustStore(
            isChainTrusted = isChainTrusted,
            verificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
            coroutineContext = testDispatcher,
        )

        val result = store.validateCertificationTrustPath(emptyList())
        assertFalse(result)
    }

    @Test
    fun createReturnsNullForEmptyChain() {
        coEvery { isChainTrusted.invoke(any(), any()) } returns null

        val store = EtsiReaderTrustStore(
            isChainTrusted = isChainTrusted,
            verificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
            coroutineContext = testDispatcher,
        )

        val result = store.createCertificationTrustPath(emptyList())
        assertNull(result)
    }
}