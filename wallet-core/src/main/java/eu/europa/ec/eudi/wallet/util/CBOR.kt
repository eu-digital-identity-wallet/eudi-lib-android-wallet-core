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
import co.nstant.`in`.cbor.model.UnicodeString
import co.nstant.`in`.cbor.model.UnsignedInteger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CBOR {
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
                throw java.lang.IllegalArgumentException("Error decoding CBOR", e)
            }
        }
    }

    @JvmStatic
    fun cborDecodeByteString(data: ByteArray): ByteArray = (cborDecode(data) as ByteString).bytes

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
            MajorType.INVALID ->                 // TODO: throw
                sb.append("<invalid>")

            MajorType.UNSIGNED_INTEGER -> {

                // Major type 0: an unsigned integer.
                val value = (dataItem as UnsignedInteger).value
                sb.append(value)
            }

            MajorType.NEGATIVE_INTEGER -> {

                // Major type 1: a negative integer.
                val value: BigInteger = (dataItem as NegativeInteger).value
                sb.append(value)
            }

            MajorType.BYTE_STRING -> {

                // Major type 2: a byte string.
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

                // Major type 3: string of Unicode characters that is encoded as UTF-8 [RFC3629].
                val value = (dataItem as UnicodeString).string
                // TODO: escape ' in |value|
                sb.append("'$value'")
            }

            MajorType.ARRAY -> {

                // Major type 4: an array of data items.
                val items = (dataItem as Array).dataItems
                if (items.size == 0) {
                    sb.append("[]")
                } else if (cborAreAllDataItemsNonCompound(items)) {
                    // The case where everything fits on one line.
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
                        sb.append(
                            """
                            
                            $indentString
                            """.trimIndent()
                        )
                    }
                    sb.append("]")
                }
            }

            MajorType.MAP -> {

                // Major type 5: a map of pairs of data items.
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
                        sb.append(
                            """
                            
                            $indentString
                            """.trimIndent()
                        )
                    }
                    sb.append("}")
                }
            }

            MajorType.TAG -> throw java.lang.IllegalStateException("Semantic tag data item not expected")
            MajorType.SPECIAL ->                 // Major type 7: floating point numbers and simple data types that need no
                // content, as well as the "break" stop code.
                when (dataItem) {
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
                        val df = DecimalFormat(
                            "0",
                            DecimalFormatSymbols.getInstance(Locale.ENGLISH)
                        )
                        df.maximumFractionDigits = 340
                        sb.append(df.format(dataItem.value))
                    }

                    is AbstractFloat -> {
                        val df = DecimalFormat(
                            "0",
                            DecimalFormatSymbols.getInstance(Locale.ENGLISH)
                        )
                        df.maximumFractionDigits = 340
                        sb.append(df.format(dataItem.value.toDouble()))
                    }

                    else -> sb.append("break")
                }
        }
    }

    private fun cborAreAllDataItemsNonCompound(items: List<DataItem>): Boolean {
        return items.all { it.majorType != MajorType.ARRAY && it.majorType != MajorType.MAP }
    }
}