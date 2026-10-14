/*
 *  Copyright (c) 2023-2025 European Commission
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
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import java.security.MessageDigest
import java.security.cert.X509Certificate

class SubjectKey : ProfileValidation {

    // SHA-1 is used here solely for SubjectKeyIdentifier verification as specified by
    // RFC 5280 §4.2.1.2 — it is not used in a cryptographic security context.
    // The hash checks structural consistency of the certificate (does the SKI extension
    // match the public key?) and runs only after PKIX chain validation has already
    // verified signatures with modern algorithms. Nearly all CAs generate SKIs using
    // SHA-1 per the RFC, so using a different algorithm would reject legitimate certificates.
    @SuppressWarnings("java:S4790")
    override fun validate(
        chain: List<X509Certificate>,
        trustCA: X509Certificate,
    ): Boolean {
        require(chain.isNotEmpty())
        val readerAuthCertificate = chain.first()
        val byteArray =
            readerAuthCertificate.getExtensionValue(Extension.subjectKeyIdentifier.id) ?: run {
                return false
            }

        val subjectKeyIdentifier: SubjectKeyIdentifier =
            SubjectKeyIdentifier.getInstance(DEROctetString.getInstance(byteArray).octets)

        val publicKeyInfoByteArray: ByteArray = SubjectPublicKeyInfo.getInstance(
            ASN1Sequence.getInstance(readerAuthCertificate.publicKey.encoded),
        ).publicKeyData.octets

        val hash = MessageDigest.getInstance("SHA-1").digest(publicKeyInfoByteArray)

        return subjectKeyIdentifier.keyIdentifier.contentEquals(hash).also {
            Log.d(this.TAG, "SubjectKeyIdentifier: $it")
        }
    }
}
