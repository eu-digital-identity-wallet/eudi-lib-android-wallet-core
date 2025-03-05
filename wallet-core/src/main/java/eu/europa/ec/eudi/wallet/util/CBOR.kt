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
                CborDecoder(bais).decode()
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
    @JvmStatic
    private fun cborPrettyPrintDataItem(
        sb: StringBuilder, indent: Int,
        dataItem: DataItem,
    ) {
        val indentBuilder = StringBuilder()
        for (n in 0 until indent) {
            indentBuilder.append(' ')
        }
        val indentString = indentBuilder.toString()
        if (dataItem.hasTag()) {
            sb.append(String.format(Locale.US, "tag %d ", dataItem.tag.value))
        }
        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        when (dataItem.majorType) {
            MajorType.INVALID -> sb.append("<invalid>")
            MajorType.UNSIGNED_INTEGER -> {
                val value = (dataItem as UnsignedInteger).value
                sb.append(value)
            }
            MajorType.NEGATIVE_INTEGER -> {
                val value: BigInteger = (dataItem as NegativeInteger).value
                sb.append(value)
            }
            MajorType.BYTE_STRING -> {
                val value = (dataItem as ByteString).bytes
                sb.append("[")
                for ((count, b) in value.withIndex()) {
                    if (count > 0) {
                        sb.append(", ")
                    }
                    sb.append(String.format("0x%02x", b))
                }
                sb.append("]")
            }
            MajorType.UNICODE_STRING -> {
                val value = (dataItem as UnicodeString).string
                sb.append("'$value'")
            }
            MajorType.ARRAY -> {
                val items = (dataItem as Array).dataItems
                if (items.size == 0) {
                    sb.append("[]")
                } else if (cborAreAllDataItemsNonCompound(items)) {
                    sb.append("[")
                    for ((count, item) in items.withIndex()) {
                        cborPrettyPrintDataItem(sb, indent, item)
                        if (count + 1 < items.size) {
                            sb.append(", ")
                        }
                    }
                    sb.append("]")
                } else {
                    sb.append("[\n$indentString")
                    for ((count, item) in items.withIndex()) {
                        sb.append("  ")
                        cborPrettyPrintDataItem(sb, indent + 2, item)
                        if (count + 1 < items.size) {
                            sb.append(",")
                        }
                        sb.append("\n$indentString")
                    }
                    sb.append("]")
                }
            }
            MajorType.MAP -> {
                val keys = (dataItem as Map).keys
                if (keys.isEmpty()) {
                    sb.append("{}")
                } else {
                    sb.append("{\n$indentString")
                    for ((count, key) in keys.withIndex()) {
                        sb.append("  ")
                        val value = dataItem[key]
                        cborPrettyPrintDataItem(sb, indent + 2, key)
                        sb.append(" : ")
                        cborPrettyPrintDataItem(sb, indent + 2, value)
                        if (count + 1 < keys.size) {
                            sb.append(",")
                        }
                        sb.append("\n$indentString")
                    }
                    sb.append("}")
                }
            }
            MajorType.TAG -> throw IllegalStateException("Semantic tag data item not expected")
            MajorType.SPECIAL -> when (dataItem) {
                is SimpleValue -> {
                    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
                    when (dataItem.simpleValueType) {
                        SimpleValueType.FALSE -> sb.append("false")
                        SimpleValueType.TRUE -> sb.append("true")
                        SimpleValueType.NULL -> sb.append("null")
                        SimpleValueType.UNDEFINED -> sb.append("undefined")
                        SimpleValueType.RESERVED -> sb.append("reserved")
                        SimpleValueType.UNALLOCATED -> sb.append("unallocated")
                    }
                }
                is DoublePrecisionFloat -> {
                    val df = DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
                    df.maximumFractionDigits = 340
                    sb.append(df.format(dataItem.value))
                }
                is AbstractFloat -> {
                    val df = DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
                    df.maximumFractionDigits = 340
                    sb.append(df.format(dataItem.value.toDouble()))
                }
                else -> sb.append("break")
            }
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