/*
 * Copyright (c) 2023 European Commission
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

import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.CborEncoder
import co.nstant.`in`.cbor.CborException
import co.nstant.`in`.cbor.model.AbstractFloat
import co.nstant.`in`.cbor.model.Array
import co.nstant.`in`.cbor.model.ByteString
import co.nstant.`in`.cbor.model.DataItem
import co.nstant.`in`.cbor.model.DoublePrecisionFloat
import co.nstant.`in`.cbor.model.MajorType
import co.nstant.`in`.cbor.model.Map
import co.nstant.`in`.cbor.model.NegativeInteger
import co.nstant.`in`.cbor.model.SimpleValue
import co.nstant.`in`.cbor.model.SimpleValueType
import co.nstant.`in`.cbor.model.Tag
import co.nstant.`in`.cbor.model.UnicodeString
import co.nstant.`in`.cbor.model.UnsignedInteger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale

/*
 * Utility object for encoding and decoding CBOR (Concise Binary Object Representation) data.
 */
object CBOR {
    /**
     * Encodes a given [DataItem] into a CBOR byte array.
     *
     * @param dataItem The [DataItem] to encode.
     * @return A byte array representing the encoded CBOR data.
     * @throws IllegalStateException If encoding fails.
     */
    @JvmStatic
    fun cborEncode(dataItem: DataItem): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            try {
                CborEncoder(stream).encode(dataItem)
            } catch (e: CborException) {
                // This should never happen and we don't want cborEncode() to throw since that
                // would complicate all callers. Log it instead.
                throw IllegalStateException("Unexpected failure encoding data", e)
            }
            stream.toByteArray()
        }
    }

    /**
     * Decodes a given CBOR byte array into a [DataItem].
     *
     * @param encodedBytes The byte array to decode.
     * @return The decoded [DataItem].
     * @throws IllegalArgumentException If decoding fails or the number of decoded items is not 1.
     */
    @JvmStatic
    fun cborDecode(encodedBytes: ByteArray): DataItem {
        return ByteArrayInputStream(encodedBytes).use { stream ->
            try {
                val dataItems: List<DataItem> = CborDecoder(stream).decode()
                require(dataItems.size == 1) {
                    "Unexpected number of items, expected 1 got ${dataItems.size}"
                }
                dataItems[0]
            } catch (e: CborException) {
                throw IllegalArgumentException("Error decoding CBOR", e)
            }
        }
    }

    /**
     * Decodes a given CBOR byte array into a byte array.
     *
     * @param data The CBOR byte array to decode.
     * @return The decoded byte array.
     */
    @JvmStatic
    fun cborDecodeByteString(data: ByteArray): ByteArray = (cborDecode(data) as ByteString).bytes

    /**
     * Pretty prints a given CBOR byte array.
     *
     * @param encodedBytes The CBOR byte array to pretty print.
     * @return A string representing the pretty-printed CBOR data.
     * @throws IllegalStateException If decoding fails.
     */
    @JvmStatic
    fun cborPrettyPrint(encodedBytes: ByteArray): String =
        ByteArrayInputStream(encodedBytes).use { bais ->
            val dataItems = try {
                CborDecoder(bais).decode().toList()
            } catch (e: CborException) {
                throw IllegalStateException(e)
            }
            StringBuilder().apply {
                for ((index, dataItem) in dataItems.withIndex()) {
                    if (index > 0) append(",\n")
                    cborPrettyPrintDataItem(this@apply, 0, dataItem)
                }
            }
        }.toString()

    /**
     * Helper function to pretty print a [DataItem].
     *
     * @param sb The [StringBuilder] to append the pretty-printed data to.
     * @param indent The current indentation level.
     * @param dataItem The [DataItem] to pretty print.
     */
    private fun cborPrettyPrintDataItem(
        sb: StringBuilder, indent: Int,
        dataItem: DataItem,
    ) {
        sb.append(cborBuildPrintString(indent, dataItem))
    }

    private val cborFloatFormatter =
        DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).apply {
            maximumFractionDigits = 340
        }

    /**
     * Recursively builds and returns a pretty-printed string for a DataItem.
     * This is the main dispatcher.
     */
    private fun cborBuildPrintString(indent: Int, dataItem: DataItem): String {
        val tagString = if (dataItem.hasTag()) {
            String.format(Locale.US, "tag %d ", dataItem.tag.value)
        } else {
            ""
        }

        val cborString = when (dataItem.majorType) {
            MajorType.INVALID -> "<invalid>"
            MajorType.UNSIGNED_INTEGER -> (dataItem as UnsignedInteger).value.toString()
            MajorType.NEGATIVE_INTEGER -> (dataItem as NegativeInteger).value.toString()
            MajorType.UNICODE_STRING -> "'${(dataItem as UnicodeString).string}'"
            MajorType.TAG -> throw IllegalStateException("Semantic tag data item not expected")

            MajorType.BYTE_STRING -> cborPrintByteString(dataItem as ByteString)
            MajorType.ARRAY -> cborPrintArray(indent, dataItem as Array)
            MajorType.MAP -> cborPrintMap(indent, dataItem as Map)
            MajorType.SPECIAL -> cborPrintSpecial(dataItem)
        }

        return tagString + cborString
    }

    /**
     * Handles Byte Strings.
     */
    private fun cborPrintByteString(dataItem: ByteString): String {
        return dataItem.bytes.joinToString(prefix = "[", separator = ", ", postfix = "]") { b -> // +1
            String.format("0x%02x", b)
        }
    }

    /**
     * Handles Arrays (your biggest complexity source).
     */
    private fun cborPrintArray(indent: Int, dataItem: Array): String {
        val items = dataItem.dataItems?.toList() ?: emptyList()
        if (items.isEmpty()) return "[]"

        val indentString = " ".repeat(indent)

        return if (cborAreAllDataItemsNonCompound(items)) {
            // All simple: Print on one line
            items.joinToString(prefix = "[", separator = ", ", postfix = "]") { item ->
                cborBuildPrintString(indent, item)
            }
        } else {
            items.joinToString(
                prefix = "[\n$indentString  ",
                separator = ",\n$indentString  ",
                postfix = "\n$indentString]"
            ) { item ->
                cborBuildPrintString(indent + 2, item)
            }
        }
    }

    /**
     * Handles Maps
     */
    private fun cborPrintMap(indent: Int, dataItem: Map): String {
        val keys = dataItem.keys
        if (keys.isEmpty()) return "{}"

        val indentString = " ".repeat(indent)

        return keys.joinToString(
            prefix = "{\n$indentString  ",
            separator = ",\n$indentString  ",
            postfix = "\n$indentString}"
        ) { key ->
            val value = dataItem[key]
            val keyString = cborBuildPrintString(indent + 2, key)
            val valueString = cborBuildPrintString(indent + 2, value)
            "$keyString : $valueString"
        }
    }

    /**
     * Handles Special types.
     */
    private fun cborPrintSpecial(dataItem: DataItem): String {
        return when (dataItem) { // +1
            is SimpleValue -> when (dataItem.simpleValueType) { // +2
                SimpleValueType.FALSE -> "false"
                SimpleValueType.TRUE -> "true"
                SimpleValueType.NULL -> "null"
                SimpleValueType.UNDEFINED -> "undefined"
                SimpleValueType.RESERVED -> "reserved"
                SimpleValueType.UNALLOCATED -> "unallocated"
            }
            is DoublePrecisionFloat -> cborFloatFormatter.format(dataItem.value)
            is AbstractFloat -> cborFloatFormatter.format(dataItem.value.toDouble())
            else -> "break"
        }
    }

    /**
     * Checks if all [DataItem]s in a list are non-compound (i.e., not arrays or maps).
     *
     * @param items The list of [DataItem]s to check.
     * @return `true` if all items are non-compound, `false` otherwise.
     */
    private fun cborAreAllDataItemsNonCompound(items: List<DataItem>): Boolean {
        return items.all { it.majorType != MajorType.ARRAY && it.majorType != MajorType.MAP }
    }

    /**
     * Parses a given CBOR byte array into a Kotlin object.
     *
     * @param data The CBOR byte array to parse.
     * @return The parsed object.
     */
    fun cborParse(data: ByteArray): Any? {
        val dataItem = cborDecode(data)
        return cborParse(dataItem)
    }

    /**
     * Parses a given [DataItem] into a Kotlin object.
     *
     * @param dataItem The [DataItem] to parse.
     * @return The parsed object.
     */
    private fun cborParse(dataItem: DataItem): Any? {
        return when (dataItem.majorType) {
            MajorType.INVALID -> "invalid"
            MajorType.UNSIGNED_INTEGER -> (dataItem as UnsignedInteger).value.toLong()
            MajorType.NEGATIVE_INTEGER -> (dataItem as NegativeInteger).value.toLong()
            MajorType.BYTE_STRING -> (dataItem as ByteString).bytes
            MajorType.UNICODE_STRING -> {
                val value = (dataItem as UnicodeString).string
                when {
                    dataItem.tag == Tag(0) -> ZonedDateTime.parse(value)
                    dataItem.tag == Tag(1004) -> LocalDate.parse(value)
                    else -> value
                }
            }
            MajorType.ARRAY -> (dataItem as Array).dataItems.map { cborParse(it) }
            MajorType.MAP -> {
                val map = (dataItem as Map)
                map.keys.associate { cborParse(it) to cborParse(map[it]) }
            }
            MajorType.TAG -> null
            MajorType.SPECIAL -> when (dataItem) {
                is SimpleValue -> when (dataItem.simpleValueType) {
                    SimpleValueType.FALSE -> false
                    SimpleValueType.TRUE -> true
                    SimpleValueType.NULL -> null
                    SimpleValueType.UNDEFINED -> "undefined"
                    SimpleValueType.RESERVED -> "reserved"
                    SimpleValueType.UNALLOCATED -> "unallocated"
                }
                is DoublePrecisionFloat -> dataItem.value
                is AbstractFloat -> dataItem.value.toDouble()
                else -> null
            }
        }
    }
}