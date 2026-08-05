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
@file:JvmName("Utils")

package eu.europa.ec.eudi.wallet.document

import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import org.json.JSONObject

/**
 * Extension function to convert [IssuedDocument]'s nameSpacedData to [JSONObject]
 * Applicable only when [IssuedDocument.data] returns [MsoMdocData]
 *
 * @return [JSONObject]
 */
@get:JvmName("nameSpacedDataAsJSONObject")
val IssuedDocument.nameSpacedDataJSONObject: JSONObject
    get() = (data as? MsoMdocData)?.nameSpacedDataDecoded
        ?.let { JSONObject(it) }
        ?: JSONObject()

/**
 * DocumentManager Extension function that returns a list of documents of type [T].
 * If [T] is [IssuedDocument], then only [IssuedDocument] will be returned.
 * If [T] is [UnsignedDocument], then only [UnsignedDocument] will be returned,
 * excluding [DeferredDocument].
 * If [T] is [DeferredDocument], then only [DeferredDocument] will be returned.
 * @receiver DocumentManager
 * @param T The type of document to be returned
 * @return List of documents of type [T]
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Document> DocumentManager.getDocuments(): List<T> {
    return when (T::class) {
        IssuedDocument::class -> getDocuments().filterIsInstance<IssuedDocument>()
        UnsignedDocument::class -> getDocuments().filterIsInstance<UnsignedDocument>()
            .filter { it !is DeferredDocument }

        DeferredDocument::class -> getDocuments().filterIsInstance<DeferredDocument>()
        else -> getDocuments()
    } as List<T>
}