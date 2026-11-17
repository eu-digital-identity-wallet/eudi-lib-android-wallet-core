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

package eu.europa.ec.eudi.wallet.dcapi.internal

import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.registry.provider.SelectedCredentialSet
import androidx.credentials.registry.provider.selectedCredentialSet
import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol.OPENID4VP_V1_SIGNED
import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol.OPENID4VP_V1_UNSIGNED
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalDigitalCredentialApi::class)
class ResolveDcApiRequestTest {

    private val supported = listOf(OPENID4VP_V1_SIGNED, OPENID4VP_V1_UNSIGNED)

    @Before
    fun setUp() = mockkStatic("androidx.credentials.registry.provider.ProviderGetCredentialRequest")

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `a selected protocol that is not supported fails instead of taking another entry`() {
        val request = request(
            selected = UNSUPPORTED,
            offered = listOf(OPENID4VP_V1_UNSIGNED.identifier, OPENID4VP_V1_SIGNED.identifier, UNSUPPORTED),
        )

        assertFailsWith<IllegalStateException> { request.resolveDcApiRequest(supported) }
    }

    @Test
    fun `a selected protocol that is not in requests fails instead of taking another entry`() {
        val request = request(selected = OPENID4VP_V1_SIGNED.identifier, offered = listOf(OPENID4VP_V1_UNSIGNED.identifier))

        assertFailsWith<IllegalStateException> { request.resolveDcApiRequest(supported) }
    }

    @Test
    fun `the entry of the selected protocol is processed`() {
        val request = request(
            selected = OPENID4VP_V1_SIGNED.identifier,
            offered = listOf(OPENID4VP_V1_UNSIGNED.identifier, OPENID4VP_V1_SIGNED.identifier),
        )

        assertEquals(ResolvedDcApiRequest(OPENID4VP_V1_SIGNED.identifier, 1), request.resolveDcApiRequest(supported))
    }

    private fun request(selected: String, offered: List<String>): ProviderGetCredentialRequest {
        val json = buildJsonObject {
            putJsonArray("requests") {
                offered.forEach { protocol ->
                    addJsonObject {
                        put("protocol", protocol)
                        put("data", buildJsonObject { })
                    }
                }
            }
        }.toString()
        val option = mockk<GetDigitalCredentialOption> { every { requestJson } returns json }
        val selectedSet = mockk<SelectedCredentialSet> { every { credentialSetId } returns "0 $selected" }
        val request = mockk<ProviderGetCredentialRequest> { every { credentialOptions } returns listOf(option) }
        every { request.selectedCredentialSet } returns selectedSet
        return request
    }

    private companion object {
        const val UNSUPPORTED = "org.example.unsupported"
    }
}
