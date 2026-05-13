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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql

import eu.europa.ec.eudi.openid4vp.dcql.CredentialSets
import eu.europa.ec.eudi.openid4vp.dcql.Credentials
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import org.multipaz.presentment.CredentialPresentmentSet
import org.multipaz.presentment.CredentialPresentmentSetOption
import org.multipaz.presentment.CredentialPresentmentSetOptionMember
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch

/**
 * Builds the [CredentialPresentmentSet] tree that represents a verifier's DCQL
 * `credentials` / `credential_sets` rules over the matches found for each query.
 *
 *  - **No `credential_sets`** (OpenID4VP §6.4.2): each [Credentials] entry is required.
 *    Every query must have at least one match, otherwise the request is unsatisfiable
 *    and an empty list is returned. When all queries are satisfied, each becomes its
 *    own non-optional [CredentialPresentmentSet].
 *
 *  - **With `credential_sets`**: each set becomes one [CredentialPresentmentSet]. An
 *    option is "satisfied" iff every credential id it references has at least one match.
 *    Satisfied options become [CredentialPresentmentSetOption]s with one
 *    [CredentialPresentmentSetOptionMember] per id. A required set with no satisfied
 *    option makes the whole request unsatisfiable; optional sets that aren't satisfied
 *    are omitted.
 *
 * Returning an empty list signals "request cannot be satisfied" to the caller.
 */
internal class CredentialSetsMatcher {

    fun toCredentialPresentmentSets(
        credentials: Credentials,
        credentialSets: CredentialSets?,
        matchesByQueryId: Map<QueryId, List<CredentialPresentmentSetOptionMemberMatch>>
    ): List<CredentialPresentmentSet> {
        if (credentialSets == null || credentialSets.value.isEmpty()) {
            // Per OpenID4VP §6.4.2: every credentials entry is required when credential_sets
            // is absent. If any query has no matches, the request is unsatisfiable.
            val sets = mutableListOf<CredentialPresentmentSet>()
            for (query in credentials.value) {
                val matches = matchesByQueryId[query.id].orEmpty()
                if (matches.isEmpty()) return emptyList()
                sets.add(singleQuerySet(matches))
            }
            return sets
        }

        val sets = mutableListOf<CredentialPresentmentSet>()
        for (csq in credentialSets.value) {
            val required = csq.requiredOrDefault
            val options = mutableListOf<CredentialPresentmentSetOption>()

            for (option in csq.options) {
                val ids = option.value
                val allSatisfied = ids.all { id ->
                    !matchesByQueryId[id].isNullOrEmpty()
                }
                if (!allSatisfied) continue

                val members = ids.map { id ->
                    CredentialPresentmentSetOptionMember(matches = matchesByQueryId.getValue(id))
                }
                options.add(CredentialPresentmentSetOption(members = members))
            }

            if (options.isEmpty() && required) {
                // A required set has no satisfied option — the whole request fails.
                return emptyList()
            }
            if (options.isNotEmpty()) {
                sets.add(CredentialPresentmentSet(optional = !required, options = options))
            }
        }
        return sets
    }

    /**
     * Wraps [matches] for a single credential query into a non-optional
     * [CredentialPresentmentSet]. Used when `credential_sets` is absent — per OpenID4VP
     * §6.4.2 every entry in `credentials` is then implicitly required.
     */
    private fun singleQuerySet(
        matches: List<CredentialPresentmentSetOptionMemberMatch>,
    ): CredentialPresentmentSet = CredentialPresentmentSet(
        optional = false,
        options = listOf(
            CredentialPresentmentSetOption(
                members = listOf(CredentialPresentmentSetOptionMember(matches = matches))
            )
        )
    )
}