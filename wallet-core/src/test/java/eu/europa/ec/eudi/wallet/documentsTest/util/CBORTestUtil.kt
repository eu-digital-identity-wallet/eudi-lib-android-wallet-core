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

package eu.europa.ec.eudi.wallet.documentsTest.util

import co.nstant.`in`.cbor.model.Array
import co.nstant.`in`.cbor.model.DataItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


object CBORTestUtil {

    fun getCBORData(sampleDataCBOR: ByteArray): Map<String, Map<String, DataItem>> {

        val resultsList = HashMap<String, Map<String, DataItem>>()

        val documents =
            CborUtil.cborMapExtractArray(
                CborUtil.cborDecode(
                    sampleDataCBOR
                ),
                "documents"
            )

        documents.forEach { docDataItem ->

            val resultEntry = HashMap<String, DataItem>()

            val docType =
                CborUtil.cborMapExtractString(
                    docDataItem,
                    "docType"
                )

            val issuerSignedMap =
                CborUtil.cborMapExtractMap(
                    docDataItem,
                    "issuerSigned"
                )
            val nameSpacesMap =
                CborUtil.cborMapExtractMap(
                    issuerSignedMap,
                    "nameSpaces"
                )
            val nameSpaceKeys =
                CborUtil.cborMapExtractMapStringKeys(
                    nameSpacesMap
                )

            for (nameSpaceKey in nameSpaceKeys) {
                CborUtil.cborMapExtractArray(
                    nameSpacesMap,
                    nameSpaceKey
                ).forEach { issuerSignedItemBytes ->
                    val issuerSignedItemMap =
                        CborUtil.cborExtractTaggedAndEncodedCbor(
                            issuerSignedItemBytes
                        )
                    val elementId =
                        CborUtil.cborMapExtractString(
                            issuerSignedItemMap,
                            "elementIdentifier"
                        )
                    val dataItem =
                        CborUtil.cborMapExtract(
                            issuerSignedItemMap,
                            "elementValue"
                        )


                    resultEntry[elementId] = dataItem
                }
            }
            resultsList[docType] = resultEntry
        }
        return resultsList
    }

    fun getDrivingPrivilegeData(dataItem: DataItem): Map<String, DataItem> {
        val map = hashMapOf<String, DataItem>()
        cborMapExtract(dataItem, "issue_date")?.let { issueDate ->
            map["issue_date"] = issueDate
        }
        cborMapExtract(dataItem, "expiry_date")?.let { expiryDate ->
            map["expiry_date"] = expiryDate
        }
        cborMapExtract(dataItem, "vehicle_category_code")?.let { vcc ->
            map["vehicle_category_code"] = vcc
        }
        cborMapExtract(dataItem, "codes")?.let { codes ->
            map["codes"] = codes
        }
        return map
    }

    private fun getDataItemBytes(dataItem: DataItem): ByteArray {
        return CborUtil.cborEncode(
            dataItem
        )
    }

    fun getStringValue(dataItem: DataItem): String {
        return CborUtil.cborDecodeString(
            getDataItemBytes(dataItem)
        )
    }

    fun getByteStringValue(dataItem: DataItem): ByteArray {
        return CborUtil.cborDecodeByteString(
            getDataItemBytes(dataItem)
        )
    }

    fun getBooleanValue(dataItem: DataItem): Boolean {
        return CborUtil.cborDecodeBoolean(
            getDataItemBytes(dataItem)
        )
    }

    fun getNumValue(dataItem: DataItem): Long {
        return CborUtil.cborDecodeLong(
            getDataItemBytes(dataItem)
        )
    }

    fun getDataItemArrayContent(dataItem: DataItem): List<DataItem> {
        return CborUtil.castTo(
            Array::class.java,
            dataItem
        ).dataItems
    }

    fun checkFullDateFormat(dateValue: String): Boolean {
        val formatter = DateTimeFormatter.ISO_DATE
        val dateIn = LocalDate.parse(dateValue, formatter)
        return dateValue == formatter.format(dateIn)
    }

    fun checkTDateFormat(dateValue: String): Boolean {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
        val dateIn = LocalDateTime.parse(dateValue, formatter)
        return dateValue == formatter.format(dateIn)
    }

    private fun cborMapExtract(item: DataItem, field: String): DataItem? {
        return try {
            CborUtil.cborMapExtract(
                item,
                field
            )
        } catch (e: Exception) {
            null
        }
    }
}
