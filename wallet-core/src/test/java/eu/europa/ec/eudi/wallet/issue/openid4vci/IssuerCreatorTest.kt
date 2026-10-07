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
package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.nimbusds.oauth2.sdk.`as`.AuthorizationServerMetadata
import com.nimbusds.oauth2.sdk.id.Issuer
import eu.europa.ec.eudi.openid4vci.CertificateChainTrust
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertSame

class IssuerCreatorTest {

    private val config = OpenId4VciManager.Config(
        clientAuthenticationType = OpenId4VciManager.ClientAuthenticationType.None("client"),
        authFlowRedirectionURI = "app://redirect",
        dpopConfig = DPopConfig.Disabled,
    )

    /**
     * Wallet-initiated issuance and re-issuance fetch the issuer metadata again in the OpenID4VCI
     * library, so the library gets the configured policy also when issuer registration is disabled.
     */
    @Test
    fun `the configured metadata policy is passed to the library when issuer registration is disabled`() = runTest {
        val policy = IssuerMetadataPolicy.RequireSigned(issuerTrust = mockk<CertificateChainTrust>())
        val creator = IssuerCreator(
            context = mockk(relaxed = true),
            config = config,
            ktorHttpClientFactory = { mockk() },
            walletInstanceAttestationProvider = null,
            walletAttestationKeyManager = mockk(),
            logger = null,
            issuerMetadataPolicy = policy,
            issuerRegistrationEnabled = false,
        )

        val libraryConfig = with(creator) {
            config.toOpenId4VCIConfig(AuthorizationServerMetadata(Issuer("https://issuer.example")))
        }

        assertSame(policy, libraryConfig.issuerMetadataPolicy)
    }
}
