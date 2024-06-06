/*
 *  Copyright (c) 2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier

/**
 * An offer of credentials to be issued.
 * @property issuerName the name of the issuer
 * @property offeredDocuments the items to be issued
 */
interface Offer {

    val issuerName: String
    val offeredDocuments: List<OfferedDocument>

    /**
     * An item to be issued.
     * @property name the name of the item
     * @property docType the document type of the item
     */
    data class OfferedDocument internal constructor(
        val name: String,
        val docType: String,
        @JvmSynthetic internal val configurationIdentifier: CredentialConfigurationIdentifier,
        @JvmSynthetic internal val configuration: CredentialConfiguration
    ) {
        /**
         * Converts this item to a pair of name and document type.
         */
        fun asPair() = Pair(name, docType)
    }
}

