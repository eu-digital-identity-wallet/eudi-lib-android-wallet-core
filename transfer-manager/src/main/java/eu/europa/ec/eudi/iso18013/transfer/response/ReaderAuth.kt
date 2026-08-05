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

import java.security.cert.X509Certificate

/**
 * Reader authentication
 *
 * @property readerAuth the reader authentication structure as CBOR encoded [ByteArray]
 * @property readerSignIsValid indicates if the signature of reader authentication is valid
 * @property readerCertificateChain reader auth certificate chain as [List] of [X509Certificate]
 * @property readerCertificatedIsTrusted result of reader auth certificate path validation
 * @property readerCommonName the Common Name (CN) field of the reader authentication certificate
 * @property isVerified indicates if the reader authentication is verified
 */
data class ReaderAuth(
    val readerAuth: ByteArray,
    val readerSignIsValid: Boolean,
    val readerCertificateChain: List<X509Certificate>,
    val readerCertificatedIsTrusted: Boolean,
    val readerCommonName: String,
) {
    val isVerified: Boolean
        get() = readerSignIsValid && readerCertificatedIsTrusted
}