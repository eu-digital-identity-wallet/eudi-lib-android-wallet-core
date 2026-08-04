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
package eu.europa.ec.eudi.wallet.registration.relyingparty

import eu.europa.ec.eudi.iso18013.transfer.response.RequestedAttestationInfo
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigInteger
import java.security.cert.X509Certificate
import java.util.Date

private val CERTIFICATE = byteArrayOf(1, 2, 3)
private val ATTESTATIONS = listOf(RequestedAttestationInfo(format = "mso_mdoc", docType = "org.iso.18013.5.1.mDL"))
private val RESULT = RegistrationCertificateResult.Verified(RegistrationCertificate(name = "Relying Party"))

class ResolvedWrpRegistrationTest {

    private val resolvedRegistration = ResolvedWrpRegistration()
    private val accessCertificate = stubCertificate(serialNumber = 1)

    @Test
    fun `an evaluation is taken by a request with the same inputs`() {
        resolvedRegistration.publish(CERTIFICATE, accessCertificate, ATTESTATIONS, RESULT)

        val taken = resolvedRegistration.take(CERTIFICATE.copyOf(), accessCertificate, ATTESTATIONS.toList())

        assertEquals(RESULT, taken)
    }

    @Test
    fun `an evaluation is not taken twice`() {
        resolvedRegistration.publish(CERTIFICATE, accessCertificate, ATTESTATIONS, RESULT)
        resolvedRegistration.take(CERTIFICATE, accessCertificate, ATTESTATIONS)

        assertNull(resolvedRegistration.take(CERTIFICATE, accessCertificate, ATTESTATIONS))
    }

    @Test
    fun `an evaluation is not taken by a request with a different registration certificate`() {
        resolvedRegistration.publish(CERTIFICATE, accessCertificate, ATTESTATIONS, RESULT)

        assertNull(resolvedRegistration.take(byteArrayOf(9, 9, 9), accessCertificate, ATTESTATIONS))
    }

    @Test
    fun `an evaluation is not taken by a request with a different access certificate`() {
        resolvedRegistration.publish(CERTIFICATE, accessCertificate, ATTESTATIONS, RESULT)

        assertNull(resolvedRegistration.take(CERTIFICATE, stubCertificate(serialNumber = 2), ATTESTATIONS))
    }

    @Test
    fun `an evaluation is not taken by a request without an access certificate`() {
        resolvedRegistration.publish(CERTIFICATE, accessCertificate, ATTESTATIONS, RESULT)

        assertNull(resolvedRegistration.take(CERTIFICATE, null, ATTESTATIONS))
    }

    @Test
    fun `an evaluation is not taken by a request asking for different attestations`() {
        resolvedRegistration.publish(CERTIFICATE, accessCertificate, ATTESTATIONS, RESULT)

        val other = listOf(RequestedAttestationInfo(format = "mso_mdoc", docType = "eu.europa.ec.eudi.pid.1"))
        assertNull(resolvedRegistration.take(CERTIFICATE, accessCertificate, other))
    }

    @Test
    fun `an evaluation is not taken by a request carrying no registration certificate`() {
        resolvedRegistration.publish(CERTIFICATE, accessCertificate, ATTESTATIONS, RESULT)

        assertNull(resolvedRegistration.take(null, accessCertificate, ATTESTATIONS))
    }

    @Test
    fun `an evaluation is discarded once a request whose inputs differ has looked for it`() {
        resolvedRegistration.publish(CERTIFICATE, accessCertificate, ATTESTATIONS, RESULT)
        resolvedRegistration.take(byteArrayOf(9, 9, 9), accessCertificate, ATTESTATIONS)

        assertNull(resolvedRegistration.take(CERTIFICATE, accessCertificate, ATTESTATIONS))
    }

    @Test
    fun `publishing again replaces the previous evaluation`() {
        val replaced = RegistrationCertificateResult.Failed(RegistrationFailureReason.REVOKED)
        resolvedRegistration.publish(CERTIFICATE, accessCertificate, ATTESTATIONS, RESULT)
        resolvedRegistration.publish(CERTIFICATE, accessCertificate, ATTESTATIONS, replaced)

        assertEquals(replaced, resolvedRegistration.take(CERTIFICATE, accessCertificate, ATTESTATIONS))
    }

    @Test
    fun `a cleared evaluation is not taken`() {
        resolvedRegistration.publish(CERTIFICATE, accessCertificate, ATTESTATIONS, RESULT)
        resolvedRegistration.clear()

        assertNull(resolvedRegistration.take(CERTIFICATE, accessCertificate, ATTESTATIONS))
    }

    @Test
    fun `nothing is taken when no evaluation was published`() {
        assertNull(resolvedRegistration.take(CERTIFICATE, accessCertificate, ATTESTATIONS))
    }
}

/**
 * A certificate that is equal only to another built from the same [serialNumber]. Certificate equality
 * compares the encoded form, so the stub reports the serial number as its encoding.
 */
private fun stubCertificate(serialNumber: Int): X509Certificate =
    object : X509Certificate() {
        override fun getEncoded(): ByteArray = byteArrayOf(serialNumber.toByte())
        override fun equals(other: Any?): Boolean =
            other is X509Certificate && other.encoded.contentEquals(encoded)

        override fun hashCode(): Int = serialNumber

        override fun getSerialNumber(): BigInteger = BigInteger.valueOf(serialNumber.toLong())
        override fun checkValidity() = Unit
        override fun checkValidity(date: Date?) = Unit
        override fun getVersion(): Int = 3
        override fun getIssuerDN() = null
        override fun getSubjectDN() = null
        override fun getNotBefore(): Date = Date(0)
        override fun getNotAfter(): Date = Date(0)
        override fun getTBSCertificate(): ByteArray = ByteArray(0)
        override fun getSignature(): ByteArray = ByteArray(0)
        override fun getSigAlgName(): String = "none"
        override fun getSigAlgOID(): String = "none"
        override fun getSigAlgParams(): ByteArray = ByteArray(0)
        override fun getIssuerUniqueID(): BooleanArray? = null
        override fun getSubjectUniqueID(): BooleanArray? = null
        override fun getKeyUsage(): BooleanArray? = null
        override fun getBasicConstraints(): Int = -1
        override fun verify(key: java.security.PublicKey?) = Unit
        override fun verify(key: java.security.PublicKey?, sigProvider: String?) = Unit
        override fun toString(): String = "stub-$serialNumber"
        override fun getPublicKey(): java.security.PublicKey? = null
        override fun hasUnsupportedCriticalExtension(): Boolean = false
        override fun getCriticalExtensionOIDs(): MutableSet<String> = mutableSetOf()
        override fun getNonCriticalExtensionOIDs(): MutableSet<String> = mutableSetOf()
        override fun getExtensionValue(oid: String?): ByteArray? = null
    }
