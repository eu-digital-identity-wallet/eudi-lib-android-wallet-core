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

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationClassifications
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifierPredicate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PidClassificationTest {

    private val pid = AttestationIdentifier.MDoc("eu.europa.ec.eudi.pid.1")
    private val mdl = AttestationIdentifier.MDoc("org.iso.18013.5.1.mDL")

    @Test
    fun `the configured pid classification is used`() {
        val classifications = AttestationClassifications(
            pids = AttestationIdentifierPredicate.any(setOf(pid)),
        )

        assertTrue(classifications.pidClassification.test(pid))
        assertFalse(classifications.pidClassification.test(mdl))
    }

    @Test
    fun `an unconfigured classification classifies nothing as a pid`() {
        val classifications: AttestationClassifications? = null

        assertFalse(classifications.pidClassification.test(pid))
    }

    @Test
    fun `a classification configured without pids classifies nothing as a pid`() {
        val classifications = AttestationClassifications(
            qEAAs = AttestationIdentifierPredicate.any(setOf(mdl)),
        )

        assertFalse(classifications.pidClassification.test(pid))
        assertFalse(classifications.pidClassification.test(mdl))
    }
}
