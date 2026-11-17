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

package eu.europa.ec.eudi.wallet.internal.crypto

import javax.crypto.spec.SecretKeySpec
import java.security.ProviderException
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DataSealerTest {

    private val sealer = DataSealer(SoftwareAeadCipher())
    private val tokens = """{"accessToken":"at-0","refreshToken":"rt-0"}""".toByteArray()
    private val issuer = listOf("https://issuer.example.com")

    @Test
    fun `sealed tokens are opened for the same record`() {
        val sealed = sealer.seal("issuance_metadata", "doc", issuer, tokens)

        assertContentEquals(tokens, sealer.open("issuance_metadata", "doc", issuer, sealed))
    }

    @Test
    fun `tokens sealed in format version 1 are opened`() {
        // Computed with the JCA: AES-256-GCM, key 00..1f, IV a0..ab, associated data of this record with the SHA-256 digest of its bound field
        val sealed = "AaChoqOkpaanqKmqq506HU4mrnHMNgrstmlY+vwR2HQgsJtgHvloVOMMwyFuuRMp3ZUAIUlyrCa1JYhEuC+Ko+XsIuxDerDW1w=="
        val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")

        assertContentEquals(
            tokens,
            DataSealer(SoftwareAeadCipher(key)).open("issuance_metadata", "doc", issuer, sealed)
        )
    }

    @Test
    fun `sealed tokens are not opened for another document or record type`() {
        val sealed = sealer.seal("issuance_metadata", "doc", issuer, tokens)

        assertFailsWith<SealedDataUnreadableException> {
            sealer.open("issuance_metadata", "other-doc", issuer, sealed)
        }
        assertFailsWith<SealedDataUnreadableException> {
            sealer.open("deferred_context", "doc", issuer, sealed)
        }
    }

    @Test
    fun `sealed tokens are not opened when a field they are bound to changed`() {
        val sealed = sealer.seal("deferred_context", "doc", listOf("https://issuer.example.com", "a"), tokens)

        listOf(
            listOf("https://attacker.example.com", "a"),
            listOf("https://issuer.example.com", "b"),
            // The same fields joined with a separator
            listOf("https://issuer.example.com\u0000a"),
        ).forEach { boundFields ->
            assertFailsWith<SealedDataUnreadableException> {
                sealer.open("deferred_context", "doc", boundFields, sealed)
            }
        }
    }

    @Test
    fun `bound fields that are not valid text are rejected`() {
        // A lone surrogate, which a lenient encoder would encode as "?"
        val notText = "\uD800"
        val sealed = sealer.seal("issuance_metadata", "doc", listOf("?"), tokens)

        assertFailsWith<SealedDataUnreadableException> { sealer.open("issuance_metadata", "doc", listOf(notText), sealed) }
        assertFailsWith<IllegalArgumentException> { sealer.seal("issuance_metadata", "doc", listOf(notText), tokens) }
    }

    @Test
    fun `a missing key means the tokens cannot be read`() {
        val sealed = sealer.seal("issuance_metadata", "doc", issuer, tokens)
        val withoutKey = DataSealer(failingCipher(AeadKeyNotFoundException("Token encryption key not found")))

        assertFailsWith<SealedDataUnreadableException> {
            withoutKey.open("issuance_metadata", "doc", issuer, sealed)
        }
    }

    @Test
    fun `a key store failure that may be temporary is thrown as it is`() {
        val sealed = sealer.seal("issuance_metadata", "doc", issuer, tokens)
        val busyKeyStore = DataSealer(failingCipher(ProviderException("Keystore operation failed")))

        assertFailsWith<ProviderException> { busyKeyStore.open("issuance_metadata", "doc", issuer, sealed) }
    }

    @Test
    fun `sealed data shorter than the IV and the tag cannot be read`() {
        val truncated = Base64.getEncoder().encodeToString(byteArrayOf(DataSealer.FORMAT_VERSION) + ByteArray(27))
        // Rejects short data with an argument error, as the Android Keystore cipher does
        val cipherCheckingLength = DataSealer(failingCipher(IllegalArgumentException("Encrypted data is too short")))

        assertFailsWith<SealedDataUnreadableException> {
            cipherCheckingLength.open("issuance_metadata", "doc", issuer, truncated)
        }
    }

    @Test
    fun `an argument error of the cipher is thrown as it is`() {
        val sealed = sealer.seal("issuance_metadata", "doc", issuer, tokens)
        val cipherRejecting = DataSealer(failingCipher(IllegalArgumentException("Rejected by the key store")))

        assertFailsWith<IllegalArgumentException> { cipherRejecting.open("issuance_metadata", "doc", issuer, sealed) }
    }

    @Test
    fun `the associated data stay small whatever the bound fields`() {
        var associatedDataSize = 0
        val recordingCipher = object : AeadCipher {
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray {
                associatedDataSize = associatedData.size
                return SoftwareAeadCipher().encrypt(plaintext, associatedData)
            }
            override fun decrypt(ivAndCiphertext: ByteArray, associatedData: ByteArray) = throw UnsupportedOperationException()
        }
        // As many key aliases as a large batch
        val boundFields = listOf("[1000") + (1..1000).map { "=pop-key-alias-$it" }

        DataSealer(recordingCipher).seal("issuance_metadata", "doc", boundFields, tokens)

        assertTrue(associatedDataSize < 256, "Associated data of $associatedDataSize bytes")
    }

    @Test
    fun `sealed data of another format version are not opened`() {
        val bytes = Base64.getDecoder().decode(sealer.seal("issuance_metadata", "doc", issuer, tokens))
        val otherVersion = bytes.copyOf().also { it[0] = 2 }

        assertFailsWith<SealedDataUnreadableException> {
            sealer.open("issuance_metadata", "doc", issuer, Base64.getEncoder().encodeToString(otherVersion))
        }
    }

    private fun failingCipher(error: Exception) = object : AeadCipher {
        override fun encrypt(plaintext: ByteArray, associatedData: ByteArray) = throw error
        override fun decrypt(ivAndCiphertext: ByteArray, associatedData: ByteArray) = throw error
    }
}
