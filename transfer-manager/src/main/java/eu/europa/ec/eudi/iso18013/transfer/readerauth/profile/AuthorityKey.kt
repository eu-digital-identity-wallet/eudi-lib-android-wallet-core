/*
 *  Copyright (c) 2023-2026 European Commission
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
package eu.europa.ec.eudi.iso18013.transfer.readerauth.profile

import android.util.Log
import eu.europa.ec.eudi.iso18013.transfer.internal.TAG
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier
import java.security.cert.X509Certificate

class AuthorityKey : ProfileValidation {

    override fun validate(
        chain: List<X509Certificate>,
        trustCA: X509Certificate,
    ): Boolean {
        require(chain.isNotEmpty())
        try {
            // ISO/IEC 18013-5 Annex B, Table B.6 requires every certificate's
            // AuthorityKeyIdentifier to match its issuer's SubjectKeyIdentifier,
            // not only the leaf. Walk every adjacent pair in the chain and fall
            // back to trustCA as the issuer of the last certificate.
            chain.forEachIndexed { index, certificate ->
                val issuer = if (index + 1 < chain.size) chain[index + 1] else trustCA
                if (!aki(certificate).contentEquals(ski(issuer))) {
                    Log.d(this.TAG, "AuthorityKeyIdentifier mismatch at chain index $index")
                    return false
                }
            }
            Log.d(this.TAG, "AuthorityKeyIdentifier: true")
            return true
        } catch (e: Throwable) {
            Log.e(this.TAG, "Error", e)
            return false
        }
    }

    private fun aki(certificate: X509Certificate): ByteArray =
        AuthorityKeyIdentifier.getInstance(
            DEROctetString.getInstance(
                certificate.getExtensionValue(Extension.authorityKeyIdentifier.id),
            ).octets,
        ).keyIdentifier

    private fun ski(certificate: X509Certificate): ByteArray =
        SubjectKeyIdentifier.getInstance(
            DEROctetString.getInstance(
                certificate.getExtensionValue(Extension.subjectKeyIdentifier.id),
            ).octets,
        ).keyIdentifier
}
