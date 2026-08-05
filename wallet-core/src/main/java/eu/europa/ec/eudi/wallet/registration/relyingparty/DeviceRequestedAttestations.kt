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
package eu.europa.ec.eudi.wallet.registration.relyingparty

import eu.europa.ec.eudi.iso18013.transfer.response.MSO_MDOC_FORMAT
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.wallet.registration.ClaimPathElement
import eu.europa.ec.eudi.wallet.registration.CredentialMeta

/**
 * Projects a document requested over ISO/IEC 18013-5 onto the attestation the registered scope is
 * checked against. An mdoc claim path is the namespace and the data element identifier, both of them
 * names (OpenID4VP clause 7.2).
 */
internal fun RequestedDocument.toRequestedAttestation(): RequestedAttestation =
    RequestedAttestation(
        format = MSO_MDOC_FORMAT,
        meta = CredentialMeta(doctypeValue = docType),
        claimPaths = nameSpaces.flatMap { (namespace, elements) ->
            elements.map { element ->
                listOf(ClaimPathElement.Claim(namespace), ClaimPathElement.Claim(element))
            }
        }
    )
