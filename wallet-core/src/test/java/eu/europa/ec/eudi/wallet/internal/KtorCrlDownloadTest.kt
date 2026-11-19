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

package eu.europa.ec.eudi.wallet.internal

import com.sun.net.httpserver.HttpServer
import eu.europa.ec.eudi.wallet.testCrl
import io.ktor.client.HttpClient
import org.junit.After
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

class KtorCrlDownloadTest {

    private val crl = testCrl()

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply { start() }
    private val client = HttpClient()

    @After
    fun close() {
        client.close()
        server.stop(0)
    }

    private fun serve(status: Int, delayMillis: Long = 0, body: ByteArray): URI {
        server.createContext("/crl") { exchange ->
            Thread.sleep(delayMillis)
            exchange.sendResponseHeaders(status, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        return URI("http://127.0.0.1:${server.address.port}/crl")
    }

    @Test
    fun `downloads the CRL with the given client`() {
        assertEquals(crl, ktorCrlDownload({ client })(serve(200, body = crl.encoded)))
    }

    @Test
    fun `fails on a response other than 200`() {
        assertFailsWith<IOException> { ktorCrlDownload({ client })(serve(404, body = crl.encoded)) }
    }

    @Test
    fun `fails on a response that is not a CRL`() {
        assertFails { ktorCrlDownload({ client })(serve(200, body = "<html/>".toByteArray())) }
    }

    @Test
    fun `fails when the download exceeds the timeout`() {
        val uri = serve(200, delayMillis = 2_000, body = crl.encoded)
        assertFails { ktorCrlDownload({ client }, timeout = 200.milliseconds)(uri) }
    }
}
