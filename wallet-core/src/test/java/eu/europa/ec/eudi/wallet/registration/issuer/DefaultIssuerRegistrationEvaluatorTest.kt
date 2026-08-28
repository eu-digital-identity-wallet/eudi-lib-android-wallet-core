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

package eu.europa.ec.eudi.wallet.registration.issuer

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifierPredicate
import eu.europa.ec.eudi.statium.StatusIndex
import eu.europa.ec.eudi.statium.StatusReference
import eu.europa.ec.eudi.wallet.registration.CredentialMeta
import eu.europa.ec.eudi.wallet.registration.ProvidedAttestation
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import eu.europa.ec.eudi.wallet.registration.RegistrationIdentifier
import eu.europa.ec.eudi.wallet.registration.RevocationOutcome
import kotlinx.coroutines.test.runTest
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.Date

class DefaultIssuerRegistrationEvaluatorTest {

    private val evaluator = DefaultIssuerRegistrationEvaluator(
        isPid = isPidPredicate,
        checkRevocation = { RevocationOutcome.VALID },
    )

    @Test
    fun `an issuer entitled and registered for what it offers is verified`() = runTest {
        val registration = registration(
            entitlements = listOf(IssuerEntitlements.PID),
            provides = listOf(pidProvidedAttestation),
        )

        val result = evaluator.evaluate(
            registration = registration,
            accessCertificate = certificateWithOrgId(ORG_ID),
            offeredAttestations = listOf(pidMdoc),
        )

        val verified = result as RegistrationCertificateResult.Verified
        assertEquals(registration, verified.registration)
        assertEquals(emptyList<Any>(), verified.overProvidedAttestations)
    }

    @Test
    fun `an issuer missing the entitlement for what it offers fails validation`() = runTest {
        val registration = registration(
            entitlements = listOf(IssuerEntitlements.QEAA),
            provides = listOf(pidProvidedAttestation),
        )

        val result = evaluator.evaluate(
            registration = registration,
            accessCertificate = certificateWithOrgId(ORG_ID),
            offeredAttestations = listOf(pidMdoc),
        )

        val failed = result as RegistrationCertificateResult.Failed
        assertEquals(RegistrationFailureReason.ENTITLEMENT_MISSING, failed.reason)
    }

    @Test
    fun `a failed entitlement check still carries the parsed registration`() = runTest {
        val registration = registration(
            entitlements = emptyList(),
            provides = listOf(pidProvidedAttestation),
        )

        val result = evaluator.evaluate(
            registration = registration,
            accessCertificate = certificateWithOrgId(ORG_ID),
            offeredAttestations = listOf(pidMdoc),
        )

        val failed = result as RegistrationCertificateResult.Failed
        assertEquals(registration, failed.registration)
    }

    @Test
    fun `the entitlement check precedes the over-providing check`() = runTest {
        // Neither entitled nor registered for the offer: ISSU_24a is reported ahead of ISSU_24b.
        val registration = registration(entitlements = emptyList(), provides = emptyList())

        val result = evaluator.evaluate(
            registration = registration,
            accessCertificate = certificateWithOrgId(ORG_ID),
            offeredAttestations = listOf(pidMdoc),
        )

        val failed = result as RegistrationCertificateResult.Failed
        assertEquals(RegistrationFailureReason.ENTITLEMENT_MISSING, failed.reason)
    }

    @Test
    fun `a certificate validity failure is reported ahead of the entitlement check`() = runTest {
        val registration = registration(entitlements = emptyList(), provides = emptyList())

        val result = evaluator.evaluate(
            registration = registration,
            accessCertificate = certificateWithOrgId("SOME-OTHER-ORG"),
            offeredAttestations = listOf(pidMdoc),
        )

        val failed = result as RegistrationCertificateResult.Failed
        assertEquals(RegistrationFailureReason.NOT_BOUND_TO_REQUESTER, failed.reason)
    }

    @Test
    fun `an entitled issuer offering outside its registered scope is verified and over-providing`() = runTest {
        val registration = registration(
            entitlements = listOf(IssuerEntitlements.PID),
            provides = emptyList(),
        )

        val result = evaluator.evaluate(
            registration = registration,
            accessCertificate = certificateWithOrgId(ORG_ID),
            offeredAttestations = listOf(pidMdoc),
        )

        val verified = result as RegistrationCertificateResult.Verified
        assertEquals(1, verified.overProvidedAttestations.size)
    }
}

private const val ORG_ID = "ORG-123"

private val isPidPredicate = AttestationIdentifierPredicate.any(
    setOf(AttestationIdentifier.MDoc("eu.europa.ec.eudi.pid.1")),
)

private val pidMdoc = OfferedAttestation(
    format = "mso_mdoc",
    meta = CredentialMeta(doctypeValue = "eu.europa.ec.eudi.pid.1"),
)

private val pidProvidedAttestation = ProvidedAttestation(
    format = "mso_mdoc",
    meta = CredentialMeta(doctypeValue = "eu.europa.ec.eudi.pid.1"),
)

private fun registration(
    entitlements: List<String>,
    provides: List<ProvidedAttestation>,
): RegistrationCertificate =
    RegistrationCertificate(
        identifiers = listOf(RegistrationIdentifier(value = ORG_ID)),
        name = "Nordic PID Provider",
        entitlements = entitlements,
        providedAttestations = provides,
        status = StatusReference(uri = "https://issuer.example/status/1", index = StatusIndex(5)),
    )

private fun certificateWithOrgId(orgId: String): X509Certificate =
    selfSignedCertificate(
        X500NameBuilder(BCStyle.INSTANCE)
            .addRDN(BCStyle.CN, "Test Credential Issuer")
            .addRDN(BCStyle.ORGANIZATION_IDENTIFIER, orgId)
            .build(),
    )

private fun selfSignedCertificate(subject: X500Name): X509Certificate {
    val keyPair = ecKeyPair()
    val now = Instant.now()
    val builder = JcaX509v3CertificateBuilder(
        subject,
        BigInteger(64, SecureRandom()),
        Date.from(now.minusSeconds(3600)),
        Date.from(now.plusSeconds(86400)),
        subject,
        keyPair.public,
    )
    val signer = JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)
    return JcaX509CertificateConverter().getCertificate(builder.build(signer))
}

private fun ecKeyPair(): KeyPair =
    KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()
