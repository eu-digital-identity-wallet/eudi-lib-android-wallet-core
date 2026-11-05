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
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.Grant
import eu.europa.ec.eudi.openid4vci.RefreshToken
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.ByteString
import org.multipaz.storage.Storage
import org.multipaz.storage.ephemeral.EphemeralStorage
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefreshedTokensTest {

    @Test
    fun `refreshed tokens replace the old ones in every document holding the old refresh token`() = runTest {
        val storage = EphemeralStorage()
        storage.put("mdoc", metadata(refreshToken = "rt-0"))
        storage.put("sd-jwt", metadata(refreshToken = "rt-0"))

        val updated = storage.storeRefreshedTokens("rt-0", refreshed(accessToken = "at-1", refreshToken = "rt-1"))

        assertEquals(listOf("mdoc", "sd-jwt"), updated.sorted())
        listOf("mdoc", "sd-jwt").forEach { documentId ->
            val stored = storage.load(documentId)
            assertEquals("at-1", stored.accessToken)
            assertEquals("rt-1", stored.refreshToken)
            assertEquals(1800000000L, stored.tokenTimestamp)
        }
    }

    @Test
    fun `documents of another authorization are left untouched`() = runTest {
        val storage = EphemeralStorage()
        storage.put("pid", metadata(refreshToken = "rt-0"))
        storage.put("mdl", metadata(refreshToken = "other-rt"))

        val updated = storage.storeRefreshedTokens("rt-0", refreshed(accessToken = "at-1", refreshToken = "rt-1"))

        assertEquals(listOf("pid"), updated)
        assertEquals(metadata(refreshToken = "other-rt"), storage.load("mdl"))
    }

    @Test
    fun `the refresh token is kept when the refresh does not rotate it`() = runTest {
        val storage = EphemeralStorage()
        storage.put("pid", metadata(refreshToken = "rt-0"))

        storage.storeRefreshedTokens("rt-0", refreshed(accessToken = "at-1", refreshToken = null))

        val stored = storage.load("pid")
        assertEquals("at-1", stored.accessToken)
        assertEquals("rt-0", stored.refreshToken)
    }

    @Test
    fun `the access token type follows the refreshed token`() = runTest {
        val storage = EphemeralStorage()
        storage.put("pid", metadata(refreshToken = "rt-0"))

        storage.storeRefreshedTokens(
            "rt-0",
            refreshed(accessToken = "at-1", refreshToken = "rt-1", bearer = true),
        )

        assertEquals("Bearer", storage.load("pid").accessTokenType)
    }

    @Test
    fun `an unreadable record does not stop the others from being updated`() = runTest {
        val storage = EphemeralStorage()
        storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
            .insert(key = "broken", data = ByteString("not json".toByteArray()))
        storage.put("pid", metadata(refreshToken = "rt-0"))

        val updated = storage.storeRefreshedTokens("rt-0", refreshed(accessToken = "at-1", refreshToken = "rt-1"))

        assertEquals(listOf("pid"), updated)
        assertEquals("rt-1", storage.load("pid").refreshToken)
    }

    @Test
    fun `nothing is updated when no document holds the refresh token`() = runTest {
        val storage = EphemeralStorage()
        storage.put("pid", metadata(refreshToken = "other-rt"))

        val updated = storage.storeRefreshedTokens("rt-0", refreshed(accessToken = "at-1", refreshToken = "rt-1"))

        assertTrue(updated.isEmpty())
    }

    private suspend fun Storage.put(documentId: String, metadata: IssuanceMetadata) {
        getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
            .insert(key = documentId, data = ByteString(metadata.toByteArray()))
    }

    private suspend fun Storage.load(documentId: String): IssuanceMetadata =
        IssuanceMetadata.fromByteArray(
            getTable(IssuanceMetadata.STORAGE_TABLE_SPEC).get(documentId)!!.toByteArray()
        )

    private fun metadata(refreshToken: String?) = IssuanceMetadata(
        credentialIssuerId = "https://issuer.example.com",
        credentialConfigurationIdentifier = "eu.europa.ec.eudi.pid_mso_mdoc",
        credentialEndpoint = "https://issuer.example.com/credential",
        tokenEndpoint = "https://auth.example.com/token",
        authorizationServerId = "https://auth.example.com",
        clientId = "wallet-client",
        popKeyAliases = listOf("key-alias-1"),
        dPoPKeyAlias = "dpop-key",
        accessToken = "at-0",
        accessTokenType = "DPoP",
        refreshToken = refreshToken,
        tokenTimestamp = 1700000000L,
        grantType = "authorization_code",
    )

    private fun refreshed(accessToken: String, refreshToken: String?, bearer: Boolean = false) = AuthorizedRequest(
        accessToken = if (bearer) AccessToken.Bearer(accessToken, null) else AccessToken.DPoP(accessToken, null),
        refreshToken = refreshToken?.let { RefreshToken(it) },
        credentialIdentifiers = emptyMap(),
        timestamp = Instant.ofEpochSecond(1800000000L),
        authorizationServerDpopNonce = null,
        resourceServerDpopNonce = null,
        grant = Grant.AuthorizationCode,
    )
}
