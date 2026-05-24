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

import com.nimbusds.jose.jwk.Curve
import eu.europa.ec.eudi.openid4vci.CredentialResponseEncryptionPolicy
import eu.europa.ec.eudi.openid4vci.EcConfig
import eu.europa.ec.eudi.openid4vci.EncryptionSupportConfig
import eu.europa.ec.eudi.openid4vci.RsaConfig
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test

@RunWith(value = Parameterized::class)
class OpenId4VciManagerConfigBuilderResponseEncryptionTest(
    private val responseEncryptionConfig: EncryptionSupportConfig,
) {

    @Test
    fun `ConfigBuilder sets the responseEncryptionConfig property correctly`() {
        val builder = OpenId4VciManager.Config.Builder()
            .withIssuerUrl("https://issuer.example.com")
            .withClientAuthenticationType(OpenId4VciManager.ClientAuthenticationType.None("testClientId"))
            .withAuthFlowRedirectionURI("app://redirect")
            .withResponseEncryptionConfig(responseEncryptionConfig)

        val config = builder.build()

        assertEquals(responseEncryptionConfig, config.responseEncryptionConfig)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: responseEncryptionConfig={0}")
        @JvmStatic
        fun responseEncryptionArgs() = arrayListOf(
            EncryptionSupportConfig(
                credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.REQUIRED,
                ecConfig = EcConfig(ecKeyCurve = Curve.P_256),
                rsaConfig = RsaConfig(rcaKeySize = 2048),
            ),
            EncryptionSupportConfig(
                credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED,
                ecConfig = EcConfig(ecKeyCurve = Curve.P_256),
                rsaConfig = RsaConfig(rcaKeySize = 2048),
            ),
            EncryptionSupportConfig(
                credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.REQUIRED,
                ecConfig = EcConfig(ecKeyCurve = Curve.P_384),
                rsaConfig = RsaConfig(rcaKeySize = 4096),
            ),
        )
    }
}
