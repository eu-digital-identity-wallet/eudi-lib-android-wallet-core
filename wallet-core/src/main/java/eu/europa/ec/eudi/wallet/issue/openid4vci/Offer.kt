/*
 * Copyright (c) 2024 European Commission
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

import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer.TxCodeSpec.InputMode.NUMERIC

/**
 * An offer of credentials to be issued.
 * @property issuerName the name of the issuer
 * @property offeredDocuments the items to be issued
 * @property txCodeSpec the specification for the transaction code
 */
interface Offer {

    val issuerName: String
    val offeredDocuments: List<OfferedDocument>
    val txCodeSpec: TxCodeSpec?

    /**
     * An item to be issued.
     * @property name the name of the item
     * @property docType the document type of the item
     */
    interface OfferedDocument {
        val name: String
        val docType: String

        /**
         * Converts this item to a pair of name and document type.
         */
        fun asPair() = Pair(name, docType)

        /**
         * Destructures this item into a pair of name and document type.
         */
        operator fun component1() = name

        /**
         * Destructures this item into a pair of name and document type.
         */
        operator fun component2() = docType

    }

    /**
     * Specification for a transaction code.
     * @property inputMode the input mode for the transaction code
     * @property length the length of the transaction code
     * @property description a description of the transaction code
     */
    data class TxCodeSpec(
        val inputMode: InputMode = NUMERIC,
        val length: Int?,
        val description: String? = null,
    ) {

        /**
         * The input mode for the transaction code.
         * @property NUMERIC the transaction code is numeric
         * @property TEXT the transaction code is text
         */
        enum class InputMode {
            NUMERIC, TEXT
        }
    }
}

