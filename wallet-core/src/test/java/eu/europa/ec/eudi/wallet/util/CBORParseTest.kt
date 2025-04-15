/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.util

import co.nstant.`in`.cbor.model.Array
import co.nstant.`in`.cbor.model.ByteString
import co.nstant.`in`.cbor.model.DoublePrecisionFloat
import co.nstant.`in`.cbor.model.Map
import co.nstant.`in`.cbor.model.NegativeInteger
import co.nstant.`in`.cbor.model.SimpleValue
import co.nstant.`in`.cbor.model.SimpleValueType
import co.nstant.`in`.cbor.model.Tag
import co.nstant.`in`.cbor.model.UnicodeString
import co.nstant.`in`.cbor.model.UnsignedInteger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Test

class CBORParseTest {

    @Test
    fun cborParse_unsignedInteger_returnsLongValue() {
        val dataItem = UnsignedInteger(42)
        val encoded = CBOR.cborEncode(dataItem)
        val result = CBOR.cborParse(encoded)
        assertEquals(42L, result)
    }

    @Test
    fun cborParse_negativeInteger_returnsLongValue() {
        val dataItem = NegativeInteger(-42)
        val encoded = CBOR.cborEncode(dataItem)
        val result = CBOR.cborParse(encoded)
        assertEquals(-42L, result)
    }

    @Test
    fun cborParse_byteString_returnsByteArray() {
        val bytes = "Hello".toByteArray()
        val dataItem = ByteString(bytes)
        val encoded = CBOR.cborEncode(dataItem)
        val result = CBOR.cborParse(encoded)
        assertArrayEquals(bytes, result as ByteArray)
    }

    @Test
    fun cborParse_unicodeString_returnsString() {
        val dataItem = UnicodeString("Hello")
        val encoded = CBOR.cborEncode(dataItem)
        val result = CBOR.cborParse(encoded)
        assertEquals("Hello", result)
    }

    @Test
    fun cborParse_taggedDateTime_returnsZonedDateTime() {
        val date = "2023-05-01T12:30:45Z"
        val dataItem = UnicodeString(date)
        dataItem.tag = Tag(0)
        val encoded = CBOR.cborEncode(dataItem)
        val result = CBOR.cborParse(encoded)
        assertTrue(result is ZonedDateTime)
        assertEquals(date, result.toString().replace("[UTC]", "Z"))
    }

    @Test
    fun cborParse_taggedDate_returnsLocalDate() {
        val date = "2023-05-01"
        val dataItem = UnicodeString(date)
        dataItem.tag = Tag(1004)
        val encoded = CBOR.cborEncode(dataItem)
        val result = CBOR.cborParse(encoded)
        assertTrue(result is LocalDate)
        assertEquals(date, result.toString())
    }

    @Test
    fun cborParse_array_returnsListOfParsedItems() {
        val array = Array()
        array.add(UnsignedInteger(1))
        array.add(UnicodeString("text"))
        val encoded = CBOR.cborEncode(array)
        val result = CBOR.cborParse(encoded)
        assertTrue(result is List<*>)
        assertEquals(2, (result as List<*>).size)
        assertEquals(1L, result[0])
        assertEquals("text", result[1])
    }

    @Test
    fun cborParse_map_returnsMapOfParsedItems() {
        val map = Map()
        map.put(UnicodeString("key1"), UnsignedInteger(42))
        map.put(UnicodeString("key2"), UnicodeString("value"))
        val encoded = CBOR.cborEncode(map)
        val result = CBOR.cborParse(encoded)
        assertTrue(result is kotlin.collections.Map<*, *>)
        val resultMap = result as kotlin.collections.Map<*, *>
        assertEquals(2, resultMap.size)
        assertEquals(42L, resultMap["key1"])
        assertEquals("value", resultMap["key2"])
    }

    @Test
    fun cborParse_booleanTrue_returnsTrue() {
        val dataItem = SimpleValue(SimpleValueType.TRUE)
        val encoded = CBOR.cborEncode(dataItem)
        val result = CBOR.cborParse(encoded)
        assertEquals(true, result)
    }

    @Test
    fun cborParse_booleanFalse_returnsFalse() {
        val dataItem = SimpleValue(SimpleValueType.FALSE)
        val encoded = CBOR.cborEncode(dataItem)
        val result = CBOR.cborParse(encoded)
        assertEquals(false, result)
    }

    @Test
    fun cborParse_null_returnsNull() {
        val dataItem = SimpleValue(SimpleValueType.NULL)
        val encoded = CBOR.cborEncode(dataItem)
        val result = CBOR.cborParse(encoded)
        assertNull(result)
    }

    @Test
    fun cborParse_doublePrecisionFloat_returnsDouble() {
        val dataItem = DoublePrecisionFloat(3.14)
        val encoded = CBOR.cborEncode(dataItem)
        val result = CBOR.cborParse(encoded)
        assertEquals(3.14, result as Double, 0.0001)
    }
}