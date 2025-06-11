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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.openid4vp.X509CertificateTrust
import java.security.cert.X509Certificate

interface OpenId4VpReaderTrust : X509CertificateTrust {
    var readerTrustStore: ReaderTrustStore?
    val result: ReaderTrustResult
}

class OpenId4VpReaderTrustImpl(
    override var readerTrustStore: ReaderTrustStore? = null
) : OpenId4VpReaderTrust {

    private var _result: ReaderTrustResult = ReaderTrustResult.Pending
    override val result: ReaderTrustResult
        get() = _result

    override fun isTrusted(chain: List<X509Certificate>): Boolean {
        val validationResult = readerTrustStore?.validateCertificationTrustPath(chain) != false
        _result = ReaderTrustResult.Processed(chain, validationResult)
        return validationResult
    }
}

sealed interface ReaderTrustResult {
    data class Processed(
        val chain: List<X509Certificate>,
        val isTrusted: Boolean
    ) : ReaderTrustResult

    data object Pending : ReaderTrustResult
}