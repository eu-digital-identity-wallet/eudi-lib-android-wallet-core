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

package eu.europa.ec.eudi.wallet.issue.openid4vci.tokens

import eu.europa.ec.eudi.openid4vci.AccessToken
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.Grant
import eu.europa.ec.eudi.openid4vci.RefreshToken
import eu.europa.ec.eudi.wallet.internal.crypto.AeadCipher
import eu.europa.ec.eudi.wallet.internal.crypto.DataSealer
import eu.europa.ec.eudi.wallet.internal.crypto.SealedDataUnreadableException
import eu.europa.ec.eudi.wallet.internal.crypto.SoftwareAeadCipher
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.IssuanceMetadata
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import java.security.ProviderException
import java.time.Instant
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.multipaz.storage.Storage
import org.multipaz.storage.ephemeral.EphemeralStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class IssuanceMetadataStoreTest {

    private val storage = EphemeralStorage()
    private val cipher = SoftwareAeadCipher()
    private val store = IssuanceMetadataStore(storage, DataSealer(cipher))

    @Test
    fun `a stored record has no token in plain text`() = runTest {
        store.insert("doc", metadata)

        assertNoPlainTokens("doc")
        assertEquals(metadata, store.get("doc"))
    }

    @Test
    fun `records written before the tokens were encrypted are encrypted when tokens are first read`() = runTest {
        storePlain("doc", metadata)
        storePlain("other-doc", metadata.copy(refreshToken = "rt-other"))

        assertEquals(metadata, store.get("doc"))

        assertNoPlainTokens("doc")
        assertNoPlainTokens("other-doc")
        assertEquals(metadata.copy(refreshToken = "rt-other"), store.get("other-doc"))
    }

    @Test
    fun `a record with a changed field that its tokens are bound to is not read`() = runTest {
        store.insert("doc", metadata)
        val table = storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
        val stored = tokenRecordJson.parseToJsonElement(table.get("doc")!!.toByteArray().toString(Charsets.UTF_8)).jsonObject

        (stored.keys - IssuanceMetadataStore.UNBOUND_FIELDS).forEach { field ->
            val value = stored[field]
            val otherValue = when {
                value is JsonArray -> JsonArray(listOf(JsonPrimitive("other-key")))
                value is JsonPrimitive && value !is JsonNull && !value.isString -> JsonPrimitive(1800000000L)
                else -> JsonPrimitive("other")
            }
            val changed = JsonObject(stored + (field to otherValue))
            table.update(key = "doc", data = ByteString(changed.toString().toByteArray()))

            assertFailsWith<SealedDataUnreadableException>(field) { store.get("doc") }
        }
    }

    @Test
    fun `a record with a field removed or added is not read`() = runTest {
        store.insert("doc", metadata)
        val stored = storedFields("doc")

        listOf(
            JsonObject(stored - "tokenEndpoint"),
            JsonObject(stored + ("addedField" to JsonPrimitive("added"))),
        ).forEach { changed ->
            replaceRecord("doc", changed)

            assertFailsWith<SealedDataUnreadableException> { store.get("doc") }
        }
    }

    @Test
    fun `an encrypted record that cannot be decoded is reported without its tokens`() = runTest {
        store.insert("doc", metadata)
        val table = storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
        // A field that the tokens are not bound to, with a wrong type: the record fails to decode once its
        // tokens are decrypted
        val altered = table.get("doc")!!.toByteArray().toString(Charsets.UTF_8)
            .replace("\"issuerName\":\"${metadata.issuerName}\"", "\"issuerName\":5")
        assertTrue("\"issuerName\":5" in altered)
        table.update(key = "doc", data = ByteString(altered.toByteArray()))

        val error = assertFailsWith<UndecodableTokenRecordException> { store.get("doc") }

        assertNull(error.cause)
        assertFalse(ACCESS_TOKEN in error.message.orEmpty() || REFRESH_TOKEN in error.message.orEmpty())
    }

    @Test
    fun `a record in plain text that cannot be decoded is reported without its tokens`() = runTest {
        // Records stored in plain text, each with a field of a wrong type near its tokens, since the errors
        // of the JSON library may quote the input around the error: one with its tokens at the end, read
        // with its tokens, and one with the wrong field just before the access token, read without them
        val plain = metadata.toByteArray().toString(Charsets.UTF_8)
        val fields = tokenRecordJson.parseToJsonElement(plain).jsonObject
        val tokens = fields.filterKeys { it == "accessToken" || it == "refreshToken" }
        val endingWithTokens = JsonObject(fields - tokens.keys + ("tokenTimestamp" to JsonPrimitive("soon")) + tokens)
        val table = storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
        table.insert(key = "doc", data = ByteString(endingWithTokens.toString().toByteArray()))
        table.insert(
            key = "other-doc",
            data = ByteString(plain.replace("\"dPoPKeyAlias\":\"${metadata.dPoPKeyAlias}\"", "\"dPoPKeyAlias\":5").toByteArray())
        )
        val logged = mutableListOf<Throwable>()
        val loggingStore = IssuanceMetadataStore(storage, DataSealer(SoftwareAeadCipher())) { record ->
            record.thrown?.let { logged += it }
        }

        val errors = listOf(
            assertFails { loggingStore.get("doc") },
            assertFails { loggingStore.getWithoutTokens("other-doc") },
        ) + logged

        assertTrue(logged.isNotEmpty())
        errors.flatMap { generateSequence(it) { error -> error.cause }.toList() }.forEach { cause ->
            assertFalse(ACCESS_TOKEN in cause.message.orEmpty() || REFRESH_TOKEN in cause.message.orEmpty(), cause.message)
        }
    }

    @Test
    fun `a record that cannot be decoded does not make the encryption of earlier records run again`() = runTest {
        val table = spyk(storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC))
        val countingStorage = mockk<Storage> { coEvery { getTable(any()) } returns table }
        table.insert(key = "unknown", data = ByteString("""{"unknown":true}""".toByteArray()))
        val countingStore = IssuanceMetadataStore(countingStorage, DataSealer(SoftwareAeadCipher()))

        countingStore.insert("doc", metadata)
        countingStore.get("doc")

        coVerify(exactly = 1) { table.enumerateWithData(any(), any(), any()) }
    }

    @Test
    fun `the fields that are not encrypted are read without the key`() = runTest {
        store.insert("doc", metadata)
        val storeWithAnotherKey = IssuanceMetadataStore(storage, DataSealer(SoftwareAeadCipher()))

        assertFailsWith<SealedDataUnreadableException> { storeWithAnotherKey.get("doc") }
        assertEquals(
            IssuanceMetadataWithoutTokens(
                credentialIssuerId = metadata.credentialIssuerId,
                credentialConfigurationIdentifier = metadata.credentialConfigurationIdentifier,
                dPoPKeyAlias = metadata.dPoPKeyAlias,
                issuerName = metadata.issuerName,
            ),
            storeWithAnotherKey.getWithoutTokens("doc")
        )
    }

    @Test
    fun `a record with both encrypted and plain tokens is not read`() = runTest {
        store.insert("doc", metadata)
        val stored = storedFields("doc")

        replaceRecord("doc", JsonObject(stored + ("accessToken" to JsonPrimitive("at-other"))))

        assertFailsWith<UndecodableTokenRecordException> { store.get("doc") }
    }

    @Test
    fun `encrypted tokens with a field that is not a token are not read`() = runTest {
        store.insert("doc", metadata)
        val stored = storedFields("doc")
        // Encrypted for this record, with a field that would replace one of the record
        val sealedTokens = DataSealer(cipher).seal(
            TokenRecord.ISSUANCE_METADATA,
            "doc",
            boundValues(stored, IssuanceMetadataStore.UNBOUND_FIELDS),
            """{"accessToken":"at-0","refreshToken":"rt-0","credentialIssuerId":"https://other.example.com"}""".toByteArray()
        )

        replaceRecord("doc", JsonObject(stored + ("sealedTokens" to JsonPrimitive(sealedTokens))))

        assertFailsWith<UndecodableTokenRecordException> { store.get("doc") }
    }

    @Test
    fun `records with an object as a bound field or as the encrypted tokens cannot be decoded and are not reported on a refresh`() = runTest {
        store.insert("doc", metadata)
        store.insert("other-doc", metadata)
        // A bound field that is an object, and encrypted tokens that are not a string
        replaceRecord("doc", JsonObject(storedFields("doc") + ("dPoPKeyAlias" to JsonObject(emptyMap()))))
        replaceRecord("other-doc", JsonObject(storedFields("other-doc") + ("sealedTokens" to JsonObject(emptyMap()))))
        val failures = mutableListOf<String>()

        listOf("doc", "other-doc").forEach { documentId ->
            assertFailsWith<UndecodableTokenRecordException>(documentId) { store.get(documentId) }
        }
        store.storeRefreshedTokens(REFRESH_TOKEN, refreshedRequest()) { documentId, _ -> failures += documentId }

        assertTrue(failures.isEmpty(), "Reported as failures that may be temporary: $failures")
    }

    @Test
    fun `records in plain text are encrypted at a later call when encrypting them failed`() = runTest {
        storePlain("doc", metadata)
        var keystoreAvailable = false
        val keystore = object : AeadCipher {
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray) =
                if (keystoreAvailable) cipher.encrypt(plaintext, associatedData)
                else throw ProviderException("Keystore operation failed")
            override fun decrypt(ivAndCiphertext: ByteArray, associatedData: ByteArray) =
                cipher.decrypt(ivAndCiphertext, associatedData)
        }
        val storeWithKeystore = IssuanceMetadataStore(storage, DataSealer(keystore))

        // Still read in plain text
        assertEquals(metadata, storeWithKeystore.get("doc"))
        keystoreAvailable = true
        storeWithKeystore.get("doc")

        assertNoPlainTokens("doc")
    }

    private fun refreshedRequest() = AuthorizedRequest(
        accessToken = AccessToken.DPoP("at-1", null),
        refreshToken = RefreshToken("rt-1"),
        credentialIdentifiers = emptyMap(),
        timestamp = Instant.ofEpochSecond(1800000000L),
        authorizationServerDpopNonce = null,
        resourceServerDpopNonce = null,
        grant = Grant.AuthorizationCode,
    )

    private suspend fun storedFields(documentId: String): JsonObject = tokenRecordJson.parseToJsonElement(
        storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC).get(documentId)!!.toByteArray().toString(Charsets.UTF_8)
    ).jsonObject

    private suspend fun replaceRecord(documentId: String, record: JsonObject) {
        storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
            .update(key = documentId, data = ByteString(record.toString().toByteArray()))
    }

    private suspend fun storePlain(documentId: String, metadata: IssuanceMetadata) {
        storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
            .insert(key = documentId, data = ByteString(metadata.toByteArray()))
    }

    private suspend fun assertNoPlainTokens(documentId: String) {
        val stored = storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC).get(documentId)!!
            .toByteArray().toString(Charsets.UTF_8)
        listOf(ACCESS_TOKEN, REFRESH_TOKEN, "rt-other").forEach { token ->
            assertFalse(token in stored, "The record of $documentId has a token in plain text")
        }
    }

    private companion object {
        const val ACCESS_TOKEN = "at-secret"
        const val REFRESH_TOKEN = "rt-secret"

        val metadata = IssuanceMetadata(
            credentialIssuerId = "https://issuer.example.com",
            credentialConfigurationIdentifier = "eu.europa.ec.eudi.pid_vc_sd_jwt",
            credentialEndpoint = "https://issuer.example.com/credential",
            tokenEndpoint = "https://auth.example.com/token",
            authorizationServerId = "https://auth.example.com",
            clientId = "wallet-client",
            popKeyAliases = listOf("key-1"),
            dPoPKeyAlias = "dpop-key",
            accessToken = ACCESS_TOKEN,
            accessTokenType = "DPoP",
            refreshToken = REFRESH_TOKEN,
            tokenTimestamp = 1700000000L,
            grantType = "authorization_code",
            issuerName = "Issuer",
        )
    }
}
