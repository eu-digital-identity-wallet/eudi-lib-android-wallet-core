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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData

import kotlinx.serialization.json.JsonElement
import org.multipaz.presentment.TransactionData

/**
 * A transaction data type that returns its processed transaction data in a claim of its own.
 *
 * OpenID4VP recommends in Appendix B.3.3 that each transaction data type define a top-level claim
 * of the Key Binding JWT to return the processed transaction data in, along with the rules that
 * produce it. A type that does so implements this interface; the claims it returns are added to
 * the Key Binding JWT beside the `transaction_data_hashes` of the profile in Appendix B.3.3.1,
 * which the wallet always sends.
 *
 * A type that does not implement this interface is bound by that profile alone.
 */
interface TransactionDataKeyBinding {

    /**
     * Calculates the Key Binding JWT claims that bind a presentation to [transactionData].
     *
     * @param transactionData the transaction data of this type that a single presentation
     * authorizes, in the order in which the request carried them; never empty
     * @return the claims to add to the Key Binding JWT
     * @throws IllegalArgumentException when the transaction data cannot be bound
     */
    fun keyBindingClaims(transactionData: List<TransactionData<*>>): Map<String, JsonElement>
}
