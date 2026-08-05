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

import eu.europa.ec.eudi.openid4vp.dcql.DCQL
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import org.multipaz.openid.dcql.DcqlCredentialQuery
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.request.RequestedClaim

/**
 * Verifies that [selection] still satisfies the verifier's [dcql] request after the
 * user's consent-UI changes (e.g. deselecting a credential or individual claims).
 *
 * Two checks are applied:
 *
 *  - **Credential coverage.** When `credential_sets` is absent, every entry in
 *    `credentials` is required (OpenID4VP §6.4.2). When `credential_sets` is present,
 *    each required set must have at least one `options` combination whose every
 *    credential id appears in the selection. Optional sets may be skipped entirely.
 *  - **Claim completeness.** For each match, the disclosed claims must satisfy the
 *    source query — every claim in `claims` when no `claim_sets` are defined, or at
 *    least one full `claim_set` when alternatives exist (OpenID4VP §6.4.1).
 *
 * @return `null` when the selection is acceptable, or a descriptive error message
 *   listing every issue. The caller should reject the response and either re-prompt
 *   the user or return an `access_denied` error to the verifier.
 */
internal fun validateSelection(
    selection: CredentialPresentmentSelection,
    dcql: DCQL,
): String? {
    val errors = mutableListOf<String>()

    // Group the user's matches by the originating query.
    val matchesByQueryId: Map<QueryId, List<CredentialPresentmentSetOptionMemberMatch>> =
        selection.matches
            .mapNotNull { match ->
                val queryId = (match.source as? CredentialMatchSourceOpenID4VP)
                    ?.credentialQuery?.id
                    ?.let(::QueryId)
                queryId?.let { it to match }
            }
            .groupBy({ it.first }, { it.second })

    // ── 1. Credential coverage ────────────────────────────────────────────────
    val credentialSets = dcql.credentialSets?.value
    if (credentialSets == null) {
        // §6.4.2: with no credential_sets, every credentials entry is required.
        for (query in dcql.credentials.value) {
            if (matchesByQueryId[query.id].isNullOrEmpty()) {
                errors.add("Required credential '${query.id.value}' is missing from the selection")
            }
        }
    } else {
        // §6.4.2: each required set must have at least one fully-satisfied option.
        for ((index, set) in credentialSets.withIndex()) {
            if (!set.requiredOrDefault) continue
            val anyOptionSatisfied = set.options.any { option ->
                option.value.all { queryId ->
                    matchesByQueryId[queryId]?.isNotEmpty() == true
                }
            }
            if (!anyOptionSatisfied) {
                errors.add("Required credential_set #$index is not satisfied by the selection")
            }
        }
    }

    // ── 2. Per-match claim completeness ───────────────────────────────────────
    for ((queryId, matches) in matchesByQueryId) {
        for (match in matches) {
            val source = match.source as? CredentialMatchSourceOpenID4VP ?: continue
            if (!matchSatisfiesClaims(match, source.credentialQuery)) {
                errors.add(
                    "Selection for credential '${queryId.value}' does not disclose every " +
                        "claim required by the verifier",
                )
            }
        }
    }

    return if (errors.isEmpty()) null else errors.joinToString("; ")
}

/**
 * Returns `true` when [match]'s disclosed claims satisfy the source [query] per
 * OpenID4VP §6.4.1 — every claim when `claim_sets` is absent, otherwise at least one
 * full `claim_set`.
 */
private fun matchSatisfiesClaims(
    match: CredentialPresentmentSetOptionMemberMatch,
    query: DcqlCredentialQuery,
): Boolean {
    val disclosed: Set<RequestedClaim> = match.claims.keys

    if (query.claimSets.isEmpty()) {
        // No alternatives — every claim must be disclosed.
        return query.claims.all { it in disclosed }
    }

    // Reconstruct the id → claim lookup (the upstream type keeps it internal, so
    // [DcqlRequestProcessor] does the same).
    val idToClaim: Map<String, RequestedClaim> = query.claims
        .mapNotNull { rc -> rc.id?.let { id -> id to rc } }
        .toMap()

    return query.claimSets.any { claimSet ->
        claimSet.claimIdentifiers.all { id ->
            val claim = idToClaim[id] ?: return@any false
            claim in disclosed
        }
    }
}
