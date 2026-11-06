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
import eu.europa.ec.eudi.openid4vp.Client
import eu.europa.ec.eudi.openid4vp.OpenId4Vp
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.wallet.internal.makeOpenId4VPConfig
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql.DcqlRequestProcessor
import eu.europa.ec.eudi.wallet.trust.ecLeafSignedByIntermediateCertificate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests that [OpenId4VpManager] fails the request of an X.509 client for which no reader trust
 * verdict was recorded while the request was resolved.
 */
class OpenId4VpManagerReaderAuthenticationTest {

    private val requestProcessor = mockk<DcqlRequestProcessor>(relaxed = true)
    private val openId4Vp = mockk<OpenId4Vp.OverRedirects>(relaxed = true)
    private val events = LinkedBlockingQueue<TransferEvent>()

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        val uri = mockk<Uri> { every { scheme } returns "openid4vp" }
        every { Uri.parse(any()) } returns uri
        mockkStatic(::makeOpenId4VPConfig)
        every { makeOpenId4VPConfig(any(), any()) } returns mockk()
        mockkObject(OpenId4Vp)
        every { OpenId4Vp.overRedirects(any(), any()) } returns openId4Vp
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `a request of an x509 client without a verdict fails and nothing is dispatched to the verifier`() {
        // The library resolves the request of an X.509 client without calling the reader trust.
        val resolved = mockk<ResolvedRequestObject>(relaxed = true) {
            every { client } returns Client.X509SanDns("verifier.example", ecLeafSignedByIntermediateCertificate)
            every { responseMode } returns ResponseMode.DirectPostJwt(URL("https://verifier.example/response"))
            every { responseEncryptionSpecification } returns null
        }
        coEvery { openId4Vp.resolveRequestUri(any()) } returns Resolution.Success(resolved)
        // A rejection ends either with the log of no active request or with a response to the verifier.
        val rejectionHandled = CountDownLatch(1)
        val dispatched = AtomicBoolean(false)
        coEvery { openId4Vp.dispatch(any(), any(), any()) } answers {
            dispatched.set(true)
            rejectionHandled.countDown()
            mockk(relaxed = true)
        }
        val logger = Logger { record ->
            if (record.message == NO_ACTIVE_REQUEST) rejectionHandled.countDown()
        }
        val manager = OpenId4VpManager(
            config = OpenId4VpConfig.Builder()
                .withClientIdSchemes(ClientIdScheme.X509SanDns)
                .withSchemes("openid4vp")
                .withFormats(Format.MsoMdoc.ES256)
                .build(),
            requestProcessor = requestProcessor,
            logger = logger,
            listenersExecutor = Executor { it.run() },
        ).apply { addTransferEventListener { events.put(it) } }

        manager.resolveRequestUri("openid4vp://?request_uri=https://verifier.example/request")

        val error = assertIs<TransferEvent.Error>(events.poll(20, TimeUnit.SECONDS))
        assertIs<IllegalStateException>(error.error)
        coVerify(exactly = 0) { requestProcessor.process(any()) }
        // The request is not the active request, so a rejection is not dispatched to its verifier.
        manager.reject()
        assertTrue(rejectionHandled.await(20, TimeUnit.SECONDS), "Expected the rejection to be handled")
        assertFalse(dispatched.get(), "Expected no response to the verifier")
    }

    private companion object {
        const val NO_ACTIVE_REQUEST = "Attempted to reject, but no active request found."
    }
}
