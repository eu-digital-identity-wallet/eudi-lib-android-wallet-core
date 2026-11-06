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

package eu.europa.ec.eudi.wallet.document.credential

import COSE.Message
import COSE.MessageTag
import COSE.Sign1Message
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.wallet.document.internal.getEmbeddedCBORObject
import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.Cbor
import org.multipaz.cose.Cose
import org.multipaz.cose.toCoseLabel
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.mdoc.mso.MobileSecurityObjectParser

class MsoMdocCredentialCertifier() : CredentialCertification {
    override suspend fun certifyCredential(
        credential: SecureAreaBoundCredential,
        issuedCredential: IssuerProvidedCredential,
    ) {
        val data = issuedCredential.data
        val issuerSigned = CBORObject.DecodeFromBytes(data)
        val issuerAuthBytes = issuerSigned["issuerAuth"].EncodeToBytes()

        verifyIssuerSignature(issuerAuthBytes)

        val issuerAuth =
            Message.DecodeFromBytes(issuerAuthBytes, MessageTag.Sign1) as Sign1Message
        val msoBytes = issuerAuth.GetContent().getEmbeddedCBORObject().EncodeToBytes()
        // TODO: deprecated
        val mso = MobileSecurityObjectParser(msoBytes).parse()
        if (mso.deviceKey != credential.secureArea.getKeyInfo(credential.alias).publicKey) {
            val msg = "Public key in MSO does not match the one in the request"
            throw IllegalArgumentException(msg)
        }
        if (credential is MdocCredential) {
            require(mso.docType == credential.docType) {
                "DocType in MSO does not match the one in the request"
            }
        }

        issuerSigned["nameSpaces"]?.let { verifyValueDigests(it, mso) }
        credential.certify(ByteString(data))
    }

    /**
     * Verifies the signature of the MSO with the public key of the document signer certificate,
     * the first certificate of the x5chain (ISO/IEC 18013-5 9.1.2.4).
     *
     * This checks the integrity of the MSO. Whether the document signer is trusted is decided by
     * the issuer trust evaluation of the wallet.
     */
    private suspend fun verifyIssuerSignature(issuerAuthBytes: ByteArray) {
        val coseSign1 = Cbor.decode(issuerAuthBytes).asCoseSign1
        val x5chain = requireNotNull(coseSign1.unprotectedHeaders[Cose.COSE_LABEL_X5CHAIN.toCoseLabel]) {
            "No x5chain in the unprotected header of issuerAuth"
        }.asX509CertChain
        val algorithm = Algorithm.fromCoseAlgorithmIdentifier(
            requireNotNull(coseSign1.protectedHeaders[Cose.COSE_LABEL_ALG.toCoseLabel]) {
                "No alg in the protected header of issuerAuth"
            }.asNumber.toInt()
        )
        Cose.coseSign1Check(
            publicKey = x5chain.certificates.first().ecPublicKey,
            detachedData = null,
            signature = coseSign1,
            signatureAlgorithm = algorithm,
        )
    }

    /**
     * Verifies that the digest of every IssuerSignedItemBytes in [nameSpaces] equals the digest
     * for its digestID in the MSO, as specified in ISO/IEC 18013-5 9.1.2.5.
     */
    private suspend fun verifyValueDigests(
        nameSpaces: CBORObject,
        mso: MobileSecurityObjectParser.MobileSecurityObject
    ) {
        for (nameSpaceKey in nameSpaces.keys) {
            val nameSpace = nameSpaceKey.AsString()
            val digestIds = requireNotNull(mso.getDigestIDs(nameSpace)) {
                "No digests in MSO for namespace $nameSpace"
            }
            for (issuerSignedItemBytes in nameSpaces[nameSpaceKey].values) {
                val issuerSignedItem = issuerSignedItemBytes.getEmbeddedCBORObject()
                val digestId = issuerSignedItem["digestID"].AsInt64Value()
                val expected = requireNotNull(digestIds[digestId]) {
                    "No digest in MSO for digestID $digestId in namespace $nameSpace"
                }
                val actual = Crypto.digest(mso.digestAlgorithm, issuerSignedItemBytes.EncodeToBytes())
                require(expected.contentEquals(actual)) {
                    "Digest mismatch for ${issuerSignedItem["elementIdentifier"].AsString()} in namespace $nameSpace"
                }
            }
        }
    }
}