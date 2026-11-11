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

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.wallet.logging.Logger
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class ReaderAuthenticationConfigBuilderTest {

    private val logger = Logger { }

    @Test
    fun `doNotEnforce keeps the configured trust source`() {
        val trustStore = mockk<ReaderTrustStore>()

        val policy = ReaderAuthenticationConfigBuilder().apply {
            trustSource(trustStore)
            doNotEnforce()
        }.build(etsiSource = null, logger = logger)

        assertEquals(ReaderAuthPolicy.DoNotEnforce(trustStore), policy)
    }

    @Test
    fun `doNotEnforce without a trust source has no trust store`() {
        val policy = ReaderAuthenticationConfigBuilder().apply {
            doNotEnforce()
        }.build(etsiSource = null, logger = logger)

        assertEquals(ReaderAuthPolicy.DoNotEnforce(), policy)
    }
}
