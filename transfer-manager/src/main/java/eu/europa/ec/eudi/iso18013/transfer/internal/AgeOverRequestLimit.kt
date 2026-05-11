/*
 * Copyright (c) 2024-2025 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer.internal

import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.RequestedClaim

/**
 * Enforces the ISO 18013-5 mDL constraint: a Device Response MUST NOT contain more than
 * two `age_over_NN` elements from the `org.iso.18013.5.1` namespace.
 *
 * @throws IllegalArgumentException if the limit would be exceeded.
 */
internal fun IssuedDocument.assertAgeOverRequestLimitForIso18013(
    requestedClaims: Iterable<RequestedClaim>,
): IssuedDocument = apply {
    val docType = (format as? MsoMdocFormat)?.docType ?: return@apply
    if (docType != "org.iso.18013.5.1.mDL") return@apply

    val ageOverCount = requestedClaims
        .filterIsInstance<MdocRequestedClaim>()
        .count {
            it.namespaceName == "org.iso.18013.5.1" &&
                it.dataElementName.startsWith("age_over_")
        }

    require(ageOverCount <= 2) {
        "Device Response is not allowed to have more than two age_over_NN elements"
    }
}