/*
 * Copyright (c) 2023-2024 European Commission
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

package eu.europa.ec.eudi.wallet.sample.util

import co.nstant.`in`.cbor.model.DataItem
import co.nstant.`in`.cbor.model.MajorType
import eu.europa.ec.eudi.wallet.sample.ResourceHelper
import eu.europa.ec.eudi.wallet.sample.util.Constants.MDL_MANDATORY_FIELDS
import eu.europa.ec.eudi.wallet.sample.util.Constants.PID_MANDATORY_FIELDS
import org.json.JSONObject
import org.junit.Before
import java.util.*

open class BaseTest {

    private lateinit var data: Map<String, Map<String, DataItem>>

    @Before
    open fun setUp() {
        val sampleJson = ResourceHelper.loadString("sample_data.json")

        sampleJson?.let {
            val sampleDataCBOR = Base64.getDecoder().decode(
                JSONObject(
                    it
                ).getString("Data")
            )
            data = CBORTestUtil.getCBORData(sampleDataCBOR)
        }
    }

    private fun getDocField(docType: String, field: String): DataItem? {
        val docKey = data.keys.first { doc -> doc == docType }
        println("----- docType------$docKey")

        var dataItem: DataItem? = null
        val docData = data[docKey]
        val dataKey = docData?.keys?.firstOrNull { key -> key.contentEquals(field) }
        println("--> key: $dataKey")

        try {
            dataItem = docData?.get(dataKey)
            println(
                "--> value: ${
                    CborUtil.cborPrettyPrint(
                        dataItem!!
                    )
                }"
            )
        } catch (e: Exception) {
            println(e)
        }
        return dataItem
    }

    private fun testField(docType: String, field: String, dataType: MajorType?): DataItem? {
        val item = getDocField(docType, field)
        when (docType) {
            DocType.MDL.docTypeName -> if (MDL_MANDATORY_FIELDS.contains(field)) assert(item != null)
            DocType.PID.docTypeName -> if (PID_MANDATORY_FIELDS.contains(field)) assert(item != null)
        }
        if (item != null && dataType != null) {
            assert(item.majorType == dataType)
        }
        return item
    }

    protected fun testPIDField(field: String, dataType: MajorType? = null): DataItem? {
        return testField(pid, field, dataType)
    }

    protected fun testMDLField(field: String, dataType: MajorType? = null): DataItem? {
        return testField(mDL, field, dataType)
    }

    protected fun testArrayMDLField(field: String): DataItem? {
        return testField(mDL, field, MajorType.ARRAY)
    }

    protected fun testDataItemMapMDL(item: DataItem) {
        assert(item.majorType == MajorType.MAP)
    }

    protected fun testUnicodeStringMDLField(field: String): String {
        val raw = testMDLField(field, MajorType.UNICODE_STRING)
        val dataValue = CBORTestUtil.getStringValue(raw!!)
        assert(dataValue.length <= 150)
        return dataValue
    }

    protected fun testByteStringMdlField(field: String) {
        testMDLField(field, MajorType.BYTE_STRING)
    }

    protected fun testBooleanMDLField(field: String) {
        testMDLField(field, MajorType.SPECIAL)
    }

    protected fun testFullDateMDLField(field: String) {
        val raw = testMDLField(field)
        assert(raw?.tag?.value == 1004L)
        assert(CBORTestUtil.checkFullDateFormat(CBORTestUtil.getStringValue(raw!!)))
    }

    protected fun testTDateOrFullDateMDLField(field: String) {
        val raw = testMDLField(field)
        val data = CBORTestUtil.getStringValue(raw!!)
        assert(CBORTestUtil.checkTDateFormat(data) || CBORTestUtil.checkFullDateFormat(data))
    }

    protected fun testNumMDLField(field: String): Long {
        val raw = testMDLField(field, MajorType.UNSIGNED_INTEGER)
        return CBORTestUtil.getNumValue(raw!!)
    }

    protected fun testUnicodeStringPIDField(field: String): String {
        val raw = testPIDField(field, MajorType.UNICODE_STRING)
        val dataValue = CBORTestUtil.getStringValue(raw!!)
        assert(dataValue.length <= 150)
        return dataValue
    }

    protected fun testByteStringPIDField(field: String) {
        testPIDField(field, MajorType.BYTE_STRING)
    }

    protected fun testTstrOrBstrPIDField(field: String) {
        try {
            testUnicodeStringPIDField(field)
        } catch (e: Exception) {
            testByteStringPIDField(field)
        }
    }

    protected fun testBooleanPIDField(field: String) {
        testPIDField(field, MajorType.SPECIAL)
    }

    protected fun testFullDatePIDField(field: String) {
        val raw = testPIDField(field)
        assert(raw?.tag?.value == 1004L)
        assert(CBORTestUtil.checkFullDateFormat(CBORTestUtil.getStringValue(raw!!)))
    }

    protected fun testTDateOrFullDatePIDField(field: String) {
        testPIDField(field)?.let { item ->
            val data = CBORTestUtil.getStringValue(item)
            assert(CBORTestUtil.checkTDateFormat(data) || CBORTestUtil.checkFullDateFormat(data))
        }
    }

    protected fun testNumPIDField(field: String): Long {
        val raw = testPIDField(field, MajorType.UNSIGNED_INTEGER)
        return CBORTestUtil.getNumValue(raw!!)
    }

    protected fun testOverNNField(field: String) {
        val raw = testMDLField(field)
        CBORTestUtil.getStringValue(raw!!)
    }

    companion object {
        private const val pid = "eu.europa.ec.eudi.pid.1"
        private const val mDL = "org.iso.18013.5.1.mDL"
    }


}
