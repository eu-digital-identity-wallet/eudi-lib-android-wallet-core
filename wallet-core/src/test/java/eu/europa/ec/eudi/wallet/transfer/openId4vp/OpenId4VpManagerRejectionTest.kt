package eu.europa.ec.eudi.wallet.transfer.openId4vp

import android.net.Uri
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.DispatchOutcome
import eu.europa.ec.eudi.openid4vp.OpenId4Vp
import kotlin.test.Test
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.wallet.internal.makeOpenId4VPConfig
import eu.europa.ec.eudi.wallet.logging.Logger
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
class OpenId4VpManagerRejectionTest {

    val testDispatcher = UnconfinedTestDispatcher()
    val requestProcessor = mockk<DcqlRequestProcessor>(relaxed = true)
    val config = mockk<OpenId4VpConfig>(relaxed = true)
    val logger = object : Logger {
        override fun log(record: Logger.Record) {
            println(record)
        }

    }
    
    @Before
    fun beforeTests() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(OpenId4Vp)

        mockkStatic(::makeOpenId4VPConfig)
        every { makeOpenId4VPConfig(any(), any()) } returns mockk()
    }

    @Test
    fun `when user rejects transaction, NegativeConsensus is dispatched and ResponseSent event is emitted`()  {
        mockkStatic(Uri::class)
        val mockUri = mockk<Uri>()
        every { Uri.parse("openid4vp://example-request-uri") } returns mockUri
        every { mockUri.scheme } returns "openid4vp"

        val config = mockk<OpenId4VpConfig>(relaxed = true)

        val mockOpenId4Vp = mockk<OpenId4Vp>(relaxed = true)


        every { OpenId4Vp(any(), any()) } returns mockOpenId4Vp

        try {
            val mockResponseMode = ResponseMode.DirectPost(URL("https://placeholder.com"))
            val mockResolvedRequest = mockk<ResolvedRequestObject>(relaxed = true) {
                every { responseMode } returns mockResponseMode
                every { responseEncryptionSpecification } returns null
            }

            // Stub Resolution
            coEvery { mockOpenId4Vp.resolveRequestUri(any()) } returns Resolution.Success(mockResolvedRequest)

            // Stub Dispatch (The core verification target)
            coEvery {
                mockOpenId4Vp.dispatch(
                    request = mockResolvedRequest,
                    consensus = match { it is Consensus.NegativeConsensus },
                    encryptionParameters = null
                )
            } returns DispatchOutcome.VerifierResponse.Accepted(null)

            // Stub Config
            every { config.schemes } returns listOf("openid4vp")

            // Stub Processor
            val mockProcessedRequest = mockk<RequestProcessor.ProcessedRequest>(relaxed = true)
            coEvery { requestProcessor.process(any()) } returns mockProcessedRequest

            val manager = OpenId4VpManager(
                config = config,
                requestProcessor = requestProcessor,
                logger = logger
            )

            val listener = spyk(object : TransferEvent.Listener {
                override fun onTransferEvent(event: TransferEvent) {
                    println("TEST LOG: $event")
                }
            })
            manager.addTransferEventListener(listener)

            manager.resolveRequestUri("openid4vp://example-request-uri")

            // Ensure RequestReceived happens before we reject
            verify(timeout = 2000) {
                listener.onTransferEvent(ofType(TransferEvent.RequestReceived::class))
            }

            manager.reject()

            verify(timeout = 2000) {
                listener.onTransferEvent(ofType(TransferEvent.ResponseSent::class))
            }

            // Double check the library interaction
            coVerify {
                mockOpenId4Vp.dispatch(
                    request = mockResolvedRequest,
                    consensus = Consensus.NegativeConsensus,
                    encryptionParameters = null
                )
            }

        } finally {
            unmockkStatic(Uri::class)
        }
    }
}