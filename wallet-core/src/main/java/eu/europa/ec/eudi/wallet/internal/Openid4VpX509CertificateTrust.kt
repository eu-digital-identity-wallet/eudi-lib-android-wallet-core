/*
 *  Copyright (c) 2023 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package eu.europa.ec.eudi.wallet.internal

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.openid4vp.X509CertificateTrust
import java.security.cert.X509Certificate

internal class Openid4VpX509CertificateTrust(
    private var readerTrustStore: ReaderTrustStore?
): X509CertificateTrust {

    private var readerCertificateChain: List<X509Certificate>? = null
    private var isTrusted: Boolean? = null

    fun setReaderTrustStore(readerTrustStore: ReaderTrustStore) {
        this.readerTrustStore = readerTrustStore
        this.isTrusted = null
    }

    override fun isTrusted(chain: List<X509Certificate>): Boolean {
        readerCertificateChain = chain
        return (readerTrustStore?.validateCertificationTrustPath(chain) ?: true).also {
            isTrusted = it
        }
    }

    fun getTrustResult(): Pair<List<X509Certificate>, Boolean>? =
        readerCertificateChain?.let { chain ->
            chain to (isTrusted ?: isTrusted(chain))
        }
}