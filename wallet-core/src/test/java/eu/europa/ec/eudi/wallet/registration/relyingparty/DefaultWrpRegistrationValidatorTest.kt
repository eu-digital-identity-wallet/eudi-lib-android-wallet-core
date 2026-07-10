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
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultWrpRegistrationValidatorTest {

    private val authenticator = mockk<WrprcAuthenticator>()
    private val evaluator = mockk<WrpRegistrationEvaluator>()
    private val validator = DefaultWrpRegistrationValidator(authenticator, evaluator)

    @Test
    fun `an absent certificate is rejected without authenticating anything`() = runTest {
        // an absent certificate is rejected (thrown)
        val error = runCatching {
            validator.validate(
                registrationCertificate = null,
                readerAccessChain = emptyList(),
                requestedAttestations = emptyList(),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        coVerify(exactly = 0) { authenticator.authenticate(any()) }
        coVerify(exactly = 0) { evaluator.evaluate(any(), any(), any()) }
    }

    @Test
    fun `an authentic certificate is evaluated and its result returned`() = runTest {
        val registration = RegistrationCertificate(name = "Nordic Bank")
        val evaluation = WrpRegistrationResult.Verified(registration = registration)
        coEvery { authenticator.authenticate(any()) } returns WrprcAuthentication.Authentic(registration)
        coEvery { evaluator.evaluate(registration, any(), any()) } returns evaluation

        val requested = listOf(RequestedAttestationInfo(format = "mso_mdoc"))
        val result = validator.validate(
            registrationCertificate = byteArrayOf(1, 2, 3),
            readerAccessChain = emptyList(),
            requestedAttestations = requested,
        )

        assertSame(evaluation, result)
        coVerify { evaluator.evaluate(registration, null, requested) }
    }

    @Test
    fun `an inauthentic certificate is rejected without being evaluated`() = runTest {
        // an inauthentic certificate is rejected (thrown)
        coEvery { authenticator.authenticate(any()) } returns
            WrprcAuthentication.Invalid(RegistrationFailureReason.SIGNATURE_INVALID)

        val error = runCatching {
            validator.validate(
                registrationCertificate = byteArrayOf(9),
                readerAccessChain = emptyList(),
                requestedAttestations = emptyList(),
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        coVerify(exactly = 0) { evaluator.evaluate(any(), any(), any()) }
    }
}
