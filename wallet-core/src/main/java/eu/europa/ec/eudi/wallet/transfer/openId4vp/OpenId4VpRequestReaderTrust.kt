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
import eu.europa.ec.eudi.openid4vp.X509CertificateTrust
import java.security.cert.X509Certificate

/**
 * Validates the certificate chain of one OpenID4VP request with [readerTrustStore], and records the
 * verdict for that request.
 *
 * @param readerTrustStore validates the certificate chain. When null, no certificate chain is
 *   trusted.
 */
internal class OpenId4VpRequestReaderTrust(
    private val readerTrustStore: ReaderTrustStore?
) : X509CertificateTrust {

    private var recorded: OpenId4VpReaderAuth.X509? = null

    override suspend fun isTrusted(chain: List<X509Certificate>): Boolean =
        (readerTrustStore?.validateCertificationTrustPath(chain) == true)
            .also { recorded = OpenId4VpReaderAuth.X509(chain, it) }

    /**
     * Returns the authentication of [client], the verifier of the request:
     * [OpenId4VpReaderAuth.Absent] for a client that is not identified by an X.509 certificate, and
     * otherwise the authentication recorded for the certificate chain of the request.
     *
     * @throws IllegalStateException when no verdict was recorded for the certificate of [client]
     */
    fun authenticationOf(client: Client): OpenId4VpReaderAuth {
        val certificate = when (client) {
            is Client.X509SanDns -> client.cert
            is Client.X509Hash -> client.cert
            else -> return OpenId4VpReaderAuth.Absent
        }
        val authentication = recorded
        check(authentication != null && authentication.chain.firstOrNull() == certificate) {
            "No reader trust verdict for the certificate chain of client ${client.id.clientId}"
        }
        return authentication
    }
}
