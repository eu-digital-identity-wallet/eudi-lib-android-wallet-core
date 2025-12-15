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

package eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing

import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedClaim
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedDocument
import eu.europa.ec.eudi.wallet.util.CBOR
import kotlinx.coroutines.runBlocking
import org.multipaz.mdoc.response.DeviceResponseParser

/**
 * Parses the MSO mdoc response and returns a list of presented documents.
 *
 * @param rawResponse The raw response byte array from the device.
 * @param sessionTranscript The session transcript byte array, or null if not available.
 * @param metadata A list of metadata strings, or null if not available.
 * @return A list of presented documents.
 */
fun parseMsoMdoc(
    rawResponse: ByteArray,
    sessionTranscript: ByteArray?,
    metadata: List<String>
): List<PresentedDocument> {
    // Parse the raw response using the DeviceResponseParser
    val parsed = runBlocking {
        DeviceResponseParser(
            rawResponse,
            sessionTranscript ?: byteArrayOf(0)
        ).parse()
    }

    val parsedMetadata = metadata.map { TransactionLog.Metadata.fromJson(it) }

    // Convert metadata strings to IssuerMetaData objects

    val issuerMetaData = parsedMetadata
        .associate { v ->
            v.index to v.issuerMetadata?.let { IssuerMetadata.fromJson(it) }?.getOrNull()
        }

    // Map parsed documents to PresentedDocument objects
    return parsed.documents.mapIndexed { index, doc ->
        val currentIssuerMetadata = issuerMetaData[index]
        // Extract claims from the document
        val claims = doc.issuerNamespaces.flatMap { nameSpace ->
            doc.getIssuerEntryNames(nameSpace).map { elementIdentifier ->
                val data = doc.getIssuerEntryData(nameSpace, elementIdentifier)
                PresentedClaim(
                    path = listOf(nameSpace, elementIdentifier),
                    value = CBOR.cborParse(data),
                    rawValue = data,
                    metadata = currentIssuerMetadata?.claims?.find {
                        it.path.size == 2 && it.path[0] == nameSpace && it.path[1] == elementIdentifier
                    }
                )
            }
        }
        // Create a PresentedDocument object
        PresentedDocument(
            format = MsoMdocFormat(docType = doc.docType),
            metadata = currentIssuerMetadata,
            claims = claims
        )
    }
}