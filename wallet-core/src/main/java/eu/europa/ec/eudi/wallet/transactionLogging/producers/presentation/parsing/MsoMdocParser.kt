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

package eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation.parsing

import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimInfo
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimPath
import kotlinx.coroutines.runBlocking
import org.multipaz.mdoc.response.DeviceResponseParser

/**
 * Parses an mdoc response into one [ClaimInfo] per document, with its `docType` and
 * `[namespace, elementIdentifier]` claim paths. Only paths are read, never values.
 *
 * @param rawResponse the raw device response bytes.
 * @param sessionTranscript the session transcript bytes, or null if not available.
 * @return one [ClaimInfo] per parsed document (paths only).
 */
fun parseMsoMdoc(
    rawResponse: ByteArray,
    sessionTranscript: ByteArray?,
): List<ClaimInfo> {
    val parsed = runBlocking {
        DeviceResponseParser(
            rawResponse,
            sessionTranscript ?: byteArrayOf(0)
        ).parse()
    }
    return parsed.documents.map { doc ->
        val paths = doc.issuerNamespaces.flatMap { ns ->
            doc.getIssuerEntryNames(ns).map { elementIdentifier ->
                ClaimPath.ofKeys(ns, elementIdentifier)
            }
        }
        ClaimInfo(credentialIdentifier = doc.docType, claims = paths)
    }
}
