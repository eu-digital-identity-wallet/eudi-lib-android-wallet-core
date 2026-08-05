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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

class EtsiCertificateChainTrustTest {

    private val isChainTrusted =
        mockk<IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>>()
    private val adapter = EtsiCertificateChainTrust(isChainTrusted)
    private val chain = listOf(ecLeafSignedByIntermediateCertificate)
    private val trustAnchor = TrustAnchor(rsaTrustedRootCertificate, null)

    @Test
    fun returnsTrueWhenChainIsTrusted() = runTest {
        coEvery {
            isChainTrusted.invoke(chain, VerificationContext.WalletRelyingPartyAccessCertificate)
        } returns CertificationChainValidation.Trusted(trustAnchor)

        assertTrue(adapter.isTrusted(chain))

        coVerify {
            isChainTrusted.invoke(chain, VerificationContext.WalletRelyingPartyAccessCertificate)
        }
    }

    @Test
    fun returnsFalseWhenChainIsNotTrusted() = runTest {
        coEvery {
            isChainTrusted.invoke(chain, VerificationContext.WalletRelyingPartyAccessCertificate)
        } returns CertificationChainValidation.NotTrusted(Exception("not trusted"))

        assertFalse(adapter.isTrusted(chain))
    }

    @Test
    fun returnsFalseWhenContextReturnsNull() = runTest {
        coEvery {
            isChainTrusted.invoke(chain, VerificationContext.WalletRelyingPartyAccessCertificate)
        } returns null

        assertFalse(adapter.isTrusted(chain))
    }

    @Test
    fun returnsFalseOnException() = runTest {
        coEvery {
            isChainTrusted.invoke(chain, VerificationContext.WalletRelyingPartyAccessCertificate)
        } throws RuntimeException("network error")

        assertFalse(adapter.isTrusted(chain))
    }
}
