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

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts and decrypts data with an authenticated cipher, which also authenticates associated data.
 */
internal interface AeadCipher {

    /**
     * Encrypts [plaintext] with a new IV. [associatedData] is authenticated together with the
     * plaintext and is not part of the output.
     *
     * @return the IV ([IV_SIZE] bytes), followed by the ciphertext and the tag
     */
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray

    /**
     * Decrypts the output of [encrypt].
     *
     * [DataSealer] takes a [javax.crypto.AEADBadTagException], an [AeadKeyNotFoundException] and a
     * [android.security.keystore.KeyPermanentlyInvalidatedException] as data that can never be
     * decrypted, and any other exception as a failure that may be temporary.
     *
     * @throws javax.crypto.AEADBadTagException when the data or [associatedData] are not the ones encrypted
     * @throws AeadKeyNotFoundException when the key is missing
     * @throws IllegalArgumentException when the data are shorter than the IV and the tag
     */
    fun decrypt(ivAndCiphertext: ByteArray, associatedData: ByteArray): ByteArray

    companion object {
        const val IV_SIZE = 12
        const val TAG_SIZE_BITS = 128
    }
}

/** The key of an [AeadCipher] is not in the key store. */
internal class AeadKeyNotFoundException(message: String) : Exception(message)

/**
 * [AeadCipher] using AES-GCM with a 256-bit key in the Android Keystore. The key needs no user
 * authentication, so that the data can be decrypted in the background.
 *
 * @param keyAlias the alias of the key in the Android Keystore
 */
internal class AndroidKeystoreAeadCipher(
    private val keyAlias: String,
) : AeadCipher {

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(associatedData)
        val ciphertext = cipher.doFinal(plaintext)
        val iv = cipher.iv
        check(iv.size == AeadCipher.IV_SIZE) { "Unexpected IV size ${iv.size}" }
        return iv + ciphertext
    }

    override fun decrypt(ivAndCiphertext: ByteArray, associatedData: ByteArray): ByteArray {
        require(ivAndCiphertext.size >= AeadCipher.IV_SIZE + AeadCipher.TAG_SIZE_BITS / 8) { "Encrypted data is too short" }
        val key = getKey() ?: throw AeadKeyNotFoundException("Key $keyAlias not found")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(AeadCipher.TAG_SIZE_BITS, ivAndCiphertext, 0, AeadCipher.IV_SIZE)
        )
        cipher.updateAAD(associatedData)
        return cipher.doFinal(
            ivAndCiphertext,
            AeadCipher.IV_SIZE,
            ivAndCiphertext.size - AeadCipher.IV_SIZE
        )
    }

    private fun getKey(): SecretKey? =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.getKey(keyAlias, null) as SecretKey?

    private fun getOrCreateKey(): SecretKey = synchronized(keyCreationLock) {
        getKey() ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build()
            )
            generateKey()
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private val keyCreationLock = Any()
    }
}
