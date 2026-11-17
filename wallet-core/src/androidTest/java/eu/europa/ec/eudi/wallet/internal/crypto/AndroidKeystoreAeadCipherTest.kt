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

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.SecureRandom
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.AEADBadTagException

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreAeadCipherTest {

    private val keyAlias = "aead-cipher-test-${System.nanoTime()}"
    private val data = "tokens".toByteArray()
    private val associatedData = "associated data".toByteArray()

    @After
    fun deleteKey() {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(keyAlias)
    }

    @Test
    fun encryptedDataAreTheIvTheCiphertextAndTheTag() {
        val cipher = AndroidKeystoreAeadCipher(keyAlias)
        // An access token can be a JWT of several kilobytes
        val large = ByteArray(16 * 1024).also { SecureRandom().nextBytes(it) }

        val first = cipher.encrypt(large, associatedData)
        val second = cipher.encrypt(large, associatedData)

        assertEquals(AeadCipher.IV_SIZE + large.size + AeadCipher.TAG_SIZE_BITS / 8, first.size)
        // A new IV for each encryption
        assertFalse(first.copyOfRange(0, AeadCipher.IV_SIZE).contentEquals(second.copyOfRange(0, AeadCipher.IV_SIZE)))
        assertArrayEquals(large, cipher.decrypt(first, associatedData))
    }

    @Test
    fun encryptedDataAreNotDecryptedWithOtherAssociatedDataOrWhenAltered() {
        val cipher = AndroidKeystoreAeadCipher(keyAlias)
        val encrypted = cipher.encrypt(data, associatedData)
        val altered = encrypted.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }

        assertThrows(AEADBadTagException::class.java) { cipher.decrypt(encrypted, "other".toByteArray()) }
        assertThrows(AEADBadTagException::class.java) { cipher.decrypt(altered, associatedData) }
        assertThrows(IllegalArgumentException::class.java) { cipher.decrypt(encrypted.copyOf(27), associatedData) }
    }

    @Test
    fun encryptedDataAreNotDecryptedOnceTheKeyIsDeleted() {
        val cipher = AndroidKeystoreAeadCipher(keyAlias)
        val encrypted = cipher.encrypt(data, associatedData)

        deleteKey()

        assertThrows(AeadKeyNotFoundException::class.java) { cipher.decrypt(encrypted, associatedData) }
        // Decrypting does not create a key
        assertFalse(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(keyAlias))
    }

    @Test
    fun ciphersEncryptingAtTheSameTimeCreateOneKey() {
        val threads = 8
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threads)
        val results = (1..threads).map {
            executor.submit<ByteArray> {
                start.await()
                AndroidKeystoreAeadCipher(keyAlias).encrypt(data, associatedData)
            }
        }
        start.countDown()
        val encrypted = results.map { it.get(30, TimeUnit.SECONDS) }
        executor.shutdown()

        // With a second key, the data encrypted with the first one could not be decrypted
        encrypted.forEach {
            assertArrayEquals(data, AndroidKeystoreAeadCipher(keyAlias).decrypt(it, associatedData))
        }
    }
}
