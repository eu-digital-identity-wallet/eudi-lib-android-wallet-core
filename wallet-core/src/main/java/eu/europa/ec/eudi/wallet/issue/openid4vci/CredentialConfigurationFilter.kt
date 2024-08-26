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
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import io.ktor.util.reflect.instanceOf
import kotlin.reflect.KClass

/**
 * Filter for [CredentialConfiguration] instances.
 */
internal fun interface CredentialConfigurationFilter {
    operator fun invoke(conf: CredentialConfiguration): Boolean = filter(conf)
    fun filter(conf: CredentialConfiguration): Boolean

    /**
     * Companion object for [CredentialConfigurationFilter] instances.
     * @property MsoMdocFormatFilter filter for [CredentialConfiguration] instances for mso_mdoc format
     */
    companion object {

        /**
         * Filter for [CredentialConfiguration] instances based on the credential format.
         * @param format credential format
         * @return [CredentialConfigurationFilter] instance
         */
        @JvmSynthetic
        internal fun FormatFilter(format: KClass<out CredentialConfiguration>) =
            CredentialConfigurationFilter { conf ->
                format.isInstance(conf)
            }

        /**
         * Filter for [CredentialConfiguration] instances for mso_mdoc format
         */
        @JvmSynthetic
        internal val MsoMdocFormatFilter: CredentialConfigurationFilter =
            FormatFilter(MsoMdocCredential::class)

        internal val SdJwtOrMsoMdocFormatFilter: CredentialConfigurationFilter =
            CredentialConfigurationFilter {
                it.instanceOf(SdJwtVcCredential::class) || it.instanceOf(MsoMdocCredential::class)
            }

        /**
         * Filter for [CredentialConfiguration] instances based on the document type.
         * @param docType document type
         * @return [CredentialConfigurationFilter] instance
         */
        @JvmSynthetic
        internal fun DocTypeFilter(docType: String): CredentialConfigurationFilter =
            Compose(
                SdJwtOrMsoMdocFormatFilter,
                CredentialConfigurationFilter { conf -> conf.docType == docType })

        /**
         * Filter for [CredentialConfiguration] instances based on the proof type.
         * @param supportedProofTypes supported proof types
         * @return [CredentialConfigurationFilter] instance
         */
        @JvmSynthetic
        internal fun ProofTypeFilter(vararg supportedProofTypes: OpenId4VciManager.Config.ProofType): CredentialConfigurationFilter =
            ProofTypeFilter(supportedProofTypes.toList())

        /**
         * Filter for [CredentialConfiguration] instances based on the proof type.
         * @param supportedProofTypes supported proof types
         * @return [CredentialConfigurationFilter] instance
         */
        @JvmSynthetic
        internal fun ProofTypeFilter(supportedProofTypes: List<OpenId4VciManager.Config.ProofType>): CredentialConfigurationFilter =
            CredentialConfigurationFilter { conf ->
                try {
                    SupportedProofType.withPriority(supportedProofTypes).select(conf)
                    true
                } catch (e: Throwable) {
                    false
                }
            }

        /**
         * Compose multiple [CredentialConfigurationFilter] instances.
         * @param filters [CredentialConfigurationFilter] instances
         * @return [CredentialConfigurationFilter] instance
         */
        @JvmSynthetic
        internal fun Compose(vararg filters: CredentialConfigurationFilter): CredentialConfigurationFilter =
            CredentialConfigurationFilter { conf -> filters.all { it(conf) } }

    }
}