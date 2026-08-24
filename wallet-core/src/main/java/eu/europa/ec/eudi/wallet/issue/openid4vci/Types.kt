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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.CredentialIdentifier

typealias KeyAlias = String

/**
 * Represents an item to be issued, pairing an [Offer.OfferedDocument] with the
 * payload type required for the credential request.
 *
 * Mirrors the distinction in [eu.europa.ec.eudi.openid4vci.IssuanceRequestPayload]:
 * - [ConfigurationBased]: no credential identifiers were returned by the Authorization Server
 *   the request uses only the credential configuration identifier.
 * - [IdentifierBased]: the Authorization Server returned credential identifiers in the token
 *   response; each identifier represents a distinct credential dataset and requires its own request.
 */
internal sealed interface IssuanceItem {
    val offeredDocument: Offer.OfferedDocument

    data class ConfigurationBased(
        override val offeredDocument: Offer.OfferedDocument,
    ) : IssuanceItem

    data class IdentifierBased(
        override val offeredDocument: Offer.OfferedDocument,
        val credentialIdentifier: CredentialIdentifier,
    ) : IssuanceItem
}