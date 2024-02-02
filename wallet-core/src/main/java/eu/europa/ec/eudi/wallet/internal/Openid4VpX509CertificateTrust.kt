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

import eu.europa.ec.eudi.iso18013.transfer.ReaderAuth
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.openid4vp.X509CertificateTrust
import java.security.cert.X509Certificate

internal class Openid4VpX509CertificateTrust(
    private var readerTrustStore: ReaderTrustStore?
): X509CertificateTrust {

    private var readerCertificateChain: List<X509Certificate>? = null

    fun setReaderTrustStore(readerTrustStore: ReaderTrustStore) {
        this.readerTrustStore = readerTrustStore
    }

    override fun isTrusted(chain: List<X509Certificate>): Boolean {
        this.readerCertificateChain = chain
        return readerTrustStore?.validateCertificationTrustPath(chain) ?: true
    }

    fun getReaderAuth(): ReaderAuth? {
        return readerTrustStore?.let {
            readerCertificateChain?.let {
                ReaderAuth(
                    byteArrayOf(0),
                    true, /* It is always true as siop-openid4vp library validates it internally and returns a fail status */
                    it,
                    isTrusted(it),
                    getReaderCommonName()
                )
            }
        }
    }

    private fun getReaderCommonName(): String {
        var commonName = ""
        readerCertificateChain?.let { chain ->
            readerTrustStore?.createCertificationTrustPath(chain)?.firstOrNull()?.let {
                it.subjectX500Principal.name.split(",").forEach { line ->
                    val (key, value) = line.split("=", limit = 2)
                    if (key == "CN") commonName = value
                }
            }
        }
        return commonName
    }
}