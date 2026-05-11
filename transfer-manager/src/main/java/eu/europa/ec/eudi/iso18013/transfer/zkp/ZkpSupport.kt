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

package eu.europa.ec.eudi.iso18013.transfer.zkp

import org.multipaz.mdoc.request.DocRequest
import org.multipaz.mdoc.zkp.ZkSystem
import org.multipaz.mdoc.zkp.ZkSystemRepository
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.RequestedClaim

/**
 * Data class representing a matched zero-knowledge proof system along with its specification.
 *
 * @property system the zero-knowledge proof system
 * @property spec the specification of the zero-knowledge proof system
 */
data class MatchedZkSystem(
    val system: ZkSystem,
    val spec: ZkSystemSpec
)

/**
 * Match a ZKP system against the verifier's request and the claims selected for disclosure.
 *
 * Returns null when [zkSystemRepository] is not configured, the verifier did not request ZKP,
 * or no compatible system spec was found for the disclosed claim set.
 *
 * @param zkSystemRepository the ZKP system repository, or null if not configured.
 * @param docRequest the verifier's [DocRequest] carrying any requested ZK system specs.
 * @param disclosedClaims the claims the user selected to disclose
 */
internal fun matchZkSystem(
    zkSystemRepository: ZkSystemRepository?,
    docRequest: DocRequest,
    disclosedClaims: Iterable<RequestedClaim>
): MatchedZkSystem? {
    val repo = zkSystemRepository ?: return null
    val specs = docRequest.docRequestInfo?.zkRequest?.systemSpecs ?: return null
    val mdocClaims = disclosedClaims.filterIsInstance<MdocRequestedClaim>()
    return findMatchingZkSystem(
        zkSystemRepository = repo,
        zkSystemSpecs = specs,
        requestedClaims = mdocClaims
    )
}

/**
 * Find the ZKP system that matches the requested claims against the verifier's
 * requested [zkSystemSpecs].
 *
 * @param zkSystemRepository the ZKP proof system repository
 * @param zkSystemSpecs the ZKP system specs requested by the verifier
 * @param requestedClaims the requested claims the proof will cover
 * @return the matched ZKP system and its specification, or null if none found
 */
private fun findMatchingZkSystem(
    zkSystemRepository: ZkSystemRepository,
    zkSystemSpecs: List<ZkSystemSpec>,
    requestedClaims: List<MdocRequestedClaim>,
): MatchedZkSystem? {
    if (zkSystemSpecs.isEmpty()) return null

    return zkSystemSpecs
        .asSequence()
        .mapNotNull { zkSpec ->
            val system = zkSystemRepository.lookup(zkSpec.system) ?: return@mapNotNull null
            val spec = system.getMatchingSystemSpec(
                zkSystemSpecs = zkSystemSpecs,
                requestedClaims = requestedClaims
            ) ?: return@mapNotNull null
            MatchedZkSystem(system, spec)
        }.firstOrNull()
}