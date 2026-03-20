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

package eu.europa.ec.eudi.wallet.issue.openid4vci.reissue

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StoredDeferredContextTest {

    @Test
    fun `serialization round-trip preserves all fields including reissuance metadata`() {
        val original = StoredDeferredContext(
            credentialIssuerId = "https://issuer.example.com",
            deferredEndpoint = "https://issuer.example.com/deferred",
            tokenEndpoint = "https://auth.example.com/token",
            authorizationServerId = "https://auth.example.com",
            challengeEndpoint = "https://auth.example.com/challenge",
            clientId = "wallet-client",
            popKeyAliases = listOf("key-1", "key-2"),
            dPoPKeyAlias = "dpop-key-alias",
            clientAttestationPopKeyId = "pop-key-1",
            clientAttestationJwt = "eyJhbGciOiJFUzI1NiJ9.test",
            transactionId = "tx-123",
            accessToken = "access-token",
            accessTokenType = "DPoP",
            refreshToken = "refresh-token",
            requestEncryptionKeyJwk = """{"kty":"EC","crv":"P-256"}""",
            requestEncryptionMethod = "A128CBC-HS256",
            responseEncryptionMethod = "A256GCM",
            credentialConfigurationIdentifier = "org.iso.18013.5.1.mDL",
            credentialEndpoint = "https://issuer.example.com/credential",
        )

        val json = Json.encodeToString(original)
        val restored = Json.decodeFromString<StoredDeferredContext>(json)

        assertEquals(original, restored)
    }

    @Test
    fun `serialization round-trip with null optional fields`() {
        val original = StoredDeferredContext(
            credentialIssuerId = "https://issuer.example.com",
            deferredEndpoint = "https://issuer.example.com/deferred",
            tokenEndpoint = "https://auth.example.com/token",
            authorizationServerId = "https://auth.example.com",
            clientId = "wallet-client",
            popKeyAliases = listOf("key-1"),
            transactionId = "tx-123",
            accessToken = "access-token",
        )

        val json = Json.encodeToString(original)
        val restored = Json.decodeFromString<StoredDeferredContext>(json)

        assertEquals(original, restored)
        assertNull(restored.challengeEndpoint)
        assertNull(restored.dPoPKeyAlias)
        assertNull(restored.clientAttestationPopKeyId)
        assertNull(restored.clientAttestationJwt)
        assertNull(restored.refreshToken)
        assertNull(restored.requestEncryptionKeyJwk)
        assertNull(restored.requestEncryptionMethod)
        assertNull(restored.responseEncryptionMethod)
        assertNull(restored.credentialConfigurationIdentifier)
        assertNull(restored.credentialEndpoint)
    }

    @Test
    fun `credentialConfigurationIdentifier and credentialEndpoint are preserved for deferred reissuance`() {
        val original = StoredDeferredContext(
            credentialIssuerId = "https://issuer.example.com",
            deferredEndpoint = "https://issuer.example.com/deferred",
            tokenEndpoint = "https://auth.example.com/token",
            authorizationServerId = "https://auth.example.com",
            clientId = "wallet-client",
            popKeyAliases = listOf("key-1"),
            transactionId = "tx-123",
            accessToken = "access-token",
            credentialConfigurationIdentifier = "eu.europa.ec.eudi.pid_mso_mdoc",
            credentialEndpoint = "https://issuer.example.com/credential",
        )

        val json = Json.encodeToString(original)
        val restored = Json.decodeFromString<StoredDeferredContext>(json)

        assertEquals("eu.europa.ec.eudi.pid_mso_mdoc", restored.credentialConfigurationIdentifier)
        assertEquals("https://issuer.example.com/credential", restored.credentialEndpoint)
    }

    @Test
    fun `default accessTokenType is DPoP`() {
        val context = StoredDeferredContext(
            credentialIssuerId = "https://issuer.example.com",
            deferredEndpoint = "https://issuer.example.com/deferred",
            tokenEndpoint = "https://auth.example.com/token",
            authorizationServerId = "https://auth.example.com",
            clientId = "wallet-client",
            popKeyAliases = listOf("key-1"),
            transactionId = "tx-123",
            accessToken = "access-token",
        )

        assertEquals("DPoP", context.accessTokenType)
    }
}
