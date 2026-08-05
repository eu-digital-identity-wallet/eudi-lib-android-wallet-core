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
package eu.europa.ec.eudi.wallet.trust

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext

/**
 * Defines the policy for how the wallet should handle issuer trust verification results.
 *
 * A [TrustPolicy] determines the [Action] to take based on the type of credential
 * ([AttestationIdentifier]) being issued and the optional [VerificationContext].
 *
 * Use [uniform] for a single action regardless of input, or [build] for a fine-grained
 * policy with per-attestation and per-context overrides.
 */
fun interface TrustPolicy {

    /**
     * Resolves the trust action for a given attestation identifier and verification context.
     *
     * @param attestationIdentifier the type of credential being issued (e.g., MDoc or SDJwtVc)
     * @param verificationContext the optional verification context (e.g., PID, QEAA), or null if unknown
     * @return the [Action] indicating how the wallet should handle the trust verification result
     */
    fun resolve(
        attestationIdentifier: AttestationIdentifier,
        verificationContext: VerificationContext?,
    ): Action

    /**
     * Describes how the wallet should react to trust verification outcomes.
     */
    enum class Action {
        /**
         * Strict enforcement: if the issuer is not trusted, reject and delete the document
         * and emit a `DocumentFailed` event.
         */
        ENFORCE,

        /**
         * Informational only: always store the document regardless of trust result,
         * and attach the trust verification result to the `DocumentIssued` event.
         */
        INFORM
    }

    companion object {
        /**
         * Creates a policy that returns the same [action] for every input.
         *
         * @param action the action to return unconditionally
         * @return a [TrustPolicy] that always returns [action]
         */
        fun uniform(action: Action): TrustPolicy = TrustPolicy { _, _ -> action }

        /**
         * Creates a policy using the [Builder] DSL.
         *
         * Example:
         * ```
         * val policy = TrustPolicy.build {
         *     default(Action.ENFORCE)
         *     forContext(VerificationContext.PID, Action.INFORM)
         *     forDocType("org.iso.18013.5.1.mDL", Action.INFORM)
         * }
         * ```
         *
         * @param block configuration block applied to the [Builder]
         * @return a [TrustPolicy] configured according to the builder
         */
        fun build(block: Builder.() -> Unit): TrustPolicy = Builder().apply(block).build()
    }

    /**
     * DSL builder for constructing a [TrustPolicy] with layered override rules.
     *
     * Resolution order (highest priority first):
     * 1. Per-attestation overrides (added via [forAttestation], [forDocType], or [forVct])
     * 2. Per-context overrides (added via [forContext])
     * 3. Default action (set via [default], defaults to [Action.ENFORCE])
     */
    class Builder {
        private var default: Action = Action.ENFORCE
        private val byContext = mutableMapOf<VerificationContext, Action>()
        private val byAttestation = mutableMapOf<AttestationIdentifier, Action>()

        /**
         * Sets the default action when no specific override matches.
         *
         * @param action the fallback action (defaults to [Action.ENFORCE] if not called)
         * @return this builder for chaining
         */
        fun default(action: Action) = apply { this.default = action }

        /**
         * Adds an override for a specific [VerificationContext].
         *
         * @param context the verification context to match
         * @param action the action to return when the context matches
         * @return this builder for chaining
         */
        fun forContext(context: VerificationContext, action: Action) = apply { byContext[context] = action }

        /**
         * Adds an override for a specific [AttestationIdentifier].
         *
         * This takes the highest priority in resolution order.
         *
         * @param identifier the attestation identifier to match
         * @param action the action to return when the identifier matches
         * @return this builder for chaining
         */
        fun forAttestation(identifier: AttestationIdentifier, action: Action) = apply { byAttestation[identifier] = action }

        /**
         * Convenience method to add an override for an MDoc document type.
         *
         * Equivalent to `forAttestation(AttestationIdentifier.MDoc(docType), action)`.
         *
         * @param docType the MDoc document type string
         * @param action the action to return when the document type matches
         * @return this builder for chaining
         */
        fun forDocType(docType: String, action: Action) = apply { byAttestation[AttestationIdentifier.MDoc(docType)] = action }

        /**
         * Convenience method to add an override for an SD-JWT VC type.
         *
         * Equivalent to `forAttestation(AttestationIdentifier.SDJwtVc(vct), action)`.
         *
         * @param vct the SD-JWT Verifiable Credential Type
         * @param action the action to return when the VCT matches
         * @return this builder for chaining
         */
        fun forVct(vct: String, action: Action) = apply { byAttestation[AttestationIdentifier.SDJwtVc(vct)] = action }

        /**
         * Builds the [TrustPolicy] with the configured overrides.
         *
         * @return a [TrustPolicy] that resolves actions using the layered override rules
         */
        fun build(): TrustPolicy = TrustPolicy { attestationId, verificationContext ->
            byAttestation[attestationId]
                ?: verificationContext?.let { byContext[it] }
                ?: default
        }
    }
}
