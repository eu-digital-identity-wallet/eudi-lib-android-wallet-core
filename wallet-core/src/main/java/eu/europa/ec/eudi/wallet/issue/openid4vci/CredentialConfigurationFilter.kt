/*
 *  Copyright (c) 2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import kotlin.reflect.KClass

internal fun interface CredentialConfigurationFilter {
    operator fun invoke(conf: CredentialConfiguration): Boolean = filter(conf)
    fun filter(conf: CredentialConfiguration): Boolean

    /**
     * Default [CredentialConfigurationFilter] implementations for credential format and proof type.
     * @property MsoMdocFormatFilter filter for credential format
     * @property ProofTypeFilter filter for proof type
     */
    companion object {
        @JvmSynthetic
        internal fun FormatFilterFactory(format: KClass<out CredentialConfiguration>) =
            CredentialConfigurationFilter { conf ->
                format.isInstance(conf)
            }

        @JvmSynthetic
        internal val MsoMdocFormatFilter: CredentialConfigurationFilter =
            FormatFilterFactory(MsoMdocCredential::class)

        @JvmSynthetic
        internal fun DocTypeFilter(docType: String): CredentialConfigurationFilter =
            Compose(MsoMdocFormatFilter, CredentialConfigurationFilter { conf -> conf.docType == docType })

        @JvmSynthetic
        internal fun ProofTypeFilter(vararg supportedProofTypes: OpenId4VciManager.Config.ProofType): CredentialConfigurationFilter =
            ProofTypeFilter(supportedProofTypes.toList())

        @JvmSynthetic
        internal fun ProofTypeFilter(supportedProofTypes: List<OpenId4VciManager.Config.ProofType>): CredentialConfigurationFilter =
            CredentialConfigurationFilter { conf ->
                try {
                    SupportedProofType.prioritize(supportedProofTypes).select(conf)
                    true
                } catch (e: Throwable) {
                    false
                }
            }

        /**
         * Composed(
         *    MsoMdocFormatFilter,
         *    ProofTypeFilter(supportedProofTypes)
         *    DocTypeFilterFactory(docType)
         * )(conf)
         */
        @JvmSynthetic
        internal fun Compose(vararg filters: CredentialConfigurationFilter): CredentialConfigurationFilter =
            CredentialConfigurationFilter { conf -> filters.all { it(conf) } }

    }
}