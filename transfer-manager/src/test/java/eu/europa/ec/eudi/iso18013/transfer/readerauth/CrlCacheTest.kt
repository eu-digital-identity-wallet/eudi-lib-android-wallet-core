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

import org.junit.Test
import java.io.IOException
import java.net.URI
import java.security.cert.X509CRL
import java.time.Instant
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class CrlCacheTest {

    private var now = Instant.now()
    private val downloads = mutableListOf<URI>()
    private var available = true
    private val failing = mutableSetOf<URI>()
    private val served = mutableMapOf<URI, X509CRL>()
    private val first = URI("https://example.com/crl.pem")
    private val mirror = URI("https://mirror.example.com/crl.pem")

    private class MemoryStore : CrlStore {
        val crls = ConcurrentHashMap<URI, CrlStore.StoredCrl>()
        override fun load(uri: URI) = crls[uri]
        override fun save(uri: URI, stored: CrlStore.StoredCrl) {
            crls[uri] = stored
        }
    }

    private fun cache(refreshInterval: Duration = 1.hours, store: CrlStore? = null) = CrlCache(
        store = store,
        refreshInterval = refreshInterval,
        clock = { now },
        fetch = { uri ->
            downloads += uri
            if (!available || uri in failing) throw IOException("offline")
            served[uri] ?: revokedCertificateCRL
        },
    )

    private fun CrlCache.crls(): List<X509CRL> = crlsFor(listOf(validCertificate))

    private fun CrlCache.crlsOfTwoDistributionPoints(): List<X509CRL> = crlsFor(listOf(certificateWithTwoDistributionPoints))

    @Test
    fun `downloads the CRL of the distribution point`() {
        assertEquals(listOf(revokedCertificateCRL), cache().crls())
        assertEquals(listOf(first), downloads)
    }

    @Test
    fun `downloads only the first distribution point that serves a CRL`() {
        assertEquals(listOf(revokedCertificateCRL), cache().crlsOfTwoDistributionPoints())
        assertEquals(listOf(first), downloads)
    }

    @Test
    fun `falls through to the next distribution point when one fails`() {
        failing += first

        assertEquals(listOf(revokedCertificateCRL), cache().crlsOfTwoDistributionPoints())
        assertEquals(listOf(first, mirror), downloads)
    }

    @Test
    fun `a kept CRL of any distribution point spares the downloads`() {
        failing += first
        val cache = cache()
        cache.crlsOfTwoDistributionPoints()
        downloads.clear()
        now += 59.minutes.toJavaDuration()

        assertEquals(listOf(revokedCertificateCRL), cache.crlsOfTwoDistributionPoints())
        assertEquals(emptyList(), downloads)
    }

    @Test
    fun `tries every distribution point before serving a previous CRL`() {
        val cache = cache()
        cache.crlsOfTwoDistributionPoints()
        downloads.clear()
        available = false
        now += 2.hours.toJavaDuration()

        assertEquals(listOf(revokedCertificateCRL), cache.crlsOfTwoDistributionPoints())
        assertEquals(listOf(first, mirror), downloads)
    }

    @Test
    fun `certificates without a distribution point need no CRL`() {
        assertEquals(emptyList(), cache().crlsFor(listOf(trustedCertificate)))
        assertEquals(emptyList(), downloads)
    }

    @Test
    fun `reuses the CRL within the refresh interval`() {
        val cache = cache()
        cache.crls()
        now += 59.minutes.toJavaDuration()

        assertEquals(listOf(revokedCertificateCRL), cache.crls())
        assertEquals(1, downloads.size)
    }

    @Test
    fun `downloads the CRL again after the refresh interval`() {
        val cache = cache()
        cache.crls()
        now += 1.hours.toJavaDuration()

        cache.crls()
        assertEquals(2, downloads.size)
    }

    @Test
    fun `downloads the CRL again once it reaches its nextUpdate`() {
        val cache = cache(refreshInterval = 365.days)
        cache.crls()
        now += 31.days.toJavaDuration()

        cache.crls()
        assertEquals(2, downloads.size)
    }

    @Test
    fun `serves the previous CRL while the download fails, until its nextUpdate`() {
        val cache = cache()
        cache.crls()
        available = false
        now += 2.hours.toJavaDuration()

        assertEquals(listOf(revokedCertificateCRL), cache.crls())

        now += 31.days.toJavaDuration()
        assertEquals(emptyList(), cache.crls())
    }

    @Test
    fun `an unavailable CRL is left out`() {
        available = false

        assertEquals(emptyList(), cache().crls())
    }

    @Test
    fun `a malformed distribution point does not hide the others`() {
        assertEquals(listOf(revokedCertificateCRL), cache().crlsFor(listOf(certificateWithAMalformedDistributionPoint)))
        assertEquals(listOf(URI("https://example.com/crl.pem")), downloads)
    }

    @Test
    fun `cached CRLs are served without a download until their nextUpdate`() {
        val cache = cache()
        assertEquals(emptyList(), cache.cachedCrlsFor(listOf(validCertificate)))
        assertEquals(emptyList(), downloads)

        cache.crls()
        now += 2.hours.toJavaDuration()
        assertEquals(listOf(revokedCertificateCRL), cache.cachedCrlsFor(listOf(validCertificate)))
        assertEquals(1, downloads.size)

        now += 31.days.toJavaDuration()
        assertEquals(emptyList(), cache.cachedCrlsFor(listOf(validCertificate)))
    }

    @Test
    fun `a CRL kept in the store outlives the cache`() {
        val store = MemoryStore()
        cache(store = store).crls()
        available = false
        now += 59.minutes.toJavaDuration()

        val restarted = cache(store = store)
        assertEquals(listOf(revokedCertificateCRL), restarted.crls())
        assertEquals(listOf(revokedCertificateCRL), restarted.cachedCrlsFor(listOf(validCertificate)))
        assertEquals(1, downloads.size)
    }

    @Test
    fun `a CRL kept in the store is downloaded again after the refresh interval`() {
        val store = MemoryStore()
        cache(store = store).crls()
        now += 1.hours.toJavaDuration()

        cache(store = store).crls()
        assertEquals(2, downloads.size)
    }

    @Test
    fun `a failing store leaves the cache in memory`() {
        val failing = object : CrlStore {
            override fun load(uri: URI): CrlStore.StoredCrl = throw IOException("unreadable")
            override fun save(uri: URI, stored: CrlStore.StoredCrl) = throw IOException("unwritable")
        }
        val cache = cache(store = failing)

        assertEquals(listOf(revokedCertificateCRL), cache.crls())
        assertEquals(listOf(revokedCertificateCRL), cache.crls())
        assertEquals(1, downloads.size)
    }

    @Test
    fun `an infinite refresh interval keeps the CRL until its nextUpdate`() {
        val cache = cache(refreshInterval = Duration.INFINITE)
        cache.crls()
        now += 29.days.toJavaDuration()

        assertEquals(listOf(revokedCertificateCRL), cache.crls())
        assertEquals(1, downloads.size)

        now += 2.days.toJavaDuration()
        cache.crls()
        assertEquals(2, downloads.size)
    }

    @Test
    fun `a download that is not current neither replaces the kept CRL nor stops the search`() {
        val cache = cache()
        cache.crlsOfTwoDistributionPoints()
        downloads.clear()
        served[first] = crlIssuedAt(Date.from(now - 40.days.toJavaDuration()))
        failing += mirror
        now += 2.hours.toJavaDuration()

        assertEquals(listOf(revokedCertificateCRL), cache.crlsOfTwoDistributionPoints())
        assertEquals(listOf(first, mirror), downloads)
        assertEquals(listOf(revokedCertificateCRL), cache.cachedCrlsFor(listOf(validCertificate)))
    }

    @Test
    fun `a CRL that is not yet current is not served`() {
        served[first] = crlIssuedAt(Date.from(now + 1.hours.toJavaDuration()))

        assertEquals(emptyList(), cache().crls())
    }

    @Test
    fun `a CRL the caller rejects is neither served nor kept`() {
        val cache = cache()
        cache.crls()
        served[first] = forgedEmptyCRL
        now += 2.hours.toJavaDuration()

        assertEquals(listOf(revokedCertificateCRL), cache.crlsFor(listOf(validCertificate)) { _, crl -> crl !== forgedEmptyCRL })
        assertEquals(listOf(revokedCertificateCRL), cache.cachedCrlsFor(listOf(validCertificate)))
    }

    @Test
    fun `a CRL the caller rejects makes way for the next distribution point`() {
        served[first] = forgedEmptyCRL

        assertEquals(
            listOf(revokedCertificateCRL),
            cache().crlsFor(listOf(certificateWithTwoDistributionPoints)) { _, crl -> crl !== forgedEmptyCRL },
        )
        assertEquals(listOf(first, mirror), downloads)
    }

    @Test
    fun `an older CRL does not replace a newer one`() {
        val store = MemoryStore()
        val cache = cache(store = store)
        cache.crls()
        served[first] = crlIssuedAt(Date.from(now - 1.days.toJavaDuration()))
        now += 2.hours.toJavaDuration()

        assertEquals(listOf(revokedCertificateCRL), cache.crls())
        assertEquals(2, downloads.size)
        assertEquals(revokedCertificateCRL, store.crls[first]?.crl)
    }

    @Test
    fun `an interrupted download is not followed by the next distribution point`() {
        val cache = CrlCache(fetch = { uri ->
            downloads += uri
            Thread.currentThread().interrupt()
            throw IOException("interrupted")
        })
        try {
            assertFailsWith<InterruptedException> { cache.crlsOfTwoDistributionPoints() }
            assertEquals(listOf(first), downloads)
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `the refresh interval must be positive`() {
        assertFailsWith<IllegalArgumentException> { cache(refreshInterval = Duration.ZERO) }
    }
}
