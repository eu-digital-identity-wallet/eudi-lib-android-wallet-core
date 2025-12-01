/*
 * Copyright (c) 2024 European Commission
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

import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test

@RunWith(value = Parameterized::class)
class OpenId4VciManagerConfigBuilderParUsageTest(
    private val parUsage: Int,
) {

    @Test
    fun `ConfigBuilder set the parUsage property correctly`() {
        val builder = OpenId4VciManager.Config.Builder()
            .withIssuerUrl("https://issuer.example.com")
            .withClientAuthenticationType(OpenId4VciManager.ClientAuthenticationType.None("testClientId"))
            .withAuthFlowRedirectionURI("app://redirect")
            .withParUsage(parUsage)

        val config = builder.build()

        assertEquals(parUsage, config.parUsage)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: parUsage={0}")
        @JvmStatic
        fun parUsageArgs() = arrayListOf(
            OpenId4VciManager.Config.ParUsage.IF_SUPPORTED,
            OpenId4VciManager.Config.ParUsage.REQUIRED,
            OpenId4VciManager.Config.ParUsage.NEVER
        )
    }

}
