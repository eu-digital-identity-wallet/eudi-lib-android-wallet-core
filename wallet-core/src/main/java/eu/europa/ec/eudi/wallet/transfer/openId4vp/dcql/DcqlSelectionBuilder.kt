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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql

import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.presentment.CredentialPresentmentSet
import org.multipaz.presentment.CredentialPresentmentSetOption
import org.multipaz.presentment.CredentialPresentmentSetOptionMember
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch

/**
 * Builds the list of options the user can choose from, applying each query's
 * `multiple` flag:
 *  - `multiple = false` (default): each candidate credential becomes its own option.
 *  - `multiple = true`: all candidates of the query are grouped into one option.
 *
 * Each member is matched back to its query through the match source; missing sources
 * are treated as `multiple = false`. Optional sets add an extra "skip" option so the
 * user can leave them out. The final list combines all per-member options across
 * credential sets and their members.
 *
 * @param data the input presentment tree.
 * @param multipleByQueryId the `multiple` flag per query (missing keys default to `false`).
 * @return the list of options, or an empty list when nothing can satisfy the request.
 */
internal fun buildMultipleAwareSelections(
    data: CredentialPresentmentData,
    multipleByQueryId: Map<QueryId, Boolean>,
): List<CredentialPresentmentSelection> {

    // Each member's matches become one or more picks, depending on the query's flag.
    fun picksForMember(
        member: CredentialPresentmentSetOptionMember,
    ): List<List<CredentialPresentmentSetOptionMemberMatch>> {
        val queryId = member.matches.firstOrNull()
            ?.let { (it.source as? CredentialMatchSourceOpenID4VP)?.credentialQuery?.id }
            ?.let(::QueryId)
        val allowsMultiple = queryId?.let { multipleByQueryId[it] } ?: false
        return if (allowsMultiple) {
            // One pick that carries every match together.
            listOf(member.matches)
        } else {
            // One pick per match.
            member.matches.map { listOf(it) }
        }
    }

    // For each option, combine its members' picks into one flat list per combination.
    fun picksForOption(
        option: CredentialPresentmentSetOption,
    ): List<List<CredentialPresentmentSetOptionMemberMatch>> {
        val perMember = option.members.map { picksForMember(it) }
        return cartesianProduct(perMember).map { perMemberPicks -> perMemberPicks.flatten() }
    }

    // For each set, gather picks from every option, plus an empty "skip" pick when
    // the set is optional.
    fun picksForSet(
        set: CredentialPresentmentSet,
    ): List<List<CredentialPresentmentSetOptionMemberMatch>> = buildList {
        if (set.optional) add(emptyList())
        set.options.forEach { addAll(picksForOption(it)) }
    }

    // Combine the per-set picks into the final list of options.
    val perSet = data.credentialSets.map { picksForSet(it) }
    return cartesianProduct(perSet).map { perSetPicks ->
        CredentialPresentmentSelection(matches = perSetPicks.flatten())
    }
}

/**
 * Combines lists of options into every possible combination:
 * `[[a, b], [c, d, e]] → [[a, c], [a, d], [a, e], [b, c], [b, d], [b, e]]`.
 *
 * Used internally by [buildMultipleAwareSelections]. An empty input gives back a single
 * empty combination (`[[]]`).
 */
private fun <T> cartesianProduct(lists: List<List<T>>): List<List<T>> {
    if (lists.isEmpty()) return listOf(emptyList())
    return lists.fold(listOf(emptyList())) { acc, list ->
        acc.flatMap { prefix -> list.map { item -> prefix + item } }
    }
}