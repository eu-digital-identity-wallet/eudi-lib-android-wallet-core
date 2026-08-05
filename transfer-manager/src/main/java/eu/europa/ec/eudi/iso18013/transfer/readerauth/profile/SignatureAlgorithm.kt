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
import eu.europa.ec.eudi.iso18013.transfer.internal.TAG
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import java.security.cert.X509Certificate

class SignatureAlgorithm : ProfileValidation {

    override fun validate(
        chain: List<X509Certificate>,
        trustCA: X509Certificate,
    ): Boolean {
        require(chain.isNotEmpty())
        val readerAuthCertificate = chain.first()
        val result =
            readerAuthCertificate.sigAlgOID == X9ObjectIdentifiers.ecdsa_with_SHA256.id ||
                readerAuthCertificate.sigAlgOID == X9ObjectIdentifiers.ecdsa_with_SHA384.id ||
                readerAuthCertificate.sigAlgOID == X9ObjectIdentifiers.ecdsa_with_SHA512.id
        Log.d(this.TAG, "SignatureAlgorithm: $result")

        return result
    }
}
