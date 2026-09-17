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

package eu.europa.ec.eudi.wallet.internal

import eu.europa.ec.eudi.openid4vp.HashAlgorithm
import eu.europa.ec.eudi.openid4vp.ResolutionError
import eu.europa.ec.eudi.openid4vp.SupportedTransactionDataType
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderTrust
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequestException
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.RawTransactionType
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.TransactionDataKeyBinding
import eu.europa.ec.eudi.wallet.transfer.openId4vp.TransactionDataType
import io.mockk.mockk
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.multipaz.crypto.Algorithm
import org.multipaz.documenttype.TransactionType
import org.multipaz.presentment.TransactionData as ParsedTransactionData
import org.multipaz.presentment.TransactionProtocol
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the conversion of the configured transaction data types to the types supported by the
 * OpenID4VP library, and for their presence in the library configuration.
 */
class OpenId4VpTransactionDataTypesTest {

    @Test
    fun `an empty configuration produces no supported types`() {
        assertTrue(emptyList<TransactionDataType>().toSupportedTransactionDataTypes().isEmpty())
    }

    @Test
    fun `a configured type keeps its identifier and is advertised with sha-256`() {
        val supported = listOf(TransactionDataType.QES_APPROVAL)
            .toSupportedTransactionDataTypes()

        assertEquals(1, supported.size)
        val sdJwtVc = assertIs<SupportedTransactionDataType.SdJwtVc>(supported.single())
        assertEquals(TransactionDataType.QES_APPROVAL.value, sdJwtVc.type.value)
        assertEquals(setOf(HashAlgorithm.SHA_256), sdJwtVc.hashAlgorithms)
    }

    @Test
    fun `every configured type is converted, in order`() {
        val supported = listOf(TransactionDataType.QES_APPROVAL, TransactionDataType.QES)
            .toSupportedTransactionDataTypes()

        assertEquals(
            listOf(TransactionDataType.QES_APPROVAL.value, TransactionDataType.QES.value),
            supported.map { it.type.value }
        )
    }

    @Test
    fun `makeOpenId4VPConfig forwards the configured types`() {
        val config = makeOpenId4VPConfig(
            config = builder().withTransactionDataTypes(TransactionDataType.QES_APPROVAL).build(),
            trust = mockk<OpenId4VpReaderTrust>()
        )

        assertEquals(
            listOf(TransactionDataType.QES_APPROVAL.value),
            config.supportedTransactionDataTypes.map { it.type.value }
        )
    }

    @Test
    fun `makeOpenId4VPConfig supports no type when none is configured`() {
        val config = makeOpenId4VPConfig(
            config = builder().build(),
            trust = mockk<OpenId4VpReaderTrust>()
        )

        assertTrue(config.supportedTransactionDataTypes.isEmpty())
    }

    @Test
    fun `no transaction data produces no key binding claims`() {
        assertTrue(emptyList<ParsedTransactionData<*>>().transactionDataKeyBindingClaims().isEmpty())
    }

    @Test
    fun `the hash is calculated over the transaction data string as received`() {
        // The value is base64url("{\"a\":1}"); the expected hash is base64url(sha-256(that string)),
        // calculated over the encoded string and not over the decoded JSON.
        val value = "eyJhIjoxfQ"
        val expected = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.US_ASCII))
        )

        val claims = listOf(transactionData(value)).transactionDataKeyBindingClaims()

        assertEquals(JsonPrimitive("sha-256"), claims["transaction_data_hashes_alg"])
        assertEquals(JsonArray(listOf(JsonPrimitive(expected))), claims["transaction_data_hashes"])
    }

    @Test
    fun `the hash differs from one calculated over the decoded transaction data`() {
        val value = "eyJhIjoxfQ"
        val overDecoded = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest("""{"a":1}""".toByteArray())
        )

        val claims = listOf(transactionData(value)).transactionDataKeyBindingClaims()

        assertNotEquals(JsonArray(listOf(JsonPrimitive(overDecoded))), claims["transaction_data_hashes"])
    }

    @Test
    fun `one hash is produced for every transaction data, in order`() {
        val claims = listOf(transactionData("eyJhIjoxfQ"), transactionData("eyJiIjoyfQ"))
            .transactionDataKeyBindingClaims()

        val hashes = claims["transaction_data_hashes"] as JsonArray
        assertEquals(2, hashes.size)
        assertNotEquals(hashes[0], hashes[1])
    }

    @Test
    fun `an unsupported hash algorithm is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            listOf(transactionData("eyJhIjoxfQ", listOf(Algorithm.SHA512)))
                .transactionDataKeyBindingClaims()
        }
    }

    @Test
    fun `a type cannot define a claim the Key Binding JWT itself carries`() {
        val error = assertFailsWith<IllegalArgumentException> {
            listOf(transactionData("eyJhIjoxfQ", type = ClaimStealingType("sd_hash")))
                .transactionDataKeyBindingClaims()
        }

        assertTrue(error.message.orEmpty().contains("sd_hash"))
    }

    @Test
    fun `a type cannot define a claim of the profile either`() {
        assertFailsWith<IllegalArgumentException> {
            listOf(transactionData("eyJhIjoxfQ", type = ClaimStealingType("transaction_data_hashes")))
                .transactionDataKeyBindingClaims()
        }
    }

    @Test
    fun `a type defines a claim of its own beside the profile`() {
        val claims = listOf(transactionData("eyJhIjoxfQ", type = ClaimStealingType("com.example.proof")))
            .transactionDataKeyBindingClaims()

        assertEquals(JsonPrimitive("bound"), claims["com.example.proof"])
        assertNotNull(claims["transaction_data_hashes"])
    }

    /**
     * Builds transaction data whose encoded value is [value]. The payload is irrelevant to the
     * hashes, which are calculated over the encoded value.
     */
    @Test
    fun `a rejected request is reported with its error`() {
        val error = ResolutionError.InvalidTransactionData(
            IllegalArgumentException("Unsupported Transaction Data 'type': 'x'")
        )

        val exception = OpenId4VpRequestException(error)

        assertEquals(error, exception.error)
    }

    @Test
    fun `a rejected request carries a message describing the error`() {
        val error = ResolutionError.InvalidTransactionData(IllegalArgumentException("boom"))

        val message = OpenId4VpRequestException(error).message

        assertNotNull(message)
        assertTrue(message.contains("InvalidTransactionData"))
    }

    /**
     * A transaction data type that returns the claim [claim], to show what the wallet does with a
     * type that defines a claim of its own.
     */
    private class ClaimStealingType(private val claim: String) : TransactionType<JsonObject>(
        displayName = "claim stealing",
        identifier = "https://example.com/2026/stealing",
    ), TransactionDataKeyBinding {
        override fun keyBindingClaims(
            transactionData: List<ParsedTransactionData<*>>
        ): Map<String, JsonElement> = mapOf(claim to JsonPrimitive("bound"))
    }

    private fun transactionData(
        value: String,
        hashAlgorithms: List<Algorithm>? = listOf(Algorithm.SHA256),
        type: TransactionType<JsonObject> = RawTransactionType("https://example.com/2026/test")
    ): ParsedTransactionData<JsonObject> = ParsedTransactionData(
        type = type,
        payload = JsonObject(emptyMap()),
        protocol = TransactionProtocol.OPENID4VP,
        rawBytes = ByteString(value.toByteArray(Charsets.US_ASCII)),
        hashAlgorithms = hashAlgorithms,
    )

    private fun builder(): OpenId4VpConfig.Builder = OpenId4VpConfig.Builder()
        .withClientIdSchemes(ClientIdScheme.X509SanDns)
        .withFormats(Format.MsoMdoc.ES256, Format.SdJwtVc.ES256)
}
