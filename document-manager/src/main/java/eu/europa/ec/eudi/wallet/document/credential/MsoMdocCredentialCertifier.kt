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
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.mdoc.mso.MobileSecurityObjectParser

class MsoMdocCredentialCertifier() : CredentialCertification {
    override suspend fun certifyCredential(
        credential: SecureAreaBoundCredential,
        issuedCredential: IssuerProvidedCredential,
    ) {
        val data = issuedCredential.data
        val issuerSigned = CBORObject.DecodeFromBytes(data)
        val issuerAuthBytes = issuerSigned["issuerAuth"].EncodeToBytes()
        val issuerAuth =
            Message.DecodeFromBytes(issuerAuthBytes, MessageTag.Sign1) as Sign1Message
        val msoBytes = issuerAuth.GetContent().getEmbeddedCBORObject().EncodeToBytes()
        // TODO: deprecated
        val mso = MobileSecurityObjectParser(msoBytes).parse()
        if (mso.deviceKey != credential.secureArea.getKeyInfo(credential.alias).publicKey) {
            val msg = "Public key in MSO does not match the one in the request"
            throw IllegalArgumentException(msg)
        }
        credential.certify(ByteString(data))
    }
}