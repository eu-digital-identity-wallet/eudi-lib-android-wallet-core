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

package eu.europa.ec.eudi.wallet.documentsTest

import eu.europa.ec.eudi.wallet.documentsTest.util.BaseTest
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.ADMINISTRATIVE_NUMBER
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.AGE_BIRTH_YEAR
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.AGE_IN_YEARS
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.AGE_OVER_15
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.AGE_OVER_18
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.AGE_OVER_21
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.AGE_OVER_60
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.AGE_OVER_65
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.AGE_OVER_68
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.BIRTH_CITY
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.BIRTH_COUNTRY
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.BIRTH_DATE
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.BIRTH_PLACE
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.BIRTH_STATE
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.DOCUMENT_NUMBER
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.EXPIRY_DATE
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.FAMILY_NAME
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.FAMILY_NAME_BIRTH
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.GENDER
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.GIVEN_NAME
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.GIVEN_NAME_BIRTH
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.ISSUANCE_DATE
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.ISSUING_AUTHORITY
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.ISSUING_COUNTRY
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.ISSUING_JURISDICTION
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.NATIONALITY
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.PORTRAIT
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.PORTRAIT_CAPTURE_DATE
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.RESIDENT_ADDRESS
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.RESIDENT_CITY
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.RESIDENT_COUNTRY
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.RESIDENT_HOUSE_NUMBER
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.RESIDENT_POSTAL_CODE
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.RESIDENT_STATE
import eu.europa.ec.eudi.wallet.documentsTest.util.Constants.RESIDENT_STREET
import org.junit.Before
import org.junit.Test

class PIDAllFieldsTest : BaseTest() {

    @Before
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun `family_name is valid`() {
        testPIDField(FAMILY_NAME)
    }

    @Test
    fun `given_name is valid`() {
        testPIDField(GIVEN_NAME)
    }

    @Test
    fun `birth_date is valid`() {
        testPIDField(BIRTH_DATE)
    }

    @Test
    fun `family_name_birth is valid`() {
        testPIDField(FAMILY_NAME_BIRTH)
    }

    @Test
    fun `given_name_birth is valid`() {
        testPIDField(GIVEN_NAME_BIRTH)
    }

    @Test
    fun `birth_place is valid`() {
        testPIDField(BIRTH_PLACE)
    }

    @Test
    fun `birth_country is valid`() {
        testPIDField(BIRTH_COUNTRY)
    }

    @Test
    fun `birth_city is valid`() {
        testPIDField(BIRTH_CITY)
    }

    @Test
    fun `birth_state is valid`() {
        testPIDField(BIRTH_STATE)
    }

    @Test
    fun `resident_address is valid`() {
        testPIDField(RESIDENT_ADDRESS)
    }

    @Test
    fun `resident_city is valid`() {
        testPIDField(RESIDENT_CITY)
    }

    @Test
    fun `resident_postal_code is valid`() {
        testPIDField(RESIDENT_POSTAL_CODE)
    }

    @Test
    fun `resident_state is valid`() {
        testPIDField(RESIDENT_STATE)
    }

    @Test
    fun `resident_country is valid`() {
        testPIDField(RESIDENT_COUNTRY)
    }

    @Test
    fun `resident_street is valid`() {
        testPIDField(RESIDENT_STREET)
    }

    @Test
    fun `resident_house_number is valid`() {
        testPIDField(RESIDENT_HOUSE_NUMBER)
    }

    @Test
    fun `gender is valid`() {
        testPIDField(GENDER)
    }

    @Test
    fun `nationality is valid`() {
        testPIDField(NATIONALITY)
    }

    @Test
    fun `age_over_15 is valid`() {
        testPIDField(AGE_OVER_15)
    }

    @Test
    fun `age_over_18 is valid`() {
        testPIDField(AGE_OVER_18)
    }

    @Test
    fun `age_over_21 is valid`() {
        testPIDField(AGE_OVER_21)
    }

    @Test
    fun `age_over_60 is valid`() {
        testPIDField(AGE_OVER_60)
    }

    @Test
    fun `age_over_65 is valid`() {
        testPIDField(AGE_OVER_65)
    }

    @Test
    fun `age_over_68 is valid`() {
        testPIDField(AGE_OVER_68)
    }

    @Test
    fun `age_in_years is valid`() {
        testPIDField(AGE_IN_YEARS)
    }

    @Test
    fun `age_birth_year is valid`() {
        testPIDField(AGE_BIRTH_YEAR)
    }

    @Test
    fun `issue_date is valid`() {
        testPIDField(ISSUANCE_DATE)
    }

    @Test
    fun `expiry_date is valid`() {
        testPIDField(EXPIRY_DATE)
    }

    @Test
    fun `issuing_authority is valid`() {
        testPIDField(ISSUING_AUTHORITY)
    }

    @Test
    fun `document_number is valid`() {
        testPIDField(DOCUMENT_NUMBER)
    }

    @Test
    fun `administrative_number is valid`() {
        testPIDField(ADMINISTRATIVE_NUMBER)
    }

    @Test
    fun `issuing_country is valid`() {
        testPIDField(ISSUING_COUNTRY)
    }

    @Test
    fun `issuing_jurisdiction is valid`() {
        testPIDField(ISSUING_JURISDICTION)
    }

    @Test
    fun `portrait is valid`() {
        testPIDField(PORTRAIT)
    }

    @Test
    fun `portrait_capture_date is valid`() {
        testPIDField(PORTRAIT_CAPTURE_DATE)
    }

}
