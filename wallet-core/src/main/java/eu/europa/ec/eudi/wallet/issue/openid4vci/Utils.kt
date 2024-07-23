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
import eu.europa.ec.eudi.openid4vci.DeferredIssuanceContext
import eu.europa.ec.eudi.wallet.document.CreateDocumentResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.security.PublicKey
import java.time.Instant
import java.util.concurrent.Executor

/**
 * Converts the [CreateDocumentResult] to a [Result].
 * @receiver the [CreateDocumentResult]
 * @return the [Result]
 */
internal val CreateDocumentResult.result: Result<UnsignedDocument>
    @JvmSynthetic get() = when (this) {
        is CreateDocumentResult.Success -> Result.success(unsignedDocument)
        is CreateDocumentResult.Failure -> Result.failure(throwable)
    }

/**
 * Creates an issuance request for the given document type.
 * @receiver the [DocumentManager]
 * @param offerOfferedDocument the offered document
 * @param useStrongBox whether the key should be hardware backed
 * @return the [Result] with the issuance request
 */
@JvmSynthetic
internal fun DocumentManager.createDocument(
    offerOfferedDocument: Offer.OfferedDocument,
    useStrongBox: Boolean = true,
): Result<UnsignedDocument> =
    createDocument(offerOfferedDocument.docType, useStrongBox)
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

/**
 * Wraps the given [OpenId4VciManager.OnResult] with the given [Executor].
 * @param executor The executor.
 * @receiver The [OpenId4VciManager.OnResult].
 * @return The wrapped [OpenId4VciManager.OnResult].
 */
@JvmSynthetic
internal inline fun <reified V : OpenId4VciResult> OpenId4VciManager.OnResult<V>.runOn(executor: Executor): OpenId4VciManager.OnResult<V> {
    return OpenId4VciManager.OnResult { result: V ->
        executor.execute {
            this@runOn.onResult(result)
        }
    }
}

/**
 * Wraps the given [OpenId4VciManager.OnResult] with debug logging.
 */
@JvmSynthetic
internal inline fun <reified V : OpenId4VciResult> OpenId4VciManager.OnResult<V>.wrapWithLogging(logger: Logger?): OpenId4VciManager.OnResult<V> {
    return when (logger) {
        null -> this
        else -> OpenId4VciManager.OnResult { result: V ->
            when (result) {
                is OpenId4VciResult.Erroneous -> logger.e(TAG, "$result", result.cause)
                else -> logger.d(TAG, "$result")
            }
            this.onResult(result)
        }
    }
}

@get:JvmSynthetic
internal val DeferredIssuanceContext.hasExpired: Boolean
    get() = with(authorizedTransaction.authorizedRequest) {
        when (val rt = refreshToken) {
            null -> accessToken.isExpired(timestamp, Instant.now())
            else -> if (accessToken.isExpired(timestamp, Instant.now())) {
                rt.isExpired(timestamp, Instant.now())
            } else false
        }
    }


