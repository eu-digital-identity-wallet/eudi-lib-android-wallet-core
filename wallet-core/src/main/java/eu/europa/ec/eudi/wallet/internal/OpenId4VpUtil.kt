/*
 * Copyright (c) 2023 European Commission
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

import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.iso18013.transfer.DocItem
import eu.europa.ec.eudi.iso18013.transfer.DocRequest
import eu.europa.ec.eudi.iso18013.transfer.ReaderAuth
import eu.europa.ec.eudi.iso18013.transfer.Request
import eu.europa.ec.eudi.iso18013.transfer.RequestDocument
import eu.europa.ec.eudi.prex.PresentationDefinition

internal object OpenId4VpUtil {

    /* A Simple parser for presentation definition */
    @JvmSynthetic
    fun parsePresentationDefinition(
        documentManager: DocumentManager,
        presentationDefinition: PresentationDefinition,
        readerAuth: ReaderAuth?
    ): Request {
        val inputDescriptor =
            presentationDefinition.inputDescriptors.first()
        val fieldConstraints = inputDescriptor.constraints.fields()

        // find doc type
        val docType = fieldConstraints.find { fieldConstraint ->
            fieldConstraint.paths.first().value == "$.mdoc.doctype"
        }?.filter?.get("const").toString().replace("\"", "")

        // find namespace
        val namespace = fieldConstraints.find { fieldConstraint ->
            fieldConstraint.paths.first().value == "$.mdoc.namespace"
        }?.filter?.get("const").toString().replace("\"", "")

        // find requested fields
        val requestedFields =
            fieldConstraints.filter { fieldConstraint ->
                fieldConstraint.intentToRetain != null
            }.map { fieldConstraint ->
                fieldConstraint.paths.first().value.replace(
                    "$.mdoc.",
                    ""
                ).replace("\"", "")
            }

        return createRequest(documentManager, mapOf(docType to mapOf(namespace to requestedFields)), readerAuth)
    }

    private fun createRequest(
        documentManager: DocumentManager,
        requestedFields: Map<String, Map<String, List<String>>>,
        readerAuth: ReaderAuth?
    ): Request {
        val requestedDocuments = mutableListOf<RequestDocument>()
        requestedFields.forEach { document ->
            // create doc item
            val docItems = mutableListOf<DocItem>()
            document.value.forEach { (namespace, elementIds) ->
                elementIds.forEach { elementId ->
                    docItems.add(DocItem(namespace, elementId))
                }
            }
            val docType = document.key
            documentManager.getDocuments().filter { it.docType == docType }
                .forEach { doc ->
                    requestedDocuments.add(
                        RequestDocument(
                            doc.id,
                            doc.docType,
                            doc.name,
                            doc.requiresUserAuth,
                            DocRequest(docType, docItems, byteArrayOf(0), readerAuth)
                        )
                    )
                }
        }
        return Request(requestedDocuments)
    }
}