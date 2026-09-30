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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.openid4vp.Client
import eu.europa.ec.eudi.wallet.trust.ecIntermediateCertificate
import eu.europa.ec.eudi.wallet.trust.ecLeafSignedByIntermediateCertificate
import eu.europa.ec.eudi.wallet.trust.rsaTrustedRootCertificate
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * Tests that [OpenId4VpRequestReaderTrust] trusts a certificate chain only when its reader trust
 * store validates the chain, and returns the verdict recorded for the certificate chain of its
 * request.
 */
class OpenId4VpRequestReaderTrustTest {

    private val chain = listOf(ecLeafSignedByIntermediateCertificate, ecIntermediateCertificate)
    private val client = Client.X509SanDns("verifier.example", chain.first())

    @Test
    fun `a certificate chain is not trusted when no reader trust store is configured`(): Unit = runBlocking {
        val trust = OpenId4VpRequestReaderTrust(readerTrustStore = null)

        assertFalse(trust.isTrusted(chain))
        assertEquals(OpenId4VpReaderAuth.X509(chain, isTrusted = false), trust.authenticationOf(client))
    }

    @Test
    fun `a client without a certificate is not authenticated`(): Unit = runBlocking {
        val trust = OpenId4VpRequestReaderTrust(readerTrustStore(validates = true))
        trust.isTrusted(chain)

        listOf(
            Client.RedirectUri(URI.create("https://verifier.example/cb")),
            Client.Preregistered("verifier", "Verifier"),
            Client.Origin("https://verifier.example"),
        ).forEach { client ->
            assertEquals(OpenId4VpReaderAuth.Absent, trust.authenticationOf(client))
        }
    }

    @Test
    fun `an x509 client with no recorded verdict is not authenticated`() {
        val trust = OpenId4VpRequestReaderTrust(readerTrustStore(validates = true))

        assertFailsWith<IllegalStateException> { trust.authenticationOf(client) }
    }

    @Test
    fun `an x509 client with the verdict for another certificate is not authenticated`(): Unit = runBlocking {
        val trust = OpenId4VpRequestReaderTrust(readerTrustStore(validates = true))
        trust.isTrusted(chain)

        assertFailsWith<IllegalStateException> {
            trust.authenticationOf(Client.X509Hash("other-verifier", rsaTrustedRootCertificate))
        }
    }

    private fun readerTrustStore(validates: Boolean): ReaderTrustStore = mockk {
        every { validateCertificationTrustPath(chain) } returns validates
    }
}
