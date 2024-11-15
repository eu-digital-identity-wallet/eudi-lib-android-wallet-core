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

package eu.europa.ec.eudi.wallet.sample

import co.nstant.`in`.cbor.model.MajorType
import eu.europa.ec.eudi.wallet.sample.util.BaseTest
import eu.europa.ec.eudi.wallet.sample.util.CBORTestUtil
import eu.europa.ec.eudi.wallet.sample.util.Constants.BIRTH_DATE
import eu.europa.ec.eudi.wallet.sample.util.Constants.CODES
import eu.europa.ec.eudi.wallet.sample.util.Constants.DOCUMENT_NUMBER
import eu.europa.ec.eudi.wallet.sample.util.Constants.DRIVING_PRIVILEGES
import eu.europa.ec.eudi.wallet.sample.util.Constants.EXPIRY_DATE
import eu.europa.ec.eudi.wallet.sample.util.Constants.FAMILY_NAME
import eu.europa.ec.eudi.wallet.sample.util.Constants.GIVEN_NAME
import eu.europa.ec.eudi.wallet.sample.util.Constants.ISSUE_DATE
import eu.europa.ec.eudi.wallet.sample.util.Constants.ISSUING_AUTHORITY
import eu.europa.ec.eudi.wallet.sample.util.Constants.ISSUING_COUNTRY
import eu.europa.ec.eudi.wallet.sample.util.Constants.PORTRAIT
import eu.europa.ec.eudi.wallet.sample.util.Constants.UN_DISTINGUISHING_SIGN
import eu.europa.ec.eudi.wallet.sample.util.Constants.VEHICLE_CATEGORY_CODE
import org.junit.Before
import org.junit.Test

class MDLMandatoryFieldsTests : BaseTest() {

    @Before
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun `family_name is valid`() {
        testUnicodeStringMDLField(FAMILY_NAME)
    }

    @Test
    fun `given_name is valid`() {
        testUnicodeStringMDLField(GIVEN_NAME)
    }

    @Test
    fun `birth_date is valid`() {
        testFullDateMDLField(BIRTH_DATE)
    }

    @Test
    fun `issue_date is valid`() {
        testTDateOrFullDateMDLField(ISSUE_DATE)
    }

    @Test
    fun `expiry_date is valid`() {
        testTDateOrFullDateMDLField(EXPIRY_DATE)
    }

    @Test
    fun `issuing_country is valid`() {
        testUnicodeStringMDLField(ISSUING_COUNTRY)
    }

    @Test
    fun `issuing_authority is valid`() {
        testUnicodeStringMDLField(ISSUING_AUTHORITY)
    }

    @Test
    fun `document_number is valid`() {
        testUnicodeStringMDLField(DOCUMENT_NUMBER)
    }

    @Test
    fun `portrait is valid`() {
        testByteStringMdlField(PORTRAIT)
    }

    @Test
    fun `driving_privileges is valid`() {

        /* TODO check category codes
        val validVehicleCategoryCodes = arrayOf(
            "AM", "A1", "A2", "A",
            "B1", "BE", "B",
            "C1", "CE", "C1E", "C",
            "D1", "DE", "D1E", "D"
        )*/

        val raw = testArrayMDLField(DRIVING_PRIVILEGES)
        val dataItems = CBORTestUtil.getDataItemArrayContent(raw!!)
        dataItems.forEach { dItem ->
            testDataItemMapMDL(dItem)
            val privilegeData = CBORTestUtil.getDrivingPrivilegeData(dItem)
            if (privilegeData.containsKey(ISSUE_DATE)) {
                assert(CBORTestUtil.checkFullDateFormat(CBORTestUtil.getStringValue(privilegeData[ISSUE_DATE]!!)))
            }
            if (privilegeData.containsKey(EXPIRY_DATE)) {
                assert(CBORTestUtil.checkFullDateFormat(CBORTestUtil.getStringValue(privilegeData[EXPIRY_DATE]!!)))
            }
            if (privilegeData.containsKey(CODES)) {
                testArrayMDLField(CODES)
            }
            assert(privilegeData[VEHICLE_CATEGORY_CODE]?.majorType == MajorType.UNICODE_STRING)
        }
    }

    @Test
    fun `un_distinguishing_sign is valid`() {
        testUnicodeStringMDLField(UN_DISTINGUISHING_SIGN)
    }
}
