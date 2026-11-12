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

import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.DERIA5String
import org.bouncycastle.asn1.x509.CRLDistPoint
import org.bouncycastle.asn1.x509.DistributionPointName
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.security.cert.CertificateFactory
import java.security.cert.X509CRL
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

/**
 * Keeps the CRLs named in certificates' HTTP(S) CRL distribution points, so that a trust path
 * validation does not download them every time.
 *
 * A CRL is downloaded again once [refreshInterval] has passed or its `nextUpdate` is reached. A
 * download replaces the kept CRL only when it is current, accepted by the caller and not older than
 * the kept one; otherwise the next distribution point is tried, and when none serves one, the
 * previous CRL is still served until its `nextUpdate`.
 *
 * @param store where the CRLs are kept across restarts, or `null` to keep them in memory only
 * @param refreshInterval how long a CRL is used before it is downloaded again, must be positive;
 * [Duration.INFINITE] keeps a CRL until its `nextUpdate`
 */
class CrlCache(
    private val store: CrlStore? = null,
    private val refreshInterval: Duration = DEFAULT_REFRESH_INTERVAL,
    private val clock: () -> Instant = Instant::now,
    private val fetch: (URI) -> X509CRL = { download(it) },
) {
    init {
        require(refreshInterval.isPositive()) { "refreshInterval must be positive" }
    }

    private val entries = ConcurrentHashMap<URI, CrlStore.StoredCrl>()
    private val locks = ConcurrentHashMap<URI, Any>()

    /**
     * One CRL for each of [certificates] that has one: a kept CRL of any of its distribution points
     * that needs no refresh, else the first one that downloads, else a previous one before its
     * `nextUpdate`. Only a current CRL that [isUsable] accepts for the certificate, typically one
     * whose signature verifies against its issuer, is served or kept.
     *
     * @throws InterruptedException when the thread is interrupted, so that no further distribution
     * point is contacted
     */
    fun crlsFor(
        certificates: Collection<X509Certificate>,
        isUsable: (certificate: X509Certificate, crl: X509CRL) -> Boolean = { _, _ -> true },
    ): List<X509CRL> = certificates.mapNotNull { crlFor(it, isUsable) }.distinct()

    /** The CRLs already kept for the distribution points of [certificates] that are not past their `nextUpdate`; nothing is downloaded. */
    fun cachedCrlsFor(certificates: Collection<X509Certificate>): List<X509CRL> {
        val now = clock()
        return certificates.flatMap { it.crlDistributionPoints() }.distinct()
            .mapNotNull { stored(it)?.crl?.takeUnless { crl -> crl.isExpiredAt(now) } }
    }

    private fun crlFor(certificate: X509Certificate, isUsable: (X509Certificate, X509CRL) -> Boolean): X509CRL? {
        val uris = certificate.crlDistributionPoints()
        val now = clock()
        fun X509CRL.usable() = isCurrentAt(now) && isUsable(certificate, this)
        return uris.firstNotNullOfOrNull { uri ->
            stored(uri)?.takeIf { it.isFreshAt(now) }?.crl?.takeIf { it.usable() }
        } ?: uris.firstNotNullOfOrNull { uri ->
            tryFetch(uri)?.takeIf { it.usable() }?.let { keep(uri, CrlStore.StoredCrl(it, now)).crl.takeIf { crl -> crl.usable() } }
        } ?: uris.firstNotNullOfOrNull { uri ->
            stored(uri)?.crl?.takeIf { it.usable() }
        }
    }

    private fun CrlStore.StoredCrl.isFreshAt(now: Instant): Boolean =
        java.time.Duration.between(fetchedAt, now).toKotlinDuration() < refreshInterval && !crl.isExpiredAt(now)

    private fun tryFetch(uri: URI): X509CRL? {
        if (Thread.currentThread().isInterrupted) throw InterruptedException()
        return try {
            fetch(uri)
        } catch (e: Exception) {
            if (Thread.currentThread().isInterrupted || generateSequence<Throwable>(e) { it.cause }.any { it is InterruptedException }) {
                throw InterruptedException().apply { initCause(e) }
            }
            null
        }
    }

    private fun stored(uri: URI): CrlStore.StoredCrl? =
        entries[uri] ?: store?.let { runCatching { it.load(uri) }.getOrNull() }?.also { entries.putIfAbsent(uri, it) }

    // Serialized per distribution point, so that an older CRL never replaces a newer one.
    private fun keep(uri: URI, downloaded: CrlStore.StoredCrl): CrlStore.StoredCrl =
        synchronized(locks.computeIfAbsent(uri) { Any() }) {
            val previous = stored(uri)
            if (previous != null && previous.crl.thisUpdate.after(downloaded.crl.thisUpdate)) return previous
            entries[uri] = downloaded
            store?.let { runCatching { it.save(uri, downloaded) } }
            downloaded
        }

    private fun X509CRL.isExpiredAt(now: Instant): Boolean = nextUpdate?.toInstant()?.let { !now.isBefore(it) } ?: false

    private fun X509CRL.isCurrentAt(now: Instant): Boolean = !thisUpdate.toInstant().isAfter(now) && !isExpiredAt(now)

    companion object {
        val DEFAULT_REFRESH_INTERVAL: Duration = 1.hours

        /** The upper bound for one CRL download. */
        val DEFAULT_DOWNLOAD_TIMEOUT: Duration = 5.seconds

        private fun X509Certificate.crlDistributionPoints(): List<URI> {
            val extension = getExtensionValue(Extension.cRLDistributionPoints.id) ?: return emptyList()
            val names = runCatching {
                CRLDistPoint.getInstance(ASN1Primitive.fromByteArray(ASN1OctetString.getInstance(extension).octets))
                    .distributionPoints
                    .mapNotNull { it.distributionPoint?.takeIf { name -> name.type == DistributionPointName.FULL_NAME } }
                    .flatMap { GeneralNames.getInstance(it.name).names.asList() }
                    .filter { it.tagNo == GeneralName.uniformResourceIdentifier }
            }.getOrDefault(emptyList())
            return names.mapNotNull { name ->
                runCatching { URI(DERIA5String.getInstance(name.name).string) }.getOrNull()
                    ?.takeIf { it.scheme.equals("http", ignoreCase = true) || it.scheme.equals("https", ignoreCase = true) }
            }
        }

        private val deadlines = ScheduledThreadPoolExecutor(1) { runnable ->
            Thread(runnable, "CrlCache-deadline").apply { isDaemon = true }
        }.apply { removeOnCancelPolicy = true }

        /**
         * Downloads with [HttpURLConnection] within [timeout] in all, so a slow server cannot outlast
         * it: the connection is closed once it passes, and the body is read in parts, checking the
         * deadline and the thread's interruption before each.
         */
        @JvmSynthetic
        internal fun download(uri: URI, timeout: Duration = DEFAULT_DOWNLOAD_TIMEOUT): X509CRL {
            val end = System.nanoTime() + timeout.inWholeNanoseconds
            val connection = uri.toURL().openConnection() as HttpURLConnection
            val deadline = deadlines.schedule({ connection.disconnect() }, timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            return try {
                connection.connectTimeout = timeout.inWholeMilliseconds.toInt()
                connection.readTimeout = timeout.inWholeMilliseconds.toInt()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("HTTP ${connection.responseCode} from $uri")
                }
                val content = ByteArrayOutputStream()
                connection.inputStream.use { input ->
                    val buffer = ByteArray(BUFFER_BYTES)
                    while (true) {
                        if (Thread.currentThread().isInterrupted) throw InterruptedIOException("download of $uri interrupted")
                        if (System.nanoTime() - end > 0) throw SocketTimeoutException("download of $uri exceeded $timeout")
                        val read = input.read(buffer)
                        if (read < 0) break
                        content.write(buffer, 0, read)
                    }
                }
                CertificateFactory.getInstance("X.509").generateCRL(content.toByteArray().inputStream()) as X509CRL
            } catch (e: IOException) {
                if (deadline.isDone) throw IOException("download of $uri exceeded $timeout", e)
                throw e
            } finally {
                deadline.cancel(false)
                connection.disconnect()
            }
        }

        private const val BUFFER_BYTES = 8192
    }
}
