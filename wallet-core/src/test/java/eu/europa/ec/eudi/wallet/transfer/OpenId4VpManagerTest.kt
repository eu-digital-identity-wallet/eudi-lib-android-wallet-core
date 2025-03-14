/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.transfer

import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.EncryptionParameters
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.SiopOpenId4Vp
import eu.europa.ec.eudi.wallet.internal.toSiopOpenId4VPConfig
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpManager
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequestProcessor
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OpenId4VpManagerTest {

    val testDispatcher = UnconfinedTestDispatcher()
    val requestProcessor = mockk<OpenId4VpRequestProcessor>(relaxed = true)
    val config = mockk<OpenId4VpConfig>(relaxed = true)
    val logger = Logger { record -> println(record) }
    lateinit var siopOpenId4Vp: SiopOpenId4Vp

    val listener = spyk(object : TransferEvent.Listener {
        override fun onTransferEvent(event: TransferEvent) {
            println(event)
        }
    })

    @Before
    fun beforeTests() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(SiopOpenId4Vp)
        siopOpenId4Vp = mockk<SiopOpenId4Vp>(relaxed = true)
        every { SiopOpenId4Vp(any(), any()) } returns siopOpenId4Vp

        mockkStatic(OpenId4VpConfig::toSiopOpenId4VPConfig)
        with(config) {
            every { toSiopOpenId4VPConfig(any()) } returns mockk()
        }
    }

    @After
    fun afterTests() {
        Dispatchers.resetMain()
        testDispatcher.cancelChildren()
        unmockkAll()
    }

    @Test
    fun `test stop cancels resolveRequestUri coroutine`() = runTest(timeout = 1.minutes) {

        every { config.schemes } returns listOf("http")

        coEvery { siopOpenId4Vp.resolveRequestUri(any()) } coAnswers {
            delay(Long.MAX_VALUE)
            mockk()
        }

        val manager = OpenId4VpManager(config, requestProcessor, logger)

        manager.addTransferEventListener(listener)

        manager.resolveRequestUri("http://example.com")
        delay(100)
        manager.stop()

        verify(
            exactly = 1,
            timeout = 1000L
        ) { listener.onTransferEvent(any<TransferEvent.Disconnected>()) }

    }

    @Test
    fun `test stop cancels sendResponse coroutine`() = runTest(timeout = 1.minutes) {

        val mockConsensus = mockk<Consensus.PositiveConsensus.VPTokenConsensus>()
        val mockResolvedRequestObject = mockk<ResolvedRequestObject>()
        val encryptionParametersMock = mockk<EncryptionParameters>()
        val fakeResponse = mockk<OpenId4VpResponse.DeviceResponse> {
            every { consensus } returns mockConsensus
            every { resolvedRequestObject } returns mockResolvedRequestObject
            every { responseBytes } returns byteArrayOf()
            every { vpContent } returns mockk()
            every { encryptionParameters } returns encryptionParametersMock
        }
        coEvery {
            siopOpenId4Vp.dispatch(
                request = mockResolvedRequestObject,
                consensus = mockConsensus,
                encryptionParameters = encryptionParametersMock
            )
        } coAnswers {
            delay(Long.MAX_VALUE)
            mockk()
        }

        val manager = OpenId4VpManager(config, requestProcessor, logger)

        manager.addTransferEventListener(listener)

        manager.sendResponse(fakeResponse)
        delay(100)
        manager.stop()

        verify(
            exactly = 1,
            timeout = 1000L
        ) { listener.onTransferEvent(any<TransferEvent.Disconnected>()) }
    }

    @Test
    fun `when sendResponse throws inside coroutine exception is passed as event`() =
        runTest(timeout = 1.minutes) {

            val mockConsensus = mockk<Consensus.PositiveConsensus.VPTokenConsensus>()
            val mockResolvedRequestObject = mockk<ResolvedRequestObject>()
            val encryptionParametersMock = mockk<EncryptionParameters>()
            val fakeResponse = mockk<OpenId4VpResponse.DeviceResponse> {
                every { consensus } returns mockConsensus
                every { resolvedRequestObject } returns mockResolvedRequestObject
                every { responseBytes } returns byteArrayOf()
                every { vpContent } returns mockk()
                every { encryptionParameters } returns encryptionParametersMock
            }
            val exception = Exception("test")
            coEvery {
                siopOpenId4Vp.dispatch(
                    request = mockResolvedRequestObject,
                    consensus = mockConsensus,
                    encryptionParameters = encryptionParametersMock
                )
            } throws exception


            val manager = OpenId4VpManager(config, requestProcessor, logger)

            manager.addTransferEventListener(listener)
            manager.sendResponse(fakeResponse)

            verify(
                exactly = 1,
                timeout = 1000L
            ) { listener.onTransferEvent(match { it is TransferEvent.Error && it.error == exception }) }


        }
}
