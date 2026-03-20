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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class IssuanceMetadataTest {

    private fun createMetadata(
        credentialIssuerId: String = "https://issuer.example.com",
        credentialConfigurationIdentifier: String = "org.iso.18013.5.1.mDL",
        credentialEndpoint: String = "https://issuer.example.com/credential",
        tokenEndpoint: String = "https://auth.example.com/token",
        authorizationServerId: String = "https://auth.example.com",
        challengeEndpoint: String? = null,
        clientId: String = "wallet-client",
        clientAttestationJwt: String? = null,
        clientAttestationPopKeyId: String? = null,
        popKeyAliases: List<String> = listOf("key-alias-1"),
        dPoPKeyAlias: String? = null,
        accessToken: String = "access-token-123",
        accessTokenType: String = "DPoP",
        refreshToken: String? = "refresh-token-456",
        tokenTimestamp: Long = 1700000000L,
        grantType: String = "authorization_code",
    ) = IssuanceMetadata(
        credentialIssuerId = credentialIssuerId,
        credentialConfigurationIdentifier = credentialConfigurationIdentifier,
        credentialEndpoint = credentialEndpoint,
        tokenEndpoint = tokenEndpoint,
        authorizationServerId = authorizationServerId,
        challengeEndpoint = challengeEndpoint,
        clientId = clientId,
        clientAttestationJwt = clientAttestationJwt,
        clientAttestationPopKeyId = clientAttestationPopKeyId,
        popKeyAliases = popKeyAliases,
        dPoPKeyAlias = dPoPKeyAlias,
        accessToken = accessToken,
        accessTokenType = accessTokenType,
        refreshToken = refreshToken,
        tokenTimestamp = tokenTimestamp,
        grantType = grantType,
    )

    @Test
    fun `toByteArray and fromByteArray round-trip preserves all fields`() {
        val original = createMetadata(
            challengeEndpoint = "https://auth.example.com/challenge",
            clientAttestationJwt = "eyJhbGciOiJFUzI1NiJ9.test",
            clientAttestationPopKeyId = "pop-key-1",
            dPoPKeyAlias = "dpop-key-alias",
            popKeyAliases = listOf("key-1", "key-2"),
        )

        val bytes = original.toByteArray()
        val restored = IssuanceMetadata.fromByteArray(bytes)

        assertEquals(original, restored)
    }

    @Test
    fun `toByteArray and fromByteArray round-trip with null optional fields`() {
        val original = createMetadata(
            challengeEndpoint = null,
            clientAttestationJwt = null,
            clientAttestationPopKeyId = null,
            dPoPKeyAlias = null,
            refreshToken = null,
        )

        val bytes = original.toByteArray()
        val restored = IssuanceMetadata.fromByteArray(bytes)

        assertEquals(original, restored)
        assertNull(restored.challengeEndpoint)
        assertNull(restored.clientAttestationJwt)
        assertNull(restored.clientAttestationPopKeyId)
        assertNull(restored.dPoPKeyAlias)
        assertNull(restored.refreshToken)
    }

    @Test
    fun `toByteArray and fromByteArray with Bearer token type`() {
        val original = createMetadata(accessTokenType = "Bearer")

        val bytes = original.toByteArray()
        val restored = IssuanceMetadata.fromByteArray(bytes)

        assertEquals("Bearer", restored.accessTokenType)
        assertEquals(original, restored)
    }

    @Test
    fun `toByteArray and fromByteArray with pre-authorized grant type`() {
        val original = createMetadata(grantType = "pre-authorized_code")

        val bytes = original.toByteArray()
        val restored = IssuanceMetadata.fromByteArray(bytes)

        assertEquals("pre-authorized_code", restored.grantType)
        assertEquals(original, restored)
    }

    @Test
    fun `fromByteArray with invalid bytes throws exception`() {
        val invalidBytes = "not valid json".toByteArray()

        assertFailsWith<Exception> {
            IssuanceMetadata.fromByteArray(invalidBytes)
        }
    }

    @Test
    fun `STORAGE_TABLE_SPEC has correct table name`() {
        assertEquals("issuance_metadata", IssuanceMetadata.STORAGE_TABLE_SPEC.name)
    }
}
