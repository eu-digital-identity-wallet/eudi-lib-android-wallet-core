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

import eu.europa.ec.eudi.openid4vci.AccessToken
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReissuanceIssuerTest {

    private val reissuanceIssuer = ReissuanceIssuer()

    private fun createMetadata(
        accessTokenType: String = "DPoP",
        accessToken: String = "access-token-123",
        refreshToken: String? = "refresh-token-456",
        tokenTimestamp: Long = 1700000000L,
    ) = IssuanceMetadata(
        credentialIssuerId = "https://issuer.example.com",
        credentialConfigurationIdentifier = "org.iso.18013.5.1.mDL",
        credentialEndpoint = "https://issuer.example.com/credential",
        tokenEndpoint = "https://auth.example.com/token",
        authorizationServerId = "https://auth.example.com",
        clientId = "wallet-client",
        popKeyAliases = listOf("key-alias-1"),
        accessToken = accessToken,
        accessTokenType = accessTokenType,
        refreshToken = refreshToken,
        tokenTimestamp = tokenTimestamp,
        grantType = "authorization_code",
    )

    @Test
    fun `reconstructAuthorizedRequest creates DPoP access token when type is DPoP`() {
        val metadata = createMetadata(accessTokenType = "DPoP", accessToken = "dpop-token")

        val result = reissuanceIssuer.reconstructAuthorizedRequest(metadata)

        assertIs<AccessToken.DPoP>(result.accessToken)
        assertEquals("dpop-token", result.accessToken.accessToken)
    }

    @Test
    fun `reconstructAuthorizedRequest creates Bearer access token when type is Bearer`() {
        val metadata = createMetadata(accessTokenType = "Bearer", accessToken = "bearer-token")

        val result = reissuanceIssuer.reconstructAuthorizedRequest(metadata)

        assertIs<AccessToken.Bearer>(result.accessToken)
        assertEquals("bearer-token", result.accessToken.accessToken)
    }

    @Test
    fun `reconstructAuthorizedRequest includes refresh token when present`() {
        val metadata = createMetadata(refreshToken = "my-refresh-token")

        val result = reissuanceIssuer.reconstructAuthorizedRequest(metadata)

        assertNotNull(result.refreshToken)
        assertEquals("my-refresh-token", result.refreshToken!!.refreshToken)
    }

    @Test
    fun `reconstructAuthorizedRequest has null refresh token when not present`() {
        val metadata = createMetadata(refreshToken = null)

        val result = reissuanceIssuer.reconstructAuthorizedRequest(metadata)

        assertNull(result.refreshToken)
    }

    @Test
    fun `reconstructAuthorizedRequest preserves token timestamp`() {
        val timestamp = 1700000000L
        val metadata = createMetadata(tokenTimestamp = timestamp)

        val result = reissuanceIssuer.reconstructAuthorizedRequest(metadata)

        assertEquals(Instant.ofEpochSecond(timestamp), result.timestamp)
    }

    @Test
    fun `reconstructAuthorizedRequest sets empty credential identifiers`() {
        val metadata = createMetadata()

        val result = reissuanceIssuer.reconstructAuthorizedRequest(metadata)

        assertEquals(emptyMap(), result.credentialIdentifiers)
    }

    @Test
    fun `reconstructAuthorizedRequest throws for unknown access token type`() {
        val metadata = createMetadata(accessTokenType = "Unknown")

        assertFailsWith<IllegalStateException> {
            reissuanceIssuer.reconstructAuthorizedRequest(metadata)
        }
    }
}
