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

import eu.europa.ec.eudi.openid4vci.RegistrationCertificatePolicy.Authorization
import eu.europa.ec.eudi.wallet.registration.OverProvidedAttestation
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class IssuerRegistrationCertificatePolicyFactoryTest {

    @Test
    fun `every failed validation denies the issuance`() = runTest {
        RegistrationFailureReason.entries.forEach { reason ->
            val authorization = authorize(RegistrationCertificateResult.Failed(reason))

            assertIs<Authorization.NotGranted>(authorization, "reason $reason")
        }
    }

    @Test
    fun `offered attestations outside the registered scope deny the issuance`() = runTest {
        val authorization = authorize(
            RegistrationCertificateResult.Verified(
                registration = RegistrationCertificate(),
                overProvidedAttestations = listOf(OverProvidedAttestation(format = "mso_mdoc")),
            )
        )

        assertIs<Authorization.NotGranted>(authorization)
    }

    @Test
    fun `a verified registration within its scope grants the issuance`() = runTest {
        val authorization = authorize(
            RegistrationCertificateResult.Verified(registration = RegistrationCertificate())
        )

        assertIs<Authorization.Granted>(authorization)
    }

    /** Applies the policy to an outcome that was already evaluated for this issuance. */
    private suspend fun authorize(outcome: RegistrationCertificateResult): Authorization =
        issuerRegistrationCertificatePolicy(
            resolver = mockk(),
            resolvedRegistration = outcome,
            onEvaluated = {},
        ).invoke(
            accessCertificate = mockk(),
            registrationCertificate = "registration-certificate",
            issuanceContext = emptyList(),
        )
}
