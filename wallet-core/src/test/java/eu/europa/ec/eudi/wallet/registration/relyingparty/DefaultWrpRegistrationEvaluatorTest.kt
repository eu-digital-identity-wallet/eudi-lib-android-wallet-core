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

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedAttestationInfo
import eu.europa.ec.eudi.statium.StatusIndex
import eu.europa.ec.eudi.statium.StatusReference
import eu.europa.ec.eudi.wallet.registration.CredentialMeta
import eu.europa.ec.eudi.wallet.registration.Intermediary
import eu.europa.ec.eudi.wallet.registration.RegisteredClaim
import eu.europa.ec.eudi.wallet.registration.RegisteredCredential
import eu.europa.ec.eudi.wallet.registration.RegistrationIdentifier
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.Date
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class DefaultWrpRegistrationEvaluatorTest {

    // Default evaluator with a passing revocation check.
    private val evaluator = DefaultWrpRegistrationEvaluator(checkRevocation = { RevocationOutcome.VALID })

    @Test
    fun `a bound registration within scope is verified with no over-ask`() = runTest {
        val registration = registration(identifier = "ORG-123")

        val result = evaluator.evaluate(
            registration = registration,
            accessCertificate = certificateWithOrgId("ORG-123"),
            requestedAttestations = emptyList(),
        )

        val verified = result as WrpRegistrationResult.Verified
        assertEquals(registration, verified.registration)
        assertTrue(verified.overAskedClaims.isEmpty())
    }

    @Test
    fun `an expired registration is rejected`() = runTest {
        val result = evaluator.evaluate(
            registration = registration(identifier = "ORG-123")
                .copy(expiresAt = (Clock.System.now() - 1.hours)),
            accessCertificate = certificateWithOrgId("ORG-123"),
            requestedAttestations = emptyList(),
        )

        assertEquals(RegistrationFailureReason.EXPIRED, (result as WrpRegistrationResult.Failed).reason)
    }

    @Test
    fun `a registration not bound to the requester is rejected`() = runTest {
        val result = evaluator.evaluate(
            registration = registration(identifier = "ORG-OTHER"),
            accessCertificate = certificateWithOrgId("ORG-123"),
            requestedAttestations = emptyList(),
        )

        val failed = result as WrpRegistrationResult.Failed
        assertEquals(RegistrationFailureReason.NOT_BOUND_TO_REQUESTER, failed.reason)
        // the parsed registration is carried on the failure result
        assertEquals("ORG-OTHER", failed.registration?.identifiers?.single()?.value)
    }

    @Test
    fun `a registration both expired and not bound reports not bound first`() = runTest {
        val result = evaluator.evaluate(
            // binding is surfaced before expiry
            registration = registration(identifier = "ORG-OTHER")
                .copy(expiresAt = (Clock.System.now() - 1.hours)),
            accessCertificate = certificateWithOrgId("ORG-123"),
            requestedAttestations = emptyList(),
        )

        assertEquals(
            RegistrationFailureReason.NOT_BOUND_TO_REQUESTER,
            (result as WrpRegistrationResult.Failed).reason,
        )
    }

    @Test
    fun `a registration without a status reference is rejected`() = runTest {
        val result = evaluator.evaluate(
            registration = registration(identifier = "ORG-123").copy(status = null),
            accessCertificate = certificateWithOrgId("ORG-123"),
            requestedAttestations = emptyList(),
        )

        assertEquals(RegistrationFailureReason.STATUS_MISSING, (result as WrpRegistrationResult.Failed).reason)
    }

    @Test
    fun `an inconclusive revocation check is treated strictly as a failure`() = runTest {
        val revocationChecking = DefaultWrpRegistrationEvaluator(
            statusTrust = mockk<ReaderTrustStore>(),
            httpClientFactory = { throw RuntimeException("status endpoint unreachable") },
        )

        val result = revocationChecking.evaluate(
            registration = registration(identifier = "ORG-123"),
            accessCertificate = certificateWithOrgId("ORG-123"),
            requestedAttestations = emptyList(),
        )

        assertEquals(
            RegistrationFailureReason.REVOCATION_STATUS_UNKNOWN,
            (result as WrpRegistrationResult.Failed).reason,
        )
    }

    @Test
    fun `a revoked certificate is rejected`() = runTest {
        val revoked = DefaultWrpRegistrationEvaluator(checkRevocation = { RevocationOutcome.REVOKED })

        val result = revoked.evaluate(
            registration = registration(identifier = "ORG-123"),
            accessCertificate = certificateWithOrgId("ORG-123"),
            requestedAttestations = emptyList(),
        )

        assertEquals(RegistrationFailureReason.REVOKED, (result as WrpRegistrationResult.Failed).reason)
    }

    @Test
    fun `an unchecked revocation is treated as an unknown revocation failure`() = runTest {
        val notChecked = DefaultWrpRegistrationEvaluator(checkRevocation = { RevocationOutcome.NOT_CHECKED })

        val result = notChecked.evaluate(
            registration = registration(identifier = "ORG-123"),
            accessCertificate = certificateWithOrgId("ORG-123"),
            requestedAttestations = emptyList(),
        )

        assertEquals(
            RegistrationFailureReason.REVOCATION_STATUS_UNKNOWN,
            (result as WrpRegistrationResult.Failed).reason,
        )
    }

    @Test
    fun `a registration presented by its registered intermediary is bound via the intermediary`() =
        runTest {
            val registration = registration(identifier = "ORG-FINAL-WRP")
                .copy(intermediary = Intermediary(identifier = "ORG-INTERMEDIARY"))

            val result = evaluator.evaluate(
                registration = registration,
                // The intermediary authenticates with its own access certificate.
                accessCertificate = certificateWithOrgId("ORG-INTERMEDIARY"),
                requestedAttestations = emptyList(),
            )

            assertTrue(result is WrpRegistrationResult.Verified)
        }

    @Test
    fun `a registration naming an intermediary is not bound to the final party's own certificate`() =
        runTest {
            val registration = registration(identifier = "ORG-FINAL-WRP")
                .copy(intermediary = Intermediary(identifier = "ORG-INTERMEDIARY"))

            val result = evaluator.evaluate(
                registration = registration,
                accessCertificate = certificateWithOrgId("ORG-FINAL-WRP"),
                requestedAttestations = emptyList(),
            )

            assertEquals(
                RegistrationFailureReason.NOT_BOUND_TO_REQUESTER,
                (result as WrpRegistrationResult.Failed).reason,
            )
        }

    @Test
    fun `a natural person registration is bound via its access certificate serial number`() = runTest {
        val result = evaluator.evaluate(
            registration = registration(identifier = "NP-123456789"),
            accessCertificate = certificateWithSerialNumber("NP-123456789"),
            requestedAttestations = emptyList(),
        )

        assertTrue(result is WrpRegistrationResult.Verified)
    }

    @Test
    fun `claims outside the registered scope are reported as over-asked on a verified result`() =
        runTest {
            val registration = registration(identifier = "ORG-123").copy(
                requestedCredentials = listOf(
                    RegisteredCredential(
                        format = "mso_mdoc",
                        meta = CredentialMeta(doctypeValue = "org.iso.18013.5.1.mDL"),
                        claims = listOf(
                            RegisteredClaim(path = listOf("org.iso.18013.5.1", "family_name")),
                        ),
                    ),
                ),
            )

            val result = evaluator.evaluate(
                registration = registration,
                accessCertificate = certificateWithOrgId("ORG-123"),
                requestedAttestations = listOf(
                    RequestedAttestationInfo(
                        format = "mso_mdoc",
                        docType = "org.iso.18013.5.1.mDL",
                        claimPaths = listOf(
                            listOf("org.iso.18013.5.1", "family_name"),
                            listOf("org.iso.18013.5.1", "portrait"),
                        ),
                    ),
                ),
            )

            val verified = result as WrpRegistrationResult.Verified
            assertEquals(
                listOf(
                    OverAskedClaim(
                        format = "mso_mdoc",
                        meta = CredentialMeta(doctypeValue = "org.iso.18013.5.1.mDL"),
                        path = listOf("org.iso.18013.5.1", "portrait"),
                    ),
                ),
                verified.overAskedClaims,
            )
        }
}

private fun registration(identifier: String): RegistrationCertificate =
    RegistrationCertificate(
        identifiers = listOf(RegistrationIdentifier(value = identifier)),
        name = "Nordic Bank",
        status = StatusReference(uri = "https://issuer.example/status/1", index = StatusIndex(5)),
    )

private fun certificateWithOrgId(orgId: String): X509Certificate =
    selfSignedCertificate(
        X500NameBuilder(BCStyle.INSTANCE)
            .addRDN(BCStyle.CN, "Test Relying Party")
            .addRDN(BCStyle.ORGANIZATION_IDENTIFIER, orgId)
            .build(),
    )

private fun certificateWithSerialNumber(serialNumber: String): X509Certificate =
    selfSignedCertificate(
        X500NameBuilder(BCStyle.INSTANCE)
            .addRDN(BCStyle.CN, "Test Natural Person Relying Party")
            .addRDN(BCStyle.SERIALNUMBER, serialNumber)
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
