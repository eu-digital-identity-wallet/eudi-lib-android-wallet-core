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

import android.security.keystore.KeyPermanentlyInvalidatedException
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.AEADBadTagException

/**
 * Encrypts the data of a stored record with [cipher].
 *
 * The result is Base64 encoded: [FORMAT_VERSION], then the output of [AeadCipher.encrypt]. The
 * associated data are the purpose, the format version, the identifier of the record and the SHA-256
 * digest of the bound fields. Each of these, and each field in the digest, is preceded by its length.
 * Encrypted data therefore cannot be moved to another record, and are not decrypted when a bound field
 * was changed. The digest keeps the associated data small, since the Android Keystore may limit their
 * size.
 */
internal class DataSealer(private val cipher: AeadCipher) {

    /**
     * Encrypts [data] of the record [recordId] stored for [purpose], bound to [boundFields].
     */
    fun seal(purpose: String, recordId: String, boundFields: List<String>, data: ByteArray): String {
        val associatedData = associatedData(purpose, FORMAT_VERSION, recordId, boundFields)
        val encrypted = cipher.encrypt(data, associatedData)
        return Base64.getEncoder().encodeToString(byteArrayOf(FORMAT_VERSION) + encrypted)
    }

    /**
     * Decrypts the output of [seal] for the same [purpose], [recordId] and [boundFields].
     *
     * @throws SealedDataUnreadableException when the data can never be decrypted: they were altered
     * or bound to other fields, the key is missing or no longer usable, or the format is not
     * supported. Other failures, which may be temporary, are thrown as they are.
     */
    fun open(purpose: String, recordId: String, boundFields: List<String>, sealed: String): ByteArray {
        val (ivAndCiphertext, associatedData) = try {
            val bytes = Base64.getDecoder().decode(sealed)
            require(bytes.isNotEmpty() && bytes[0] == FORMAT_VERSION) { "Unsupported format of the sealed data" }
            require(bytes.size - 1 >= AeadCipher.IV_SIZE + AeadCipher.TAG_SIZE_BITS / 8) { "Sealed data are too short" }
            bytes.copyOfRange(1, bytes.size) to associatedData(purpose, bytes[0], recordId, boundFields)
        } catch (e: IllegalArgumentException) {
            throw SealedDataUnreadableException("The sealed data of record $recordId cannot be read", e)
        }
        return try {
            cipher.decrypt(ivAndCiphertext, associatedData)
        } catch (e: Exception) {
            if (!e.meansDataUnreadable()) throw e
            throw SealedDataUnreadableException("The sealed data of record $recordId cannot be read", e)
        }
    }

    private fun Exception.meansDataUnreadable(): Boolean =
        this is AEADBadTagException ||
            this is AeadKeyNotFoundException ||
            this is KeyPermanentlyInvalidatedException

    private fun associatedData(
        purpose: String,
        version: Byte,
        recordId: String,
        boundFields: List<String>,
    ): ByteArray {
        val boundFieldsDigest = MessageDigest.getInstance("SHA-256")
            .digest(lengthPrefixed(boundFields.map { it.encodeStrictly() }))
        return lengthPrefixed(listOf(purpose, version.toString(), recordId).map { it.encodeStrictly() } + boundFieldsDigest)
    }

    private fun lengthPrefixed(parts: List<ByteArray>): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            parts.forEach { part ->
                out.writeInt(part.size)
                out.write(part)
            }
        }
        return bytes.toByteArray()
    }

    // Strings that are not valid UTF-16 are rejected rather than encoded with replacement
    // characters, so two different strings never give the same associated data
    private fun String.encodeStrictly(): ByteArray = try {
        val encoded = Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .encode(CharBuffer.wrap(this))
        ByteArray(encoded.remaining()).also { encoded.get(it) }
    } catch (e: CharacterCodingException) {
        throw IllegalArgumentException("Associated data that is not valid text", e)
    }

    companion object {
        const val FORMAT_VERSION: Byte = 1
    }
}

/**
 * Sealed data can never be decrypted; see [DataSealer.open].
 */
internal class SealedDataUnreadableException(message: String, cause: Throwable) : Exception(message, cause)
