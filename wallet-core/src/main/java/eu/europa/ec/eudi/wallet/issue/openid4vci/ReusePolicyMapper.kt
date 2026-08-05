/*
 * Copyright (c) 2024-2026 European Commission
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

import eu.europa.ec.eudi.openid4vci.CredentialReusePolicies
import eu.europa.ec.eudi.openid4vci.CredentialReusePolicy
import eu.europa.ec.eudi.openid4vci.EudiReusePolicy
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy

/**
 * The result of resolving an issuer's credential reuse policy against the wallet's
 * supported policies.
 *
 * @property credentialPolicy The [CredentialPolicy] that the document should be created with.
 *           The number of credentials (batch size) is embedded in the policy itself via
 *           [CredentialPolicy.numberOfCredentials].
 * @property selectedEudiReusePolicy The specific [EudiReusePolicy] option that was selected
 *           from the issuer's advertised options.
 */
data class ResolvedReusePolicy(
    val credentialPolicy: CredentialPolicy,
    val selectedEudiReusePolicy: EudiReusePolicy,
)

/**
 * Resolves the effective [CredentialPolicy] and number of credentials from the issuer's
 * [CredentialReusePolicy] and the wallet's supported policies.
 *
 * Per ETSI TS 119 472-3, the wallet must select the first supported option from the
 * issuer's prioritized list of reuse policy options.
 *
 * @param credentialReusePolicy The reuse policy from the issuer's credential metadata.
 * @param supportedPolicies The wallet's declared supported reuse policies, or null if
 *        the wallet has no preference.
 * @return A [ResolvedReusePolicy] if the issuer advertises a reuse policy and a supported
 *         option is found, or null if the issuer has no reuse policy ([CredentialReusePolicy.None]).
 * @throws IllegalStateException if the issuer advertises a reuse policy but none of its
 *         options are supported by this wallet.
 */
internal fun resolveReusePolicy(
    credentialReusePolicy: CredentialReusePolicy,
    supportedPolicies: CredentialReusePolicies?,
): ResolvedReusePolicy? {
    if (credentialReusePolicy is CredentialReusePolicy.None) return null

    val eudiPolicy = credentialReusePolicy as CredentialReusePolicy.EUDI

    // Select the first supported option (per spec: "order reflects issuer prioritization,
    // wallet must select the first supported option from the list").
    // PerRelyingParty is excluded because it is not yet supported by this wallet.
    val selected = eudiPolicy.options
        .filterNot { it is EudiReusePolicy.PerRelyingParty }
        .firstOrNull { it.isSupported(supportedPolicies) }
        ?: return null

    val numberOfCredentials = selected.batchSize ?: 1

    val policy = when (selected) {
        is EudiReusePolicy.OnceOnly -> CredentialPolicy.OnceOnly(
            numberOfCredentials = numberOfCredentials,
            reissueTriggerUnused = selected.reissueTriggerUnused,
        )
        is EudiReusePolicy.LimitedTime -> CredentialPolicy.LimitedTime(
            reissueTriggerLifetimeLeft = selected.reissueTriggerLifetimeLeft,
        )
        is EudiReusePolicy.RotatingBatch -> CredentialPolicy.RotatingBatch(
            numberOfCredentials = numberOfCredentials,
            reissueTriggerLifetimeLeft = selected.reissueTriggerLifetimeLeft,
        )
        is EudiReusePolicy.PerRelyingParty -> error("Unreachable: PerRelyingParty filtered above")
    }

    return ResolvedReusePolicy(
        credentialPolicy = policy,
        selectedEudiReusePolicy = selected,
    )
}
