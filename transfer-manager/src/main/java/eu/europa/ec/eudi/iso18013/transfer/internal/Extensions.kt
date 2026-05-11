/*
 * Copyright (c) 2023-2025 European Commission
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
package eu.europa.ec.eudi.iso18013.transfer.internal

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import eu.europa.ec.eudi.wallet.document.DocType
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import org.multipaz.credential.Credential
import java.security.cert.X509Certificate
import java.util.concurrent.Executor

internal val Any.TAG: String
    get() {
        if (this is String) return this
        val fullClassName: String = this::class.qualifiedName ?: this::class.java.typeName
        val outerClassName = fullClassName.substringBefore('$')
        val simplerOuterClassName = outerClassName.substringAfterLast('.')
        return if (simplerOuterClassName.isEmpty()) {
            fullClassName
        } else {
            simplerOuterClassName.removeSuffix("Kt")
        }
    }

internal fun Context.mainExecutor(): Executor {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        mainExecutor
    } else {
        ContextCompat.getMainExecutor(applicationContext)
    }
}

internal val List<X509Certificate>.cn: String
    get() = firstOrNull()
        ?.subjectX500Principal
        ?.name
        ?.split(",")
        ?.map { it.split("=", limit = 2) }
        ?.firstOrNull { it.size == 2 && it[0] == "CN" }
        ?.get(1)
        ?.trim()
        ?: ""

internal suspend fun DocumentManager.getValidIssuedMsoMdocDocuments(docType: DocType): List<IssuedDocument> {
    return getDocuments()
        .filter { it.format is MsoMdocFormat && (it.format as MsoMdocFormat).docType == docType }
        .filterIsInstance<IssuedDocument>()
        .filter { it.findCredential() != null } // Filter out documents without credentials
}

internal suspend fun DocumentManager.getValidIssuedMsoMdocDocumentById(documentId: DocumentId): IssuedDocument {
    return getDocumentById(documentId)
        ?.takeIf { it.format is MsoMdocFormat }
        ?.takeIf { it is IssuedDocument }
        ?.let { it as IssuedDocument }
        ?.takeIf { it.findCredential() != null }
        ?: throw IllegalArgumentException("Invalid document")
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
