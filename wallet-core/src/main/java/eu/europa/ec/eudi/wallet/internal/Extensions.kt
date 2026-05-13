/*
 * Copyright (c) 2023-2024 European Commission
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

package eu.europa.ec.eudi.wallet.internal

import android.content.Context
import android.os.Build
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ReaderTrustResult
import org.multipaz.credential.Credential
import org.multipaz.crypto.X509CertChain
import org.multipaz.crypto.fromJavaX509Certificates
import org.multipaz.request.Requester
import org.multipaz.trustmanagement.TrustMetadata
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.Executor

@JvmSynthetic
internal fun Context.getCertificate(@RawRes resId: Int): X509Certificate =
    resources.openRawResource(resId).use {
        CertificateFactory.getInstance("X509")
            .generateCertificate(it) as X509Certificate

    }

@JvmSynthetic
internal fun Context.mainExecutor(): Executor {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        mainExecutor
    } else {
        ContextCompat.getMainExecutor(applicationContext)
    }
}

/**
 * Maps a [Credential] back to its [IssuedDocument]
 */
internal fun Credential.toIssuedDocument(
    documentManager: DocumentManager
): IssuedDocument? = documentManager.getDocumentById(document.identifier) as? IssuedDocument

/**
 * Maps a [Credential] back to its [IssuedDocument] and throws if the lookup fails.
 */
internal fun Credential.requireIssuedDocument(
    documentManager: DocumentManager
): IssuedDocument = toIssuedDocument(documentManager)
    ?: error("IssuedDocument not found for credential ${document.identifier}")

/**
 * Adapts a [ReaderTrustResult.Processed] into a [Requester] + [TrustMetadata] pair
 * consumed by the presentment layer.
 *
 *  - [Requester] always carries the verified cert chain (and any provided [appId] /
 *    [origin]) regardless of the trust outcome — consumers consult [TrustMetadata]
 *    for the trust verdict.
 *  - [TrustMetadata] is populated only when the chain validated against the
 *    configured `ReaderTrustStore` (`isTrusted == true`). For untrusted readers it
 *    is `null`, which lets UIs render the "unknown verifier" branch.
 *
 * [legalName] is used as the [TrustMetadata.displayName] (e.g. the verifier's legal
 * name extracted by the OpenID4VP layer from `client.legalName()`); it has no effect
 * when the result is untrusted.
 */
internal fun ReaderTrustResult.Processed.toRequesterAndTrust(
    legalName: String? = null,
    appId: String? = null,
    origin: String? = null
): Pair<Requester, TrustMetadata?> {
    val requester = Requester(
        certChain = chain.takeIf { it.isNotEmpty() }
            ?.let { X509CertChain.fromJavaX509Certificates(it) },
        appId = appId,
        origin = origin
    )
    val trustMetadata = if (isTrusted) {
        TrustMetadata(displayName = legalName?.takeIf { it.isNotBlank() })
    } else null
    return requester to trustMetadata
}