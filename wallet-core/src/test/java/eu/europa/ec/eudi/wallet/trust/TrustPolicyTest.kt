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

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import eu.europa.ec.eudi.wallet.trust.TrustPolicy.Action.ENFORCE
import eu.europa.ec.eudi.wallet.trust.TrustPolicy.Action.INFORM
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrustPolicyTest {

    @Test
    fun uniformPolicyReturnsSameActionForAnyInput() {
        val policy = TrustPolicy.uniform(ENFORCE)

        assertEquals(ENFORCE, policy.resolve(AttestationIdentifier.MDoc("org.iso.18013.5.1.mDL"), VerificationContext.PID))
        assertEquals(ENFORCE, policy.resolve(AttestationIdentifier.SDJwtVc("urn:example:vct"), VerificationContext.QEAA))
        assertEquals(ENFORCE, policy.resolve(AttestationIdentifier.MDoc("any"), null))
    }

    @Test
    fun builderDefaultIsUsedWhenNoSpecificMatch() {
        val policy = TrustPolicy.build {
            default(INFORM)
        }

        assertEquals(INFORM, policy.resolve(AttestationIdentifier.MDoc("org.iso.18013.5.1.mDL"), VerificationContext.PID))
        assertEquals(INFORM, policy.resolve(AttestationIdentifier.SDJwtVc("urn:example:vct"), null))
    }

    @Test
    fun forContextOverridesDefault() {
        val policy = TrustPolicy.build {
            default(ENFORCE)
            forContext(VerificationContext.PID, INFORM)
        }

        assertEquals(INFORM, policy.resolve(AttestationIdentifier.MDoc("any"), VerificationContext.PID))
        assertEquals(ENFORCE, policy.resolve(AttestationIdentifier.MDoc("any"), VerificationContext.QEAA))
    }

    @Test
    fun forAttestationOverridesForContext() {
        val mdoc = AttestationIdentifier.MDoc("org.iso.18013.5.1.mDL")
        val policy = TrustPolicy.build {
            default(ENFORCE)
            forContext(VerificationContext.PID, ENFORCE)
            forAttestation(mdoc, INFORM)
        }

        assertEquals(INFORM, policy.resolve(mdoc, VerificationContext.PID))
    }

    @Test
    fun forDocTypeCreatesAttestationOverride() {
        val policy = TrustPolicy.build {
            default(ENFORCE)
            forDocType("org.iso.18013.5.1.mDL", INFORM)
        }

        assertEquals(INFORM, policy.resolve(AttestationIdentifier.MDoc("org.iso.18013.5.1.mDL"), VerificationContext.PID))
        assertEquals(ENFORCE, policy.resolve(AttestationIdentifier.MDoc("other"), VerificationContext.PID))
    }

    @Test
    fun forVctCreatesAttestationOverride() {
        val policy = TrustPolicy.build {
            default(ENFORCE)
            forVct("urn:eu:pid", INFORM)
        }

        assertEquals(INFORM, policy.resolve(AttestationIdentifier.SDJwtVc("urn:eu:pid"), VerificationContext.PID))
        assertEquals(ENFORCE, policy.resolve(AttestationIdentifier.SDJwtVc("other"), VerificationContext.PID))
    }

    @Test
    fun nullVerificationContextFallsToDefault() {
        val policy = TrustPolicy.build {
            default(ENFORCE)
            forContext(VerificationContext.PID, INFORM)
        }

        assertEquals(ENFORCE, policy.resolve(AttestationIdentifier.MDoc("any"), null))
    }

    @Test
    fun customFunInterfaceImplementationWorks() {
        val policy = TrustPolicy { _, _ -> INFORM }

        assertEquals(INFORM, policy.resolve(AttestationIdentifier.MDoc("any"), VerificationContext.PID))
        assertEquals(INFORM, policy.resolve(AttestationIdentifier.SDJwtVc("any"), null))
    }
}
