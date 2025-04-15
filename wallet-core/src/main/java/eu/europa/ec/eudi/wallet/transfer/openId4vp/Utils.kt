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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.Vct
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat

@JvmSynthetic
internal suspend fun DocumentManager.getValidIssuedDocumentById(documentId: DocumentId): IssuedDocument {
    return getDocumentById(documentId)
        ?.takeIf { it is IssuedDocument }
        ?.let { it as IssuedDocument }
        ?.takeIf { it.findCredential() != null }
        ?: throw IllegalArgumentException("Invalid document")
}

@JvmSynthetic
internal suspend fun DocumentManager.getValidIssuedSdJwtVcDocuments(vct: Vct): List<IssuedDocument> {
    return getDocuments()
        .filter { it.format is SdJwtVcFormat && (it.format as SdJwtVcFormat).vct == vct }
        .filterIsInstance<IssuedDocument>()
        .filter { it.findCredential() != null }
}