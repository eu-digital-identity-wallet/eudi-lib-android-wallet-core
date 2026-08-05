/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer.response

import java.security.cert.X509Certificate

/**
 * Marker for the relying party registration information resolved for a request.
 */
interface WrpRegistrationInfo

/**
 * A document requested in a device request, used by the validator to determine whether the request
 * asks for more than the relying party registered.
 *
 * @property docType the requested document type
 * @property nameSpaces the requested data element identifiers, per namespace
 */
data class RequestedDocument(
    val docType: String,
    val nameSpaces: Map<String, Set<String>> = emptyMap()
)

/**
 * Validates a relying party registration certificate presented in a request and produces the
 * resolved [WrpRegistrationInfo].
 */
fun interface WrpRegistrationValidator {

    suspend fun validate(
        registrationCertificate: ByteArray?,
        readerAccessChain: List<X509Certificate>,
        requestedDocuments: List<RequestedDocument>
    ): WrpRegistrationInfo
}

/**
 * The ISO/IEC-mdoc credential format identifier, used when reporting requested mdoc attestations.
 */
const val MSO_MDOC_FORMAT: String = "mso_mdoc"

/**
 * The `requestInfo` member of an ISO/IEC 18013-5 `ItemsRequest` that carries the relying party
 * registration certificate (ETSI TS 119 472-2 clause 5.3.2).
 */
const val EU_WRPRC_REQUEST_INFO_KEY: String = "euWrprc"
