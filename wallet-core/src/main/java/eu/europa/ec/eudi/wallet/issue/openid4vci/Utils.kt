/*
 * Copyright (c) 2024-2025 European Commission
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

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.impl.ECDSA
import eu.europa.ec.eudi.openid4vci.Credential
import eu.europa.ec.eudi.openid4vci.DeferredIssuanceContext
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.credential.IssuerProvidedCredential
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.issue.openid4vci.transformations.extractIssuerMetadata
import eu.europa.ec.eudi.wallet.logging.Logger
import org.bouncycastle.util.encoders.Hex
import org.multipaz.crypto.EcSignature
import java.time.Instant
import java.util.*
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
        toDerEncoded(), ECDSA.getSignatureByteArrayLength(jwsAlgorithm)
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
    val documentFormat = offerOfferedDocument.documentFormat
        ?: return Result.failure(IllegalArgumentException("Unsupported document format"))

    val documentName = offerOfferedDocument
        .configuration
        .display.firstOrNull()?.name ?: run {
        when (documentFormat) {
            is MsoMdocFormat -> documentFormat.docType
            is SdJwtVcFormat -> documentFormat.vct
        }
    }

    return createDocument(
        format = documentFormat,
        createSettings = createDocumentSettings,
        issuerMetadata = offerOfferedDocument.extractIssuerMetadata()
    ).kotlinResult.map {
        it.apply { name = documentName }
    }
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
        if (accessToken.isExpired(timestamp, now)) {
            return null == refreshToken
        } else {
            return false
        }
    }

@JvmSynthetic
internal fun DocumentManager.storeIssuedDocument(
    document: UnsignedDocument,
    credentials: List<Pair<Credential, String>>,
    log: (message: String) -> Unit,
): Result<IssuedDocument> = runCatching {

    val issuerProvidedData = credentials.map { (credential, keyAlias) ->
        require(credential is Credential.Str) { "Credential must be a string" }
        val issuerData = when (document.format) {
            is MsoMdocFormat -> Base64.getUrlDecoder().decode(credential.value)
                .also {
                    log("CBOR bytes: ${Hex.toHexString(it)}")
                }

            is SdJwtVcFormat -> credential.value.also {
                log("SD-JWT-VC: $it")
            }.toByteArray(charset = Charsets.US_ASCII)
        }
        IssuerProvidedCredential(
            publicKeyAlias = keyAlias,
            data = issuerData
        )
    }
    storeIssuedDocument(document, issuerProvidedData).kotlinResult.getOrThrow()
}


