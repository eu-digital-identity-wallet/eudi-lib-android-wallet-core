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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import android.net.Uri
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.openid4vp.OpenId4Vp
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.wallet.internal.makeOpenId4VPConfig
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig.EncryptionPolicy
import eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql.DcqlRequestProcessor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import java.net.URL
import kotlin.test.Test

/**
 * Integration tests for [OpenId4VpConfig.EncryptionPolicy] wired into [OpenId4VpManager].
 * When the policy rejects, the manager emits [TransferEvent.Error] and skips the
 * processor. When it accepts, the normal `RequestReceived` flow continues.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OpenId4VpManagerEncryptionPolicyTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val requestProcessor = mockk<DcqlRequestProcessor>(relaxed = true)
    private val logger = Logger { record -> println(record) }

    @Before
    fun beforeTests() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(OpenId4Vp)
        mockkStatic(::makeOpenId4VPConfig)
        every { makeOpenId4VPConfig(any(), any()) } returns mockk()
    }

    /**
     * HAIP rejects an unencrypted `direct_post` request — the manager emits Error and
     * does not call the processor or emit `RequestReceived`.
     */
    @Test
    fun `HAIP rejects DirectPost — Error emitted, processor never called`() {
        withMockedUri("haip-vp") {
            val config = buildConfig(EncryptionPolicy.HAIP)
            val mockOpenId4Vp = mockk<OpenId4Vp.OverRedirects>(relaxed = true)
            every { OpenId4Vp.overRedirects(any(), any()) } returns mockOpenId4Vp

            val resolved = mockk<ResolvedRequestObject>(relaxed = true) {
                every { responseMode } returns
                    ResponseMode.DirectPost(URL("https://verifier.example/cb"))
                every { responseEncryptionSpecification } returns null
            }
            coEvery { mockOpenId4Vp.resolveRequestUri(any()) } returns Resolution.Success(resolved)

            val manager = OpenId4VpManager(
                config = config,
                requestProcessor = requestProcessor,
                logger = logger,
            )
            val listener = spyk(object : TransferEvent.Listener {
                override fun onTransferEvent(event: TransferEvent) {
                    println("TEST: $event")
                }
            })
            manager.addTransferEventListener(listener)

            manager.resolveRequestUri("haip-vp://example-request-uri")

            verify(timeout = 2000) {
                listener.onTransferEvent(ofType(TransferEvent.Error::class))
            }
            coVerify(exactly = 0) { requestProcessor.process(any()) }
            verify(exactly = 0) {
                listener.onTransferEvent(ofType(TransferEvent.RequestReceived::class))
            }
        }
    }

    /**
     * HAIP accepts an encrypted `direct_post.jwt` request — the manager calls the
     * processor and emits `RequestReceived`.
     */
    @Test
    fun `HAIP accepts DirectPostJwt — processor invoked and RequestReceived emitted`() {
        withMockedUri("haip-vp") {
            val config = buildConfig(EncryptionPolicy.HAIP)
            val mockOpenId4Vp = mockk<OpenId4Vp.OverRedirects>(relaxed = true)
            every { OpenId4Vp.overRedirects(any(), any()) } returns mockOpenId4Vp

            val resolved = mockk<ResolvedRequestObject>(relaxed = true) {
                every { responseMode } returns
                    ResponseMode.DirectPostJwt(URL("https://verifier.example/cb"))
                every { responseEncryptionSpecification } returns null
            }
            coEvery { mockOpenId4Vp.resolveRequestUri(any()) } returns Resolution.Success(resolved)

            val processed = mockk<RequestProcessor.ProcessedRequest>(relaxed = true)
            coEvery { requestProcessor.process(any()) } returns processed

            val manager = OpenId4VpManager(
                config = config,
                requestProcessor = requestProcessor,
                logger = logger,
            )
            val listener = spyk(object : TransferEvent.Listener {
                override fun onTransferEvent(event: TransferEvent) {
                    println("TEST: $event")
                }
            })
            manager.addTransferEventListener(listener)

            manager.resolveRequestUri("haip-vp://example-request-uri")

            // Wait for RequestReceived before asserting on `process(...)` to avoid
            // racing the IO coroutine.
            verify(timeout = 2000) {
                listener.onTransferEvent(ofType(TransferEvent.RequestReceived::class))
            }
            coVerify(exactly = 1) { requestProcessor.process(any()) }
            verify(exactly = 0) {
                listener.onTransferEvent(ofType(TransferEvent.Error::class))
            }
        }
    }

    /**
     * A custom policy passed via `withEncryptionPolicy(...)` is the one the manager
     * actually invokes — proves the override reaches `enforce`, not just the config.
     */
    @Test
    fun `custom policy is the one actually invoked by the manager`() {
        withMockedUri("haip-vp") {
            var seenMode: ResponseMode? = null
            val custom = EncryptionPolicy { mode -> seenMode = mode }
            val config = buildConfig(custom)

            val mockOpenId4Vp = mockk<OpenId4Vp.OverRedirects>(relaxed = true)
            every { OpenId4Vp.overRedirects(any(), any()) } returns mockOpenId4Vp

            val mode = ResponseMode.DirectPost(URL("https://verifier.example/cb"))
            val resolved = mockk<ResolvedRequestObject>(relaxed = true) {
                every { responseMode } returns mode
                every { responseEncryptionSpecification } returns null
            }
            coEvery { mockOpenId4Vp.resolveRequestUri(any()) } returns Resolution.Success(resolved)
            coEvery { requestProcessor.process(any()) } returns
                mockk<RequestProcessor.ProcessedRequest>(relaxed = true)

            val manager = OpenId4VpManager(
                config = config,
                requestProcessor = requestProcessor,
                logger = logger,
            )
            val listener = spyk(object : TransferEvent.Listener {
                override fun onTransferEvent(event: TransferEvent) {
                    println("TEST: $event")
                }
            })
            manager.addTransferEventListener(listener)

            manager.resolveRequestUri("haip-vp://example-request-uri")

            // Wait for any event so we know the coroutine has run past the policy check.
            verify(timeout = 2000) { listener.onTransferEvent(any()) }

            kotlin.test.assertEquals(mode, seenMode, "custom policy must be invoked")
        }
    }

    /** Build a real config with the given policy — no mocks on the config itself. */
    private fun buildConfig(policy: EncryptionPolicy): OpenId4VpConfig =
        OpenId4VpConfig.Builder()
            .withClientIdSchemes(ClientIdScheme.X509SanDns)
            .withSchemes("haip-vp")
            .withFormats(Format.MsoMdoc.ES256, Format.SdJwtVc.ES256)
            .withEncryptionPolicy(policy)
            .build()

    /** Runs [block] with `Uri.parse` mocked so the manager's scheme check succeeds. */
    private inline fun withMockedUri(scheme: String, block: () -> Unit) {
        mockkStatic(Uri::class)
        val mockUri = mockk<Uri>()
        every { Uri.parse(any()) } returns mockUri
        every { mockUri.scheme } returns scheme
        try {
            block()
        } finally {
            unmockkStatic(Uri::class)
        }
    }
}
