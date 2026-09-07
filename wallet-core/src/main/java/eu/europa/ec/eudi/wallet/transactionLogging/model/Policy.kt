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

package eu.europa.ec.eudi.wallet.transactionLogging.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

/**
 * A reference to a policy document (TS02 §2.8.6).
 *
 * @property type URI of the policy type (e.g. [PRIVACY_STATEMENT]).
 * @property policyURI URL where the policy document is published.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Policy(
    val type: String,
    val policyURI: String,
) {
    companion object {
        const val TRUST_SERVICE_PRACTICE_STATEMENT =
            "http://data.europa.eu/eudi/policy/trust-service-practice-statement"
        const val TERMS_AND_CONDITIONS = "http://data.europa.eu/eudi/policy/terms-and-conditions"
        const val PRIVACY_STATEMENT = "http://data.europa.eu/eudi/policy/privacy-statement"
        const val PRIVACY_POLICY = "http://data.europa.eu/eudi/policy/privacy-policy"
        const val REGISTRATION_POLICY = "http://data.europa.eu/eudi/policy/registration-policy"
    }
}
