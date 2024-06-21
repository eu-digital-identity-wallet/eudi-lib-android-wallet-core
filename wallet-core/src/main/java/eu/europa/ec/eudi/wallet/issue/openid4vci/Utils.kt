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
import eu.europa.ec.eudi.openid4vci.KtorHttpClientFactory
import eu.europa.ec.eudi.wallet.document.CreateDocumentResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import io.ktor.client.plugins.logging.*
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.security.PublicKey
import io.ktor.client.plugins.logging.Logger as KtorLogger

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
    useStrongBox: Boolean = true
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

@get:JvmSynthetic
internal val OpenId4VciManager.Config.ktorHttpClientFactoryWithLogging: KtorHttpClientFactory
    get() = {
        ktorHttpClientFactory().let { client ->
            if (httpLoggingStatus) {
                val baseLogger = OpenId4VciLogger(this)
                client.config {
                    install(Logging) {
                        logger = object : KtorLogger {
                            override fun log(message: String) {
                                baseLogger.log(message)
                            }
                        }
                        level = LogLevel.ALL
                    }
                }
            } else client
        }
    }


