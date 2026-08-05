/*
 * Copyright (c) 2024-2025 European Commission
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

import eu.europa.ec.eudi.openid4vci.CredentialResponseEncryptionPolicy
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import kotlin.test.Test
import kotlin.test.assertIs


class OpenId4VciManagerConfigBuilderTest {

    @Test
    fun `ConfigBuilder builds Config with valid clientAuthenticationType and authFlowRedirectionURI`() {
        val builder = OpenId4VciManager.Config.Builder()
            .withClientAuthenticationType(
                OpenId4VciManager.ClientAuthenticationType.AttestationBased("test-client-id")
            )
            .withAuthFlowRedirectionURI("app://redirect")

        val config = builder.build()

        assertNotNull(config)
    }

    @Test
    fun `ConfigBuilder throws exception when clientAuthenticationType is not set`() {
        val builder = OpenId4VciManager.Config.Builder()
            .withAuthFlowRedirectionURI("app://redirect")

        assertThrows(IllegalStateException::class.java) {
            builder.build()
        }
    }

    @Test
    fun `ConfigBuilder throws exception when authFlowRedirectionURI is not set`() {
        val builder = OpenId4VciManager.Config.Builder()
            .withClientAuthenticationType(
                OpenId4VciManager.ClientAuthenticationType.AttestationBased("test-client-id")
            )

        assertThrows(IllegalStateException::class.java) {
            builder.build()
        }
    }

    @Test
    fun `ConfigBuilder sets clientAuthenticationType correctly`() {
        val authType = OpenId4VciManager.ClientAuthenticationType.AttestationBased("test-client-id")
        val builder = OpenId4VciManager.Config.Builder()
            .withClientAuthenticationType(authType)
            .withAuthFlowRedirectionURI("app://redirect")

        val config = builder.build()

        assertEquals(authType, config.clientAuthenticationType)
        assertEquals("app://redirect", config.authFlowRedirectionURI)
        assertIs<DPopConfig.Default>(config.dpopConfig)
    }

    @Test
    fun `ConfigBuilder sets useDPoPIfSupported correctly`() {
        val builder = OpenId4VciManager.Config.Builder()
            .withClientAuthenticationType(
                OpenId4VciManager.ClientAuthenticationType.AttestationBased("test-client-id")
            )
            .withAuthFlowRedirectionURI("app://redirect")
            .withDPopConfig(DPopConfig.Disabled)

        val config = builder.build()

        assertIs<DPopConfig.Disabled>(config.dpopConfig)
    }

    @Test
    fun `ConfigBuilder uses REQUIRED as default responseEncryptionConfig policy`() {
        val builder = OpenId4VciManager.Config.Builder()
            .withClientAuthenticationType(
                OpenId4VciManager.ClientAuthenticationType.AttestationBased("test-client-id")
            )
            .withAuthFlowRedirectionURI("app://redirect")

        val config = builder.build()

        assertEquals(
            CredentialResponseEncryptionPolicy.REQUIRED,
            config.responseEncryptionConfig.credentialResponseEncryptionPolicy
        )
    }
}
