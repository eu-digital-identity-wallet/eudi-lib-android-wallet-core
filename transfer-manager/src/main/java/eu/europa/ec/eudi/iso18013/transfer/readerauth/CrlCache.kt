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
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.security.cert.CertificateFactory
import java.security.cert.X509CRL
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Keeps the CRLs named in certificates' HTTP(S) CRL distribution points, so that a trust path
 * validation does not download them every time.
 *
 * A CRL is downloaded again once [refreshInterval] has passed or its `nextUpdate` is reached. When
 * that download fails, the previous CRL is still served until its `nextUpdate`. The CRLs are handed
 * to the PKIX validator, which checks their signature and validity and applies the
 * [RevocationPolicy]; a certificate without a usable CRL here falls back to the platform's own
 * download.
 *
 * @param store where the CRLs are kept across restarts, or `null` to keep them in memory only
 * @param refreshInterval how long a CRL is used before it is downloaded again, must be positive
 */
class CrlCache(
    private val store: CrlStore? = null,
    private val refreshInterval: Duration = DEFAULT_REFRESH_INTERVAL,
    private val clock: () -> Instant = Instant::now,
    private val fetch: (URI) -> X509CRL = ::download,
) {
    init {
        require(refreshInterval.isPositive()) { "refreshInterval must be positive" }
    }

    private val entries = ConcurrentHashMap<URI, CrlStore.StoredCrl>()

    /**
     * One CRL for each of [certificates] that has one: a kept CRL of any of its distribution points
     * that needs no refresh, else the first one that downloads, else a previous one before its
     * `nextUpdate`.
     */
    fun crlsFor(certificates: Collection<X509Certificate>): List<X509CRL> =
        certificates.mapNotNull { crlFor(it.crlDistributionPoints()) }.distinct()

    /** The CRLs already kept for the distribution points of [certificates] that are not past their `nextUpdate`; nothing is downloaded. */
    fun cachedCrlsFor(certificates: Collection<X509Certificate>): List<X509CRL> {
        val now = clock()
        return certificates.flatMap { it.crlDistributionPoints() }.distinct()
            .mapNotNull { stored(it)?.crl?.takeUnless { crl -> crl.isExpiredAt(now) } }
    }

    private fun crlFor(uris: List<URI>): X509CRL? {
        val now = clock()
        return uris.firstNotNullOfOrNull { uri ->
            stored(uri)?.takeIf { now < it.fetchedAt + refreshInterval.toJavaDuration() && !it.crl.isExpiredAt(now) }?.crl
        } ?: uris.firstNotNullOfOrNull { uri ->
            runCatching { fetch(uri) }.onSuccess { keep(uri, CrlStore.StoredCrl(it, now)) }.getOrNull()
        } ?: uris.firstNotNullOfOrNull { uri ->
            stored(uri)?.crl?.takeUnless { it.isExpiredAt(now) }
        }
    }

    private fun stored(uri: URI): CrlStore.StoredCrl? =
        entries[uri] ?: store?.let { runCatching { it.load(uri) }.getOrNull() }?.also { entries.putIfAbsent(uri, it) }

    private fun keep(uri: URI, stored: CrlStore.StoredCrl) {
        entries[uri] = stored
        store?.let { runCatching { it.save(uri, stored) } }
    }

    private fun X509CRL.isExpiredAt(now: Instant): Boolean = nextUpdate?.toInstant()?.let { !now.isBefore(it) } ?: false

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

        private fun download(uri: URI): X509CRL {
            val connection = uri.toURL().openConnection() as HttpURLConnection
            return try {
                connection.connectTimeout = DEFAULT_DOWNLOAD_TIMEOUT.inWholeMilliseconds.toInt()
                connection.readTimeout = DEFAULT_DOWNLOAD_TIMEOUT.inWholeMilliseconds.toInt()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("HTTP ${connection.responseCode} from $uri")
                }
                connection.inputStream.use { CertificateFactory.getInstance("X.509").generateCRL(it) as X509CRL }
            } finally {
                connection.disconnect()
            }
        }
    }
}
