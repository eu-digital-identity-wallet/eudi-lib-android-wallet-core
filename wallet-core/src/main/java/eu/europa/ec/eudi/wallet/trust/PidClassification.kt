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
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifierPredicate

/**
 * The classification deciding which attestations are PIDs, as configured for trust-area routing
 * (`configureEtsiTrust { classifications(...) }`). It is the single source of truth for the question,
 * so checks that treat PIDs differently from other attestations agree with the trust evaluation.
 *
 * Absent configuration classifies nothing as a PID.
 */
internal val AttestationClassifications?.pidClassification: AttestationIdentifierPredicate
    get() = this?.pids ?: AttestationIdentifierPredicate.None
