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
import eu.europa.ec.eudi.openid4vp.DispatchOutcome
import eu.europa.ec.eudi.openid4vp.EncryptionParameters
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.SiopOpenId4Vp
import eu.europa.ec.eudi.wallet.internal.toSiopOpenId4VPConfig
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpManager
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpResponse
import eu.europa.ec.eudi.wallet.transfer.openId4vp.RequestProcessorDispatcher
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
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
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OpenId4VpManagerTest {

    val testDispatcher = UnconfinedTestDispatcher()
    val requestProcessor = mockk<RequestProcessorDispatcher>(relaxed = true)
    val config = mockk<OpenId4VpConfig>(relaxed = true)
    val logger = object : Logger {
        override fun log(record: Logger.Record) {
            println(record)
        }

    }
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
    @Ignore(value = "Fails in CI")
    fun `test stop cancels resolveRequestUri coroutine`() = runTest(timeout = 1.minutes) {
        // Set up a latch to wait for the disconnected event
        val eventReceived = java.util.concurrent.CountDownLatch(1)

        every { config.schemes } returns listOf("http")

        // Setup a long-running coroutine that we can cancel
        coEvery { siopOpenId4Vp.resolveRequestUri(any()) } coAnswers {
            delay(Long.MAX_VALUE)
            mockk()
        }

        // Create a test listener that counts down the latch when Disconnected event is received
        val testListener = object : TransferEvent.Listener {
            override fun onTransferEvent(event: TransferEvent) {
                println("Event received in test: $event")
                if (event is TransferEvent.Disconnected) {
                    println("Disconnected event received, counting down latch")
                    eventReceived.countDown()
                }
            }
        }

        val manager = OpenId4VpManager(config, requestProcessor, logger)
        manager.addTransferEventListener(testListener)
        manager.addTransferEventListener(listener) // Keep original listener for verification

        // Start the long-running coroutine
        println("Starting resolveRequestUri")
        manager.resolveRequestUri("http://example.com")

        // Ensure the coroutine has started
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        // Now stop the manager, which should cancel the coroutine
        println("Stopping manager")
        manager.stop()

        // Advance time to ensure cancellation processing completes
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.runCurrent()

        // Wait for the event with timeout
        val received = eventReceived.await(3, java.util.concurrent.TimeUnit.SECONDS)

        // Assert that we received the event
        assertTrue(received, "Disconnected event not received within timeout")

        // Also verify with mockk for completeness
        verify(
            exactly = 1,
            timeout = 5000L
        ) { listener.onTransferEvent(ofType(TransferEvent.Disconnected::class)) }
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
            every { debugLog(logger,any()) } just Runs
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
        testDispatcher.scheduler.advanceUntilIdle()
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
                every { debugLog(logger,any()) } just Runs
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

    @Test
    fun `when dispatch outcome is RedirectURI, ResponseSent event should be emitted`() =
        runTest {
            // Setup
            val mockConsensus = mockk<Consensus.PositiveConsensus.VPTokenConsensus>()
            val mockResolvedRequestObject = mockk<ResolvedRequestObject.OpenId4VPAuthorization>()
            val encryptionParametersMock = mockk<EncryptionParameters>()
            val fakeResponse = mockk<OpenId4VpResponse.DeviceResponse> {
                every { consensus } returns mockConsensus
                every { resolvedRequestObject } returns mockResolvedRequestObject
                every { responseBytes } returns byteArrayOf()
                every { vpContent } returns mockk()
                every { encryptionParameters } returns encryptionParametersMock
                every { debugLog(logger,any()) } just Runs
            }

            // Mock dispatch to return RedirectURI outcome
            coEvery {
                siopOpenId4Vp.dispatch(
                    request = mockResolvedRequestObject,
                    consensus = mockConsensus,
                    encryptionParameters = encryptionParametersMock
                )
            } returns DispatchOutcome.RedirectURI(java.net.URI.create("https://example.com/redirect"))

            // Use verify approach instead of collecting events
            val manager = OpenId4VpManager(config, requestProcessor, logger)
            manager.addTransferEventListener(listener)

            // Execute
            manager.sendResponse(fakeResponse)

            // Verify that ResponseSent event was emitted
            verify(timeout = 1000) {
                listener.onTransferEvent(ofType(TransferEvent.ResponseSent::class))
            }
        }

    @Test
    fun `when dispatch outcome is VerifierResponse_Accepted with null redirectURI, ResponseSent event should be emitted`() =
        runTest {
            // Setup
            val mockConsensus = mockk<Consensus.PositiveConsensus.VPTokenConsensus>()
            val mockResolvedRequestObject = mockk<ResolvedRequestObject.OpenId4VPAuthorization>()
            val encryptionParametersMock = mockk<EncryptionParameters>()
            val fakeResponse = mockk<OpenId4VpResponse.DeviceResponse> {
                every { consensus } returns mockConsensus
                every { resolvedRequestObject } returns mockResolvedRequestObject
                every { responseBytes } returns byteArrayOf()
                every { vpContent } returns mockk()
                every { encryptionParameters } returns encryptionParametersMock
                every { debugLog(logger,any()) } just Runs
            }

            // Mock dispatch to return VerifierResponse.Accepted with null redirectURI
            coEvery {
                siopOpenId4Vp.dispatch(
                    request = mockResolvedRequestObject,
                    consensus = mockConsensus,
                    encryptionParameters = encryptionParametersMock
                )
            } returns DispatchOutcome.VerifierResponse.Accepted(null)

            // Use verify approach instead of collecting events
            val manager = OpenId4VpManager(config, requestProcessor, logger)
            manager.addTransferEventListener(listener)

            // Execute
            manager.sendResponse(fakeResponse)

            // Verify that ResponseSent event was emitted
            verify(timeout = 1000) {
                listener.onTransferEvent(ofType(TransferEvent.ResponseSent::class))
            }
        }

    @Test
    fun `when dispatch outcome is VerifierResponse_Accepted with redirectURI, Redirect event should be emitted`() =
        runTest {
            // Setup
            val mockConsensus = mockk<Consensus.PositiveConsensus.VPTokenConsensus>()
            val mockResolvedRequestObject = mockk<ResolvedRequestObject.OpenId4VPAuthorization>()
            val encryptionParametersMock = mockk<EncryptionParameters>()
            val redirectUri = java.net.URI.create("https://example.com/redirect")
            val fakeResponse = mockk<OpenId4VpResponse.DeviceResponse> {
                every { consensus } returns mockConsensus
                every { resolvedRequestObject } returns mockResolvedRequestObject
                every { responseBytes } returns byteArrayOf()
                every { vpContent } returns mockk()
                every { encryptionParameters } returns encryptionParametersMock
                every { debugLog(logger,any()) } just Runs
            }

            // Mock dispatch to return VerifierResponse.Accepted with a redirectURI
            coEvery {
                siopOpenId4Vp.dispatch(
                    request = mockResolvedRequestObject,
                    consensus = mockConsensus,
                    encryptionParameters = encryptionParametersMock
                )
            } returns DispatchOutcome.VerifierResponse.Accepted(redirectUri)

            // Use verify approach instead of collecting events
            val manager = OpenId4VpManager(config, requestProcessor, logger)
            manager.addTransferEventListener(listener)

            // Execute
            manager.sendResponse(fakeResponse)

            // Verify that Redirect event was emitted with the correct URI
            verify(timeout = 1000) {
                listener.onTransferEvent(match {
                    it is TransferEvent.Redirect && it.redirectUri == redirectUri
                })
            }
        }

    @Test
    fun `when dispatch outcome is VerifierResponse_Rejected, Error event should be emitted`() =
        runTest {
            // Setup
            val mockConsensus = mockk<Consensus.PositiveConsensus.VPTokenConsensus>()
            val mockResolvedRequestObject = mockk<ResolvedRequestObject.OpenId4VPAuthorization>()
            val encryptionParametersMock = mockk<EncryptionParameters>()
            val fakeResponse = mockk<OpenId4VpResponse.DeviceResponse> {
                every { consensus } returns mockConsensus
                every { resolvedRequestObject } returns mockResolvedRequestObject
                every { responseBytes } returns byteArrayOf()
                every { vpContent } returns mockk()
                every { encryptionParameters } returns encryptionParametersMock
                every { debugLog(logger,any()) } just Runs
            }

            // Mock dispatch to return VerifierResponse.Rejected
            coEvery {
                siopOpenId4Vp.dispatch(
                    request = mockResolvedRequestObject,
                    consensus = mockConsensus,
                    encryptionParameters = encryptionParametersMock
                )
            } returns DispatchOutcome.VerifierResponse.Rejected

            // Use verify approach instead of collecting events
            val manager = OpenId4VpManager(config, requestProcessor, logger)
            manager.addTransferEventListener(listener)

            // Execute
            manager.sendResponse(fakeResponse)

            // Verify that Error event was emitted with the correct exception type
            verify(timeout = 1000) {
                listener.onTransferEvent(match {
                    it is TransferEvent.Error &&
                            it.error is IllegalStateException &&
                            it.error.message == "Verifier rejected the response"
                })
            }
        }
}
