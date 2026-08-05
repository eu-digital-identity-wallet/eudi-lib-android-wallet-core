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

package eu.europa.ec.eudi.wallet.document.internal

import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import org.multipaz.credential.Credential
import org.multipaz.document.Document

/**
 * Internal interface for implementing credential policy application.
 * This interface encapsulates the logic for applying different credential policies.
 */
internal fun interface CredentialPolicyApplier {
    /**
     * Applies the credential policy after a credential is used.
     *
     * @param document The document containing the credential
     * @param credential The credential that has been used
     */
    suspend fun apply(document: Document, credential: Credential)

    companion object {
        /**
         * Creates a [CredentialPolicyApplier] for the specified [CreateDocumentSettings.CredentialPolicy].
         *
         * @param policy The credential policy
         * @return A policy applier that implements the specified policy
         */
        fun from(policy: CreateDocumentSettings.CredentialPolicy): CredentialPolicyApplier {
            return when (policy) {
                is CreateDocumentSettings.CredentialPolicy.OnceOnly -> OneTimeUsePolicyApplier
                is CreateDocumentSettings.CredentialPolicy.LimitedTime -> RotateUsePolicyApplier
                is CreateDocumentSettings.CredentialPolicy.RotatingBatch -> RotateUsePolicyApplier
            }
        }
    }
}

/**
 * Implementation of [CredentialPolicyApplier] for one-time use policy.
 * Deletes the credential after it's used.
 */
internal object OneTimeUsePolicyApplier : CredentialPolicyApplier {
    override suspend fun apply(document: Document, credential: Credential) {
        document.deleteCredential(credential.identifier)
    }
}

/**
 * Implementation of [CredentialPolicyApplier] for rotate use policy.
 * Increases the usage count of the credential after it's used.
 */
internal object RotateUsePolicyApplier : CredentialPolicyApplier {
    override suspend fun apply(document: Document, credential: Credential) {
        credential.increaseUsageCount()
    }
}