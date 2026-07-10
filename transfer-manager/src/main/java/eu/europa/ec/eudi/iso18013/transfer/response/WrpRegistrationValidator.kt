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
 * An attestation requested in a presentation request, used by the validator to determine whether the
 * request asks for more than the relying party registered.
 *
 * @property format the requested attestation format, for example `mso_mdoc`
 * @property docType the ISO/IEC-mdoc document type, when the attestation is an mdoc
 * @property vctValues the SD-JWT VC type values, when the attestation is an SD-JWT VC
 * @property claimPaths the path pointers of the requested claims
 */
data class RequestedAttestationInfo(
    val format: String,
    val docType: String? = null,
    val vctValues: List<String>? = null,
    val claimPaths: List<List<String>> = emptyList()
)

/**
 * Validates a relying party registration certificate presented in a request and produces the
 * resolved [WrpRegistrationInfo].
 */
fun interface WrpRegistrationValidator {

    suspend fun validate(
        registrationCertificate: ByteArray?,
        readerAccessChain: List<X509Certificate>,
        requestedAttestations: List<RequestedAttestationInfo>
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
