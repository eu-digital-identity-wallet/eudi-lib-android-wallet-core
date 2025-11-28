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

import android.content.Context
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.provider.WalletKeyManager
import io.mockk.mockk
import org.junit.Assert.assertThrows
import kotlin.test.Test
import kotlin.test.assertNotNull

class OpenId4VciManagerBuilderTest {

    private val context = mockk<Context>(relaxed = true)
    private val documentManager = mockk<DocumentManager>(relaxed = true)
    private val walletKeyManager = mockk<WalletKeyManager>(relaxed = true)
    private val config = OpenId4VciManager.Config(
        issuerUrl = "https://issuer.example.com",
        clientId = "testClientId",
        authFlowRedirectionURI = "app://redirect",
        dPoPUsage = OpenId4VciManager.Config.DPoPUsage.IfSupported(),
        parUsage = OpenId4VciManager.Config.ParUsage.IF_SUPPORTED,
    )

    @Test
    fun `Builder builds OpenId4VciManager with valid config and documentManager`() {
        val builder = OpenId4VciManager.Builder(context)
            .config(config)
            .documentManager(documentManager)
            .walletKeyManager(walletKeyManager)

        val manager = builder.build()

        assertNotNull(manager)
    }

    @Test
    fun `Builder throws exception when config is not set`() {
        val builder = OpenId4VciManager.Builder(context)
            .documentManager(documentManager)

        assertThrows(IllegalStateException::class.java) {
            builder.build()
        }
    }

    @Test
    fun `Builder throws exception when documentManager is not set`() {
        val builder = OpenId4VciManager.Builder(context)
            .config(config)

        assertThrows(IllegalStateException::class.java) {
            builder.build()
        }
    }
}