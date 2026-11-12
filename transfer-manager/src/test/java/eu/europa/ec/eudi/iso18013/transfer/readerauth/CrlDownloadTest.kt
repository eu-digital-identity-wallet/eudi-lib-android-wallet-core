/*
 *  Copyright (c) 2026 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.iso18013.transfer.readerauth

import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class CrlDownloadTest {

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        executor = Executors.newCachedThreadPool()
        createContext("/crl") { exchange ->
            val body = revokedCertificateCRL.encoded
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        // Sends one byte at a time, each well within the read timeout.
        createContext("/slow") { exchange ->
            exchange.sendResponseHeaders(200, 0)
            runCatching {
                exchange.responseBody.use { body ->
                    repeat(200) {
                        body.write(0x30)
                        body.flush()
                        Thread.sleep(50)
                    }
                }
            }
        }
        start()
    }

    private fun uri(path: String) = URI("http://127.0.0.1:${server.address.port}$path")

    @After
    fun stop() {
        server.stop(0)
    }

    @Test
    fun `downloads a CRL`() {
        assertEquals(revokedCertificateCRL, CrlCache.download(uri("/crl")))
    }

    @Test
    fun `a slow response cannot outlast the timeout`() {
        val start = System.nanoTime()
        assertFailsWith<IOException> { CrlCache.download(uri("/slow"), timeout = 500.milliseconds) }
        val elapsed = (System.nanoTime() - start).nanoseconds

        assertTrue(elapsed < 3.seconds, "download took $elapsed")
    }

    @Test
    fun `an interrupted download stops reading`() {
        var failure: Throwable? = null
        val download = thread {
            failure = runCatching { CrlCache.download(uri("/slow"), timeout = 30.seconds) }.exceptionOrNull()
        }
        Thread.sleep(300)
        val start = System.nanoTime()
        download.interrupt()
        download.join(5_000)
        val elapsed = (System.nanoTime() - start).nanoseconds

        assertTrue(!download.isAlive && elapsed < 2.seconds, "download still running after $elapsed")
        assertTrue(failure is IOException, "failed with $failure")
    }
}
