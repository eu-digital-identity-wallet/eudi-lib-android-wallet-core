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

import android.util.Log
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.impl.ECDSA
import eu.europa.ec.eudi.openid4vci.KtorHttpClientFactory
import eu.europa.ec.eudi.wallet.document.CreateIssuanceRequestResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuanceRequest
import eu.europa.ec.eudi.wallet.issue.openid4vci.DefaultOpenId4VciManager.Companion.TAG
import io.ktor.client.plugins.logging.*
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
 * @param offerOfferedDocument the offered document
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

@get:JvmSynthetic
internal val OpenId4VciManager.Config.ktorHttpClientFactoryWithLogging: KtorHttpClientFactory
    get() = {
        ktorHttpClientFactory().let { client ->
            if (httpLoggingStatus) {
                client.config {
                    install(Logging) {
                        logger = object : Logger {
                            override fun log(message: String) {
                                debugLog(TAG, message)
                            }
                        }
                        level = LogLevel.ALL
                    }
                }
            } else client
        }
    }

/**
 * Logs a message with the given tag. If a [Throwable] is provided, it will be logged as an error.
 * If the message is too long, it will be split into multiple log messages.
 * @param tag the tag
 * @param message the message
 * @param throwable the throwable
 */
@JvmSynthetic
internal fun debugLog(tag: String, message: String, throwable: Throwable? = null) {
    val maxLogSize = 1000
    for (i in 0..message.length step maxLogSize) {
        val end = if (i + maxLogSize < message.length) i + maxLogSize else message.length
        if (i == 0 && throwable != null) {
            Log.e(tag, message.substring(i, end), throwable)
            continue
        }
        if (throwable != null) {
            Log.e(tag, message.substring(i, end))
            continue
        }
        Log.d(tag, message.substring(i, end))
    }
}


