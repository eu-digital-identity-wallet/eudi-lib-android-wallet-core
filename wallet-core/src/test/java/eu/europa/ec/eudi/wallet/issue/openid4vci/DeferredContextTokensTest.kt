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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.wallet.internal.crypto.DataSealer
import eu.europa.ec.eudi.wallet.internal.crypto.SealedDataUnreadableException
import eu.europa.ec.eudi.wallet.internal.crypto.SoftwareAeadCipher
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.StoredDeferredContext
import eu.europa.ec.eudi.wallet.issue.openid4vci.tokens.UndecodableTokenRecordException
import eu.europa.ec.eudi.wallet.issue.openid4vci.tokens.tokenRecordJson
import io.mockk.mockk
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class DeferredContextTokensTest {

    private val tokenSealer = DataSealer(SoftwareAeadCipher())

    // A context written before the tokens were encrypted
    private val plainContext = """
        {"credentialIssuerId":"https://issuer.example.com","deferredEndpoint":"https://issuer.example.com/deferred","tokenEndpoint":"https://auth.example.com/token","authorizationServerId":"https://auth.example.com","clientId":"wallet-client","popKeyAliases":["key-1"],"transactionId":"tx-secret","accessToken":"at-secret","accessTokenType":"Bearer","refreshToken":"rt-secret"}
    """.trimIndent().toByteArray()

    @Test
    fun `a stored context has no token or transaction id in plain text`() {
        val bytes = read(plainContext).toBytes("doc", tokenSealer)

        val stored = bytes.toString(Charsets.UTF_8)
        listOf("tx-secret", "at-secret", "rt-secret").forEach { assertFalse(it in stored) }
        val transaction = read(bytes).issuanceContext.authorizedTransaction
        assertEquals("tx-secret", transaction.transactionId.value)
        assertEquals("at-secret", transaction.authorizedRequest.accessToken.accessToken)
        assertEquals("rt-secret", transaction.authorizedRequest.refreshToken?.refreshToken)
    }

    @Test
    fun `a stored context is not read as the context of another document`() {
        val bytes = read(plainContext).toBytes("doc", tokenSealer)

        assertFailsWith<SealedDataUnreadableException> { read(bytes, documentId = "other-doc") }
    }

    @Test
    fun `a stored context with a changed field that its tokens are bound to is not read`() {
        val stored = tokenRecordJson.parseToJsonElement(
            read(plainContext).toBytes("doc", tokenSealer).toString(Charsets.UTF_8)
        ).jsonObject

        (stored.keys - StoredDeferredContext.UNBOUND_FIELDS).forEach { field ->
            val otherValue = if (stored[field] is JsonArray) JsonArray(listOf(JsonPrimitive("other-key")))
            else JsonPrimitive("https://other.example.com")
            val changed = JsonObject(stored + (field to otherValue))

            assertFailsWith<SealedDataUnreadableException>(field) { read(changed.toString().toByteArray()) }
        }
    }

    @Test
    fun `a context that cannot be decoded is reported without its tokens`() {
        // The field after the access token has a wrong type
        val altered = plainContext.toString(Charsets.UTF_8).replace("\"accessTokenType\":\"Bearer\"", "\"accessTokenType\":5")

        val error = kotlin.test.assertFails { read(altered.toByteArray()) }

        generateSequence(error) { it.cause }.forEach { cause ->
            listOf("tx-secret", "at-secret", "rt-secret").forEach { assertFalse(it in cause.message.orEmpty()) }
        }
    }

    @Test
    fun `a stored context with both encrypted and plain tokens is not read`() {
        val stored = tokenRecordJson.parseToJsonElement(
            read(plainContext).toBytes("doc", tokenSealer).toString(Charsets.UTF_8)
        ).jsonObject
        val mixed = JsonObject(stored + ("accessToken" to JsonPrimitive("at-other")))

        assertFailsWith<UndecodableTokenRecordException> { read(mixed.toString().toByteArray()) }
    }

    private fun read(bytes: ByteArray, documentId: String = "doc") = DeferredContext.fromBytes(
        bytes,
        documentId = documentId,
        tokenSealer = tokenSealer,
        walletKeyManager = mockk(),
        walletInstanceAttestationProvider = null,
    )
}
