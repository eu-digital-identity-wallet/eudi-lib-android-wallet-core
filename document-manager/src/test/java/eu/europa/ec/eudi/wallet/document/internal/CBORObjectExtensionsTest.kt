/*
 * Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet.document.internal

import com.upokecenter.cbor.CBORObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CBORObjectExtensionsTest {

    @Test
    fun `get Embedded CBORObject with Tag 24 returns Decoded Object`() {
        val byteArray =
            CBORObject.FromObjectAndTag(CBORObject.FromObject("test").EncodeToBytes(), 24)
                .EncodeToBytes()
        val result = byteArray.getEmbeddedCBORObject()

        assertEquals("test", result.AsString())
    }

    @Test
    fun `get Embedded CBORObject without Tag 24 returns Same Object`() {
        val embedded = CBORObject.FromObject("test").EncodeToBytes()
        val byteArray = CBORObject.FromObject(embedded).EncodeToBytes()
        val result = byteArray.getEmbeddedCBORObject()

        assertContentEquals(embedded, result.GetByteString())
    }

    @Test
    fun `with Tag 24 returns ByteArray with Tag 24`() {
        val byteArray = "test".toByteArray()
        val result = byteArray.withTag24()

        val cborObject = CBORObject.DecodeFromBytes(result)
        assertTrue(cborObject.HasTag(24))
    }

    @Test
    fun `to Object with Null returns Null`() {
        val byteArray = CBORObject.Null.EncodeToBytes()
        val result = byteArray.toObject()

        assertNull(result)
    }

    @Test
    fun `to Object with Boolean True returns True`() {
        val byteArray = CBORObject.True.EncodeToBytes()
        val result = byteArray.toObject()

        assertEquals(true, result)
    }

    @Test
    fun `to Object with Boolean False returns False`() {
        val byteArray = CBORObject.False.EncodeToBytes()
        val result = byteArray.toObject()

        assertEquals(false, result)
    }

    @Test
    fun `to Object with Integer returns Integer`() {
        val byteArray = CBORObject.FromObject(42).EncodeToBytes()
        val result = byteArray.toObject()

        assertEquals(42, result)
    }

    @Test
    fun `to Object with Long returns Long`() {
        val long = Random.nextLong()
        val byteArray = CBORObject.FromObject(long).EncodeToBytes()
        val result = byteArray.toObject()

        assertEquals(long, result)
    }

    @Test
    fun `to Object with Double returns Double`() {
        val double = Random.nextDouble(from = Float.MAX_VALUE.toDouble(), until = Double.MAX_VALUE)
        val byteArray = CBORObject.FromObject(double).EncodeToBytes()
        val result = byteArray.toObject()

        assertEquals(double, result)
    }

    @Test
    fun `to Object with Float returns Double`() {
        val float = Random.nextFloat()
        val byteArray = CBORObject.FromObject(float).EncodeToBytes()
        val result = byteArray.toObject()

        assertEquals(float.toDouble(), result)
    }

    @Test
    fun `to Object with Text String returns String`() {
        val byteArray = CBORObject.FromObject("test").EncodeToBytes()
        val result = byteArray.toObject()

        assertEquals("test", result)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `to Object with Byte String returns Base64 String`() {
        val byteArray = CBORObject.FromObject("test".toByteArray()).EncodeToBytes()
        val result = byteArray.toObject()

        assertEquals(Base64.encode("test".toByteArray()), result)
    }

    @Test
    fun `to Object with Array returns List`() {
        val byteArray = CBORObject.NewArray().Add(1).Add(2).Add(3).EncodeToBytes()
        val result = byteArray.toObject()

        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun `to Object with Map returns Map`() {
        val byteArray = CBORObject.NewMap().Add("key", "value").EncodeToBytes()
        val result = byteArray.toObject()

        assertEquals(mapOf("key" to "value"), result)
    }

    @Test
    fun `to Object with Nested Structures returns Correctly Parsed Object`() {
        val byteArray = CBORObject.NewMap().apply {
            Add("key1", CBORObject.NewArray().Add(1).Add(2).Add(3))
            Add("key2", CBORObject.NewMap().Add("nestedKey", "nestedValue"))
        }.EncodeToBytes()
        val result = byteArray.toObject()

        assertEquals(
            mapOf(
                "key1" to listOf(1, 2, 3),
                "key2" to mapOf("nestedKey" to "nestedValue")
            ), result
        )
    }
}