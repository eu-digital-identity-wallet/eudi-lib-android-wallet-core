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
package eu.europa.ec.eudi.wallet.trust

import org.bouncycastle.asn1.DERIA5String
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class X509SanExtensionsTest {

    @Test
    fun sanUrisReturnsEmptyForCertWithNoSan() {
        val cert = createCert()
        assertEquals(emptyList<String>(), cert.sanUris())
    }

    @Test
    fun sanUrisReturnsUriEntries() {
        val cert = createCert(
            uriSans = listOf("https://example.com"),
        )
        assertEquals(listOf("https://example.com"), cert.sanUris())
    }

    @Test
    fun sanUrisIgnoresDnsEntries() {
        val cert = createCert(
            dnsSans = listOf("example.com"),
        )
        assertEquals(emptyList<String>(), cert.sanUris())
    }

    @Test
    fun sanDnsNamesReturnsDnsEntries() {
        val cert = createCert(
            dnsSans = listOf("example.com"),
        )
        assertEquals(listOf("example.com"), cert.sanDnsNames())
    }

    @Test
    fun sanDnsNamesIgnoresUriEntries() {
        val cert = createCert(
            uriSans = listOf("https://example.com"),
        )
        assertEquals(emptyList<String>(), cert.sanDnsNames())
    }

    @Test
    fun sanUrisReturnsMultipleUris() {
        val cert = createCert(
            uriSans = listOf("https://first.com", "https://second.com"),
        )
        assertEquals(listOf("https://first.com", "https://second.com"), cert.sanUris())
    }

    @Test
    fun sanHandlesMixedSanTypes() {
        val cert = createCert(
            uriSans = listOf("https://example.com"),
            dnsSans = listOf("example.com"),
        )
        assertEquals(listOf("https://example.com"), cert.sanUris())
        assertEquals(listOf("example.com"), cert.sanDnsNames())
    }

    @Test
    fun sanDnsNamesReturnsEmptyForCertWithNoSan() {
        val cert = createCert()
        assertTrue(cert.sanDnsNames().isEmpty())
    }

    // -- helpers --

    private fun createCert(
        uriSans: List<String>? = null,
        dnsSans: List<String>? = null,
    ): X509Certificate {
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()

        val issuer = org.bouncycastle.asn1.x500.X500Name("CN=Test")
        val notBefore = Date(System.currentTimeMillis() - 86400000L)
        val notAfter = Date(System.currentTimeMillis() + 30 * 86400000L)
        val builder = JcaX509v3CertificateBuilder(
            issuer, BigInteger.ONE, notBefore, notAfter, issuer, keyPair.public,
        )

        val generalNames = buildList {
            uriSans?.forEach { uri ->
                add(GeneralName(GeneralName.uniformResourceIdentifier, DERIA5String(uri)))
            }
            dnsSans?.forEach { dns ->
                add(GeneralName(GeneralName.dNSName, dns))
            }
        }
        if (generalNames.isNotEmpty()) {
            builder.addExtension(
                Extension.subjectAlternativeName,
                false,
                DERSequence(generalNames.toTypedArray()),
            )
        }

        val signer = JcaContentSignerBuilder("SHA256WithECDSA").build(keyPair.private)
        return JcaX509CertificateConverter().getCertificate(builder.build(signer))
    }
}
