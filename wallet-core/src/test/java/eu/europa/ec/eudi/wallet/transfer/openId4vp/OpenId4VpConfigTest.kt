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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig.EncryptionPolicy
import org.junit.Test
import java.net.URI
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [OpenId4VpConfig.EncryptionPolicy] and its Builder wiring.
 */
class OpenId4VpConfigTest {

    @Test
    fun `HAIP accepts DirectPostJwt`() {
        EncryptionPolicy.HAIP.enforce(
            ResponseMode.DirectPostJwt(URL("https://verifier.example/cb"))
        )
    }

    @Test
    fun `HAIP accepts DCApiJwt`() {
        EncryptionPolicy.HAIP.enforce(ResponseMode.DCApiJwt)
    }

    @Test
    fun `HAIP rejects DirectPost`() {
        assertFailsWith<IllegalArgumentException> {
            EncryptionPolicy.HAIP.enforce(
                ResponseMode.DirectPost(URL("https://verifier.example/cb"))
            )
        }
    }

    @Test
    fun `HAIP rejects Fragment`() {
        assertFailsWith<IllegalArgumentException> {
            EncryptionPolicy.HAIP.enforce(
                ResponseMode.Fragment(URI.create("https://verifier.example/cb"))
            )
        }
    }

    @Test
    fun `HAIP rejects FragmentJwt`() {
        assertFailsWith<IllegalArgumentException> {
            EncryptionPolicy.HAIP.enforce(
                ResponseMode.FragmentJwt(URI.create("https://verifier.example/cb"))
            )
        }
    }

    @Test
    fun `HAIP rejects Query`() {
        assertFailsWith<IllegalArgumentException> {
            EncryptionPolicy.HAIP.enforce(
                ResponseMode.Query(URI.create("https://verifier.example/cb"))
            )
        }
    }

    @Test
    fun `HAIP rejects QueryJwt`() {
        assertFailsWith<IllegalArgumentException> {
            EncryptionPolicy.HAIP.enforce(
                ResponseMode.QueryJwt(URI.create("https://verifier.example/cb"))
            )
        }
    }

    @Test
    fun `HAIP rejects DCApi`() {
        assertFailsWith<IllegalArgumentException> {
            EncryptionPolicy.HAIP.enforce(ResponseMode.DCApi)
        }
    }

    @Test
    fun `HAIP rejection message names the offending response mode`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            EncryptionPolicy.HAIP.enforce(ResponseMode.DCApi)
        }
        val msg = assertNotNull(ex.message, "rejection must carry a message")
        assertTrue(
            msg.contains("DCApi"),
            "message must mention the offending mode; was: '$msg'"
        )
    }

    @Test
    fun `Builder default encryptionPolicy is HAIP`() {
        val config = defaultBuilder().build()
        assertEquals(EncryptionPolicy.HAIP, config.encryptionPolicy)
    }

    @Test
    fun `Builder withEncryptionPolicy overrides the default`() {
        var seenMode: ResponseMode? = null
        val custom = EncryptionPolicy { mode -> seenMode = mode }

        val config = defaultBuilder()
            .withEncryptionPolicy(custom)
            .build()

        assertEquals(custom, config.encryptionPolicy)

        // Confirm the custom policy is the one actually invoked.
        config.encryptionPolicy.enforce(ResponseMode.DCApi)
        assertEquals(ResponseMode.DCApi, seenMode)
    }

    private fun defaultBuilder(): OpenId4VpConfig.Builder = OpenId4VpConfig.Builder()
        .withClientIdSchemes(ClientIdScheme.X509SanDns)
        .withFormats(Format.MsoMdoc.ES256, Format.SdJwtVc.ES256)
}
