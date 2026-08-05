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

package eu.europa.ec.eudi.iso18013.transfer.internal

import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch

/**
 * Returns one option that gathers every credential the wallet has for the request.
 *
 * Currently, in an ISO 18013-5 presentment each set carries exactly one option, so
 * this collects all the matches under that option.
 */
internal fun CredentialPresentmentData.flattenToSingleSelection(): CredentialPresentmentSelection {
    val allMatches: List<CredentialPresentmentSetOptionMemberMatch> = credentialSets
        .flatMap { set ->
            val firstOption = set.options.firstOrNull() ?: return@flatMap emptyList()
            firstOption.members.flatMap { it.matches }
        }
    return CredentialPresentmentSelection(matches = allMatches)
}