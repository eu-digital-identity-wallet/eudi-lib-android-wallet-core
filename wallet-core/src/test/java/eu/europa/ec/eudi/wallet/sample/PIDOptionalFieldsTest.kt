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

import eu.europa.ec.eudi.wallet.sample.util.BaseTest
import eu.europa.ec.eudi.wallet.sample.util.Constants
import eu.europa.ec.eudi.wallet.sample.util.Constants.ADMINISTRATIVE_NUMBER
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_BIRTH_YEAR
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_IN_YEARS
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_OVER_15
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_OVER_21
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_OVER_60
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_OVER_65
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_OVER_68
import eu.europa.ec.eudi.wallet.sample.util.Constants.BIRTH_PLACE
import eu.europa.ec.eudi.wallet.sample.util.Constants.FAMILY_NAME_BIRTH
import eu.europa.ec.eudi.wallet.sample.util.Constants.GENDER
import eu.europa.ec.eudi.wallet.sample.util.Constants.GIVEN_NAME_BIRTH
import eu.europa.ec.eudi.wallet.sample.util.Constants.ISSUING_JURISDICTION
import eu.europa.ec.eudi.wallet.sample.util.Constants.NATIONALITY
import eu.europa.ec.eudi.wallet.sample.util.Constants.PORTRAIT
import eu.europa.ec.eudi.wallet.sample.util.Constants.PORTRAIT_CAPTURE_DATE
import eu.europa.ec.eudi.wallet.sample.util.Constants.RESIDENT_ADDRESS
import eu.europa.ec.eudi.wallet.sample.util.Constants.RESIDENT_CITY
import eu.europa.ec.eudi.wallet.sample.util.Constants.RESIDENT_COUNTRY
import eu.europa.ec.eudi.wallet.sample.util.Constants.RESIDENT_POSTAL_CODE
import eu.europa.ec.eudi.wallet.sample.util.Constants.RESIDENT_STATE
import org.junit.Before
import org.junit.Test
import java.util.Locale

class PIDOptionalFieldsTest : BaseTest() {

    @Before
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun `family_name_birth is valid`() {
        testUnicodeStringPIDField(FAMILY_NAME_BIRTH)
    }

    @Test
    fun `given_name_birth is valid`() {
        testUnicodeStringPIDField(GIVEN_NAME_BIRTH)
    }

    @Test
    fun `birth_place is valid`() {
        testUnicodeStringPIDField(BIRTH_PLACE)
    }

    @Test
    fun `birth_country is valid`() {
        testUnicodeStringPIDField(Constants.BIRTH_COUNTRY)
    }

    @Test
    fun `birth_city is valid`() {
        testUnicodeStringPIDField(Constants.BIRTH_CITY)
    }

    @Test
    fun `birth_state is valid`() {
        testUnicodeStringPIDField(Constants.BIRTH_STATE)
    }

    @Test
    fun `resident_address is valid`() {
        testUnicodeStringPIDField(RESIDENT_ADDRESS)
    }

    @Test
    fun `resident_city is valid`() {
        testUnicodeStringPIDField(RESIDENT_CITY)
    }

    @Test
    fun `resident_postal_code is valid`() {
        testUnicodeStringPIDField(RESIDENT_POSTAL_CODE)
    }

    @Test
    fun `resident_state is valid`() {
        testUnicodeStringPIDField(RESIDENT_STATE)
    }

    @Test
    fun `resident_country is valid`() {
        testUnicodeStringPIDField(RESIDENT_COUNTRY)
    }

    @Test
    fun `resident_street is valid`() {
        testUnicodeStringPIDField(Constants.RESIDENT_STREET)
    }

    @Test
    fun `resident_house_number is valid`() {
        testUnicodeStringPIDField(Constants.RESIDENT_HOUSE_NUMBER)
    }

    @Test
    fun `gender is valid`() {
        testNumPIDField(GENDER)
    }

    @Test
    fun `nationality is valid`() {
        val countryCodes = Locale.getISOCountries()
        val countryCode = testUnicodeStringPIDField(NATIONALITY)
        assert(countryCodes.contains(countryCode))
    }

    @Test
    fun `age_over_15 is valid`() {
        testBooleanPIDField(AGE_OVER_15)
    }

    @Test
    fun `age_over_18 is valid`() {
        testBooleanPIDField(Constants.AGE_OVER_18)
    }

    @Test
    fun `age_over_21 is valid`() {
        testBooleanPIDField(AGE_OVER_21)
    }

    @Test
    fun `age_over_60 is valid`() {
        testBooleanPIDField(AGE_OVER_60)
    }

    @Test
    fun `age_over_65 is valid`() {
        testBooleanPIDField(AGE_OVER_65)
    }

    @Test
    fun `age_over_68 is valid`() {
        testBooleanPIDField(AGE_OVER_68)
    }

    @Test
    fun `age_in_years is valid`() {
        testNumPIDField(AGE_IN_YEARS)
    }

    @Test
    fun `age_birth_year is valid`() {
        testNumPIDField(AGE_BIRTH_YEAR)
    }

    @Test
    fun `document_number is valid`() {
        testUnicodeStringPIDField(Constants.DOCUMENT_NUMBER)
    }

    @Test
    fun `administrative_number is valid`() {
        testUnicodeStringPIDField(ADMINISTRATIVE_NUMBER)
    }

    @Test
    fun `issuing_jurisdiction is valid`() {
        testUnicodeStringPIDField(ISSUING_JURISDICTION)
    }

    @Test
    fun `portrait is valid`() {
        testByteStringPIDField(PORTRAIT)
    }

    @Test
    fun `portrait_capture_date is valid`() {
        testTDateOrFullDatePIDField(PORTRAIT_CAPTURE_DATE)
    }

}
