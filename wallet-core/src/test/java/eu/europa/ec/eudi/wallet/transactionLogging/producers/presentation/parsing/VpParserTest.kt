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

package eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation.parsing

import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimPath
import org.junit.Test
import kotlin.test.assertEquals

class VpParserTest {

    @Test
    fun `parses an SD-JWT VC presentation into the disclosed attributes only`() {
        // The fixture holds two presentations: an SD-JWT VC PID and an mdoc.
        val result = parseVp(getResourceAsByteArray("vp_response_2.json"))

        val pid = result.single { it.credentialIdentifier == "urn:eu.europa.ec.eudi:pid:1" }

        // The recreated payload also carries iss, iat, exp, vct, _sd_alg, status.* and cnf.jwk.*;
        // none of those is an attribute the User presented (TS10 §3.2).
        assertEquals(
            listOf(
                ClaimPath.ofKeys("age_equal_or_over", "18"),
                ClaimPath.key("family_name"),
                ClaimPath.key("given_name"),
            ),
            pid.claims.sortedBy { path -> path.segments.joinToString(".") { it.toString() } },
        )
    }
}
