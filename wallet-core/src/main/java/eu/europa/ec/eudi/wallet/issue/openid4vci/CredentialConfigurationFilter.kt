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
    operator fun invoke(conf: CredentialConfiguration): Boolean

    /**
     *
     */
    class DocTypeFilterFactory(private val docType: String) : CredentialConfigurationFilter {
        override fun invoke(conf: CredentialConfiguration): Boolean {
            return conf is MsoMdocCredential && conf.docType == docType
        }
    }

    class FormatFilterFactory<in F : CredentialConfiguration>(private val format: KClass<F>) :
        CredentialConfigurationFilter {
        override fun invoke(conf: CredentialConfiguration): Boolean {
            return format.isInstance(conf)
        }
    }

    /**
     * Default [CredentialConfigurationFilter] implementations for credential format and proof type.
     * @property MsoMdocFormatFilter filter for credential format
     * @property ProofTypeFilter filter for proof type
     */
    companion object {
        @JvmSynthetic
        internal val MsoMdocFormatFilter: CredentialConfigurationFilter =
            FormatFilterFactory(MsoMdocCredential::class)

        @JvmSynthetic
        internal val ProofTypeFilter: CredentialConfigurationFilter = CredentialConfigurationFilter { conf ->
            try {
                SupportedProofType.selectProofType(conf)
                true
            } catch (e: Throwable) {
                false
            }
        }


    }
}