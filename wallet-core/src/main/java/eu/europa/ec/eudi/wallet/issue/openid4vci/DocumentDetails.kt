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

import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat

internal interface DocumentDetails {
    val documentId: DocumentId
    val name: String
    val docType: String

    companion object {
        operator fun invoke(document: Document) = object : DocumentDetails {
            override val documentId: DocumentId = document.id
            override val name: String = document.name
            override val docType: String = when (val f = document.format) {
                is MsoMdocFormat -> f.docType
                else -> "Not mso_mdoc format"
            }
        }
    }
}