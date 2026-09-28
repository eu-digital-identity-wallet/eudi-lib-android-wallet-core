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

import org.multipaz.cbor.DataItem
import org.multipaz.presentment.TransactionData

/**
 * A transaction data type that returns its processed transaction data in a data element of its own.
 *
 * OpenID4VP recommends in Appendix B.2.1 that each transaction data type define a data element —
 * a name space, a data element identifier and a value — to return the processed transaction data
 * in, along with the rules that produce it. A type that does so implements this interface; the
 * elements it returns are added to the `DeviceSigned` structure of an ISO/IEC 18013-5 mdoc
 * presentation, and are therefore covered by mdoc authentication.
 *
 * The issuer authorizes the name space in the `KeyAuthorizations` of the mdoc. A request whose
 * data element the mdoc is not authorized to return is rejected.
 */
interface TransactionDataDeviceSigned {

    /** The name space the elements of [deviceSignedElements] belong to. */
    val nameSpace: String

    /**
     * Calculates the `DeviceSigned` data elements that bind a presentation to [transactionData].
     *
     * @param transactionData the transaction data of this type that a single presentation
     * authorizes, in the order in which the request carried them; never empty
     * @return the elements to add to [nameSpace], by data element identifier
     * @throws IllegalArgumentException when the transaction data cannot be bound
     */
    fun deviceSignedElements(transactionData: List<TransactionData<*>>): Map<String, DataItem>
}
