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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.android.identity.crypto.EcSignature
import com.android.identity.crypto.toDer
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.impl.ECDSA
import eu.europa.ec.eudi.openid4vci.DeferredIssuanceContext
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger
import java.time.Instant
import java.util.concurrent.Executor

/**
 * Transcodes the [EcSignature] to a JOSE encoded byte array.
 *
 * Alternatively, use [EcSignature.toCoseEncoded].
 *
 * @receiver the [EcSignature]
 * @param jwsAlgorithm the JWS algorithm
 * @return the JOSE encoded byte array
 */
internal fun EcSignature.toJoseEncoded(jwsAlgorithm: JWSAlgorithm): ByteArray {
    return ECDSA.transcodeSignatureToConcat(
        toDer(), ECDSA.getSignatureByteArrayLength(jwsAlgorithm)
    )
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
    createDocumentSettings: CreateDocumentSettings,
): Result<UnsignedDocument> {
    val documentFormat = (offerOfferedDocument as DefaultOfferedDocument).documentFormat
        ?: return Result.failure(IllegalArgumentException("Unsupported document format"))

    return createDocument(
        format = documentFormat,
        createSettings = createDocumentSettings,
        documentMetaData = offerOfferedDocument.metaData
    ).kotlinResult.map { it.apply { name = offerOfferedDocument.name } }
}

/**
 * Wraps the given [OpenId4VciManager.OnResult] with the given [Executor].
 * @param executor The executor.
 * @receiver The [OpenId4VciManager.OnResult].
 * @return The wrapped [OpenId4VciManager.OnResult].
 */
@JvmSynthetic
internal inline fun <reified V : OpenId4VciResult> OpenId4VciManager.OnResult<V>.runOn(executor: Executor): OpenId4VciManager.OnResult<V> =
    OpenId4VciManager.OnResult { result: V ->
        executor.execute {
            this@runOn.onResult(result)
        }
    }

/**
 * Wraps the given [OpenId4VciManager.OnResult] with debug logging.
 */
@JvmSynthetic
internal inline fun <reified V : OpenId4VciResult> OpenId4VciManager.OnResult<V>.wrapLogging(
    logger: Logger?,
): OpenId4VciManager.OnResult<V> = logger?.let { l ->
    OpenId4VciManager.OnResult { result: V ->
        when (result) {
            is OpenId4VciResult.Erroneous -> l.e(TAG, "$result", result.cause)
            else -> l.d(TAG, "$result")
        }
        this@wrapLogging.onResult(result)
    }
} ?: this

@get:JvmSynthetic
internal val DeferredIssuanceContext.hasExpired: Boolean
    get() = with(authorizedTransaction.authorizedRequest) {
        val now = Instant.now()
        arrayOf(accessToken, refreshToken)
            .filterNotNull()
            .all { token -> token.isExpired(timestamp, now) }
    }


