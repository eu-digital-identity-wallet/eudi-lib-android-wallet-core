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

package eu.europa.ec.eudi.wallet

import android.content.Context
import eu.europa.ec.eudi.wallet.document.KeyProtectionPolicy
import eu.europa.ec.eudi.wallet.document.KeyProtectionUnsupportedException
import eu.europa.ec.eudi.wallet.document.KeyProtectionUnsupportedException.Protection
import eu.europa.ec.eudi.wallet.logging.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(value = Parameterized::class)
class EudiWalletBuilderDeviceCapabilitiesTest(
    private val policy: KeyProtectionPolicy,
    private val deviceSupported: Boolean,
    private val expected: Boolean?,
) {

    companion object {
        // expected null: building the wallet fails
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: policy={0}, device={1}, expected={2}")
        fun data() = arrayListOf(
            arrayOf<Any?>(KeyProtectionPolicy.Required, true, true),
            arrayOf<Any?>(KeyProtectionPolicy.Required, false, null),
            arrayOf<Any?>(KeyProtectionPolicy.IfSupported, true, true),
            arrayOf<Any?>(KeyProtectionPolicy.IfSupported, false, false),
            arrayOf<Any?>(KeyProtectionPolicy.Disabled, true, false),
            arrayOf<Any?>(KeyProtectionPolicy.Disabled, false, false),
        )
    }

    private val logger = Logger { record ->
        println(record.message)
    }

    @Test
    fun `user authentication is resolved from the policy and the device support`() {
        val config = EudiWalletConfig()
            .configureDocumentKeyCreation(userAuthentication = policy, strongBox = KeyProtectionPolicy.Disabled)
        val builder = builder(config, secureLockScreenSetup = deviceSupported, strongBoxSupported = true)

        assertResolved(Protection.UserAuthentication) { builder.resolveUserAuthentication(logger) }
    }

    @Test
    fun `StrongBox is resolved from the policy and the device support`() {
        val config = EudiWalletConfig()
            .configureDocumentKeyCreation(userAuthentication = KeyProtectionPolicy.Disabled, strongBox = policy)
        val builder = builder(config, secureLockScreenSetup = true, strongBoxSupported = deviceSupported)

        assertResolved(Protection.StrongBox) { builder.resolveStrongBox(logger) }
    }

    private fun assertResolved(protection: Protection, resolve: () -> Boolean) {
        when (expected) {
            null -> assertEquals(protection, assertFailsWith<KeyProtectionUnsupportedException> { resolve() }.protection)
            else -> assertEquals(expected, resolve())
        }
    }

    private fun builder(
        config: EudiWalletConfig,
        secureLockScreenSetup: Boolean,
        strongBoxSupported: Boolean,
    ): EudiWallet.Builder {
        val context: Context = mockk {
            every { applicationContext } returns this
        }
        return spyk(EudiWallet.Builder(context, config, null).withLogger(logger)) {
            every { capabilities } returns mockk {
                every { this@mockk.secureLockScreenSetup } returns secureLockScreenSetup
                every { this@mockk.strongBoxSupported } returns strongBoxSupported
            }
        }
    }
}
