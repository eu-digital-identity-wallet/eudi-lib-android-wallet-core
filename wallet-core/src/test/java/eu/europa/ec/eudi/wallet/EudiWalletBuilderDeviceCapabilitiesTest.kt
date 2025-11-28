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
import eu.europa.ec.eudi.wallet.logging.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@RunWith(value = Parameterized::class)
class EudiWalletBuilderDeviceCapabilitiesTest(
    private val configUserAuthenticationRequired: Boolean,
    private val configUseStrongBoxForKeys: Boolean,
    private val deviceSecureLockScreenSetup: Boolean,
    private val deviceStrongBoxSupported: Boolean,
    private val expectedUserAuthenticationRequired: Boolean,
    private val expectedUseStrongBoxForKeys: Boolean,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: config(ua={0},sb={1}), device(ua={2},sb={3}), expected(ua={4},sb={5})")
        fun data() = arrayListOf(
            arrayOf(true, true, false, false, false, false),
            arrayOf(true, true, true, false, true, false),
            arrayOf(true, true, false, true, false, true),
            arrayOf(true, true, true, true, true, true),
            arrayOf(false, false, false, false, false, false),
            arrayOf(false, false, true, false, false, false),
            arrayOf(false, false, false, true, false, false),
            arrayOf(false, false, true, true, false, false),
        )
    }

    @Test
    fun `EudiWalletConfig userAuthenticationRequired and useStrongBoxForKeys should be reverted to false if not supported by device`() {
        val config = EudiWalletConfig()
            .configureDocumentKeyCreation(
                userAuthenticationRequired = configUserAuthenticationRequired,
                userAuthenticationTimeout = 30000.milliseconds,
                useStrongBoxForKeys = configUseStrongBoxForKeys
            )

        val context: Context = mockk {
            every { applicationContext } returns this
        }
        val logger = Logger { record ->
            println(record.message)
        }

        val builder = spyk(
            EudiWallet.Builder(context, config, null)
                .withLogger(logger)
        ) {
            every { capabilities } returns mockk {
                every { secureLockScreenSetup } returns deviceSecureLockScreenSetup
                every { strongBoxSupported } returns deviceStrongBoxSupported
            }
        }

        builder.ensureUserAuthIsSupported(logger)
        builder.ensureStrongBoxIsSupported(logger)

        assertEquals(expectedUserAuthenticationRequired, config.userAuthenticationRequired)
        assertEquals(expectedUseStrongBoxForKeys, config.useStrongBoxForKeys)

    }
}