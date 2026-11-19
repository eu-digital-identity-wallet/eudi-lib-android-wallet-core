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

import eu.europa.ec.eudi.iso18013.transfer.readerauth.CrlStore
import eu.europa.ec.eudi.wallet.testCrl
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URI
import java.time.Instant
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EncryptedCrlStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val encryptionKey: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val macKey: SecretKey = KeyGenerator.getInstance("HmacSHA256").generateKey()
    private val uri = URI("https://example.com/crl")
    private val crl = testCrl(42)
    private val fetchedAt = Instant.ofEpochMilli(1_790_000_000_000)

    private fun store() = EncryptedCrlStore(folder.root, { encryptionKey }, { macKey })

    @Test
    fun `a saved CRL is loaded back with when it was downloaded`() {
        store().save(uri, CrlStore.StoredCrl(crl, fetchedAt))

        val loaded = store().load(uri)!!
        assertEquals(crl, loaded.crl)
        assertEquals(fetchedAt, loaded.fetchedAt)
    }

    @Test
    fun `nothing is loaded for a distribution point that was not saved`() {
        assertNull(store().load(uri))
    }

    @Test
    fun `the file reveals neither the distribution point, the CRL nor when it was downloaded`() {
        store().save(uri, CrlStore.StoredCrl(crl, fetchedAt))

        val file = folder.root.listFiles()!!.single()
        assertFalse(file.name.contains("example"))
        val content = file.readBytes()
        assertFalse(content.containsSubArray(crl.issuerX500Principal.encoded))
        assertFalse(content.containsSubArray(crl.encoded.copyOfRange(0, 32)))
        assertEquals(0, file.lastModified())
        assertEquals(0, folder.root.lastModified())
    }

    @Test
    fun `a file moved to another distribution point is not loaded`() {
        val other = URI("https://example.com/other")
        store().save(uri, CrlStore.StoredCrl(crl, fetchedAt))
        store().save(other, CrlStore.StoredCrl(testCrl(), fetchedAt))
        val files = folder.root.listFiles()!!.sortedBy { it.name }
        val swapped = files[0].readBytes()
        files[0].writeBytes(files[1].readBytes())
        files[1].writeBytes(swapped)

        assertFails { store().load(uri) }
        assertFails { store().load(other) }
    }

    @Test
    fun `another key cannot read the CRL`() {
        store().save(uri, CrlStore.StoredCrl(crl, fetchedAt))
        val otherKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        assertFails { EncryptedCrlStore(folder.root, { otherKey }, { macKey }).load(uri) }
    }

    @Test
    fun `saving again replaces the CRL`() {
        val newer = testCrl()
        store().save(uri, CrlStore.StoredCrl(crl, fetchedAt))
        store().save(uri, CrlStore.StoredCrl(newer, fetchedAt.plusSeconds(60)))

        assertEquals(1, folder.root.listFiles()!!.size)
        assertContentEquals(newer.encoded, store().load(uri)!!.crl.encoded)
        assertTrue(store().load(uri)!!.fetchedAt.isAfter(fetchedAt))
    }

    private fun ByteArray.containsSubArray(part: ByteArray): Boolean =
        (0..size - part.size).any { start -> part.indices.all { this[start + it] == part[it] } }
}
