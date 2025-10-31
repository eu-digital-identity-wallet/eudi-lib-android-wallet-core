/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.statium

import COSE.Message
import COSE.MessageTag
import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import eu.europa.ec.eudi.statium.StatusIndex
import eu.europa.ec.eudi.statium.StatusReference
import eu.europa.ec.eudi.statium.TokenStatusListSpec.IDX
import eu.europa.ec.eudi.statium.TokenStatusListSpec.STATUS
import eu.europa.ec.eudi.statium.TokenStatusListSpec.STATUS_LIST
import eu.europa.ec.eudi.statium.TokenStatusListSpec.URI
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import org.multipaz.mdoc.mso.StaticAuthDataParser


/**
 * Implements [StatusReferenceExtractor] for MSO MDOC format.
 * Extracts the status reference from the issuerAuth data of the document.
 */
object MsoMdocStatusReferenceExtractor : StatusReferenceExtractor {
    /**
     * Parses the MSO document and extracts the status reference.
     * If the document format is not [MsoMdocFormat], it throws an [IllegalArgumentException].
     * If the status reference cannot be extracted, it returns a failure result.
     *
     * @param document The issued document to extract the status reference from.
     * @return A [Result] containing the extracted [StatusReference] or an exception if the extraction fails.
     */
    override suspend fun extractStatusReference(document: IssuedDocument): Result<StatusReference> {
        return runCatching { parseMso(document) }
            .mapCatching(::extractStatusList)
    }

    /**
     * Parses the MSO document and extracts the MSO data.
     *
     * @param document The issued document to parse.
     * @return The parsed MSO data as a [CBORObject].
     */

    internal suspend fun parseMso(document: IssuedDocument): CBORObject {
        require(document.format is MsoMdocFormat) {
            "Document format is not MsoMdocFormat"
        }

        val credential = document.findCredential()

        requireNotNull(credential) {
            "No credential found for ${document.name}"
        }

        val issuerAuthBytes = StaticAuthDataParser(credential.issuerProvidedData)
            .parse()
            .issuerAuth

        val issuerAuthSign1 = Message.DecodeFromBytes(issuerAuthBytes, MessageTag.Sign1)

        return issuerAuthSign1
            .GetContent()
            .let { CBORObject.DecodeFromBytes(it) }
            .GetByteString()
            .let { CBORObject.DecodeFromBytes(it) }
    }

    /**
     * Extracts the status reference from the MSO data.
     *
     * @param mso The MSO data as a [CBORObject].
     * @return The extracted [StatusReference].
     */
    internal fun extractStatusList(mso: CBORObject): StatusReference {
        require(mso.type == CBORType.Map) {
            "MSO is not a CBOR map"
        }

        val statusList = mso[STATUS][STATUS_LIST]
        val uri = statusList[URI].AsString()
        val idx = statusList[IDX].AsInt32()

        return StatusReference(
            uri = uri,
            index = StatusIndex(idx),
        )
    }
}
