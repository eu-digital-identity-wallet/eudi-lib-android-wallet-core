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

package eu.europa.ec.eudi.wallet.trust

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import eu.europa.ec.eudi.iso18013.transfer.readerauth.CrlStore
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509CRL
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps each CRL encrypted with AES-GCM in a file of [directory], together with when it was
 * downloaded. The file is named by an HMAC of the distribution point and the distribution point is
 * authenticated with the CRL, so neither the files' names nor their times tell which CRLs are kept
 * or when they were downloaded.
 */
internal class EncryptedCrlStore(
    private val directory: File,
    encryptionKey: () -> SecretKey,
    macKey: () -> SecretKey,
) : CrlStore {

    private val encryptionKey by lazy(encryptionKey)
    private val macKey by lazy(macKey)

    override fun load(uri: URI): CrlStore.StoredCrl? {
        val file = fileFor(uri).takeIf { it.isFile } ?: return null
        val content = file.readBytes()
        val plain = Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, encryptionKey, GCMParameterSpec(TAG_BITS, content, 0, IV_BYTES))
            updateAAD(uri.toString().toByteArray())
            doFinal(content, IV_BYTES, content.size - IV_BYTES)
        }
        val fetchedAt = Instant.ofEpochMilli(ByteBuffer.wrap(plain, 0, Long.SIZE_BYTES).long)
        val crl = plain.inputStream(Long.SIZE_BYTES, plain.size - Long.SIZE_BYTES)
            .use { CertificateFactory.getInstance("X.509").generateCRL(it) as X509CRL }
        return CrlStore.StoredCrl(crl, fetchedAt)
    }

    override fun save(uri: URI, stored: CrlStore.StoredCrl) {
        val plain = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(stored.fetchedAt.toEpochMilli()).array() + stored.crl.encoded
        val content = Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.ENCRYPT_MODE, encryptionKey)
            updateAAD(uri.toString().toByteArray())
            val encrypted = doFinal(plain)
            check(iv.size == IV_BYTES) { "unexpected IV length ${iv.size}" }
            iv + encrypted
        }
        directory.mkdirs()
        val file = fileFor(uri)
        // Written in place: a torn write fails to decrypt and reads as no CRL.
        file.writeBytes(content)
        file.setLastModified(0)
        directory.setLastModified(0)
    }

    private fun fileFor(uri: URI): File {
        val mac = Mac.getInstance(MAC_ALGORITHM).run {
            init(macKey)
            doFinal(uri.toString().toByteArray())
        }
        return File(directory, mac.joinToString("") { "%02x".format(it) })
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val MAC_ALGORITHM = "HmacSHA256"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val ENCRYPTION_KEY_ALIAS = "eudi-wallet-crl-cache-encryption"
        private const val MAC_KEY_ALIAS = "eudi-wallet-crl-cache-mac"

        /** An [EncryptedCrlStore] whose keys are kept in the Android Keystore. */
        fun inAndroidKeyStore(directory: File) = EncryptedCrlStore(
            directory,
            encryptionKey = {
                androidKeyStoreKey(ENCRYPTION_KEY_ALIAS, KeyProperties.KEY_ALGORITHM_AES) { alias ->
                    KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                }
            },
            macKey = {
                androidKeyStoreKey(MAC_KEY_ALIAS, KeyProperties.KEY_ALGORITHM_HMAC_SHA256) { alias ->
                    KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN).build()
                }
            },
        )

        private fun androidKeyStoreKey(alias: String, algorithm: String, spec: (String) -> KeyGenParameterSpec): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
            return KeyGenerator.getInstance(algorithm, ANDROID_KEY_STORE).apply { init(spec(alias)) }.generateKey()
        }
    }
}
