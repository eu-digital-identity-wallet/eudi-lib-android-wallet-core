/*
 *  Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.impl.ECDSA
import eu.europa.ec.eudi.wallet.document.CreateIssuanceRequestResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuanceRequest
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.security.PublicKey

/**
 * Converts the [CreateIssuanceRequestResult] to a [Result].
 * @receiver the [CreateIssuanceRequestResult]
 * @return the [Result]
 */
internal val CreateIssuanceRequestResult.result: Result<IssuanceRequest>
    @JvmSynthetic get() = when (this) {
        is CreateIssuanceRequestResult.Success -> Result.success(issuanceRequest)
        is CreateIssuanceRequestResult.Failure -> Result.failure(throwable)
    }

/**
 * Creates an issuance request for the given document type.
 * @receiver the [DocumentManager]
 * @param docType the document type
 * @param hardwareBacked whether the key should be hardware backed
 * @return the [Result] with the issuance request
 */
@JvmSynthetic
internal fun DocumentManager.createIssuanceRequest(
    offerOfferedDocument: Offer.OfferedDocument,
    hardwareBacked: Boolean = true
): Result<IssuanceRequest> =
    createIssuanceRequest(offerOfferedDocument.docType, hardwareBacked)
        .result
        .map { it.apply { name = offerOfferedDocument.name } }

/**
 * Converts the [PublicKey] to a PEM string.
 * @receiver the [PublicKey]
 * @return the PEM string
 */
internal val PublicKey.pem: String
    @JvmSynthetic get() = StringWriter().use { wr ->
        PemWriter(wr).use { pwr ->
            pwr.writeObject(PemObject("PUBLIC KEY", this.encoded))
            pwr.flush()
        }
        wr.toString()
    }

/**
 * Converts a signature in DER format to a concatenated format.
 * @receiver the [ByteArray]
 * @param signatureLength the length of the signature
 * @return the concatenated signature
 */
@JvmSynthetic
internal fun ByteArray.derToConcat(signatureLength: Int) =
    ECDSA.transcodeSignatureToConcat(this, signatureLength)

/**
 * Converts a signature in DER format to a concatenated format.
 * @receiver the [ByteArray]
 * @param algorithm the supported proof algorithm
 * @return the concatenated signature
 */
@JvmSynthetic
internal fun ByteArray.derToConcat(algorithm: SupportedProofType.ProofAlgorithm) =
    derToConcat(algorithm.signatureByteArrayLength)

/**
 * Converts the [ByteArray] to a JOSE signature.
 * @receiver the [ByteArray]
 * @param algorithm the JWS algorithm
 * @return the JOSE signature
 */
@JvmSynthetic
internal fun ByteArray.derToJose(algorithm: JWSAlgorithm = JWSAlgorithm.ES256): ByteArray {
    val len = ECDSA.getSignatureByteArrayLength(algorithm)
    return derToConcat(len)
}


