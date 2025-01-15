/*
 * Copyright (c) 2023-2025 European Commission
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
import eu.europa.ec.eudi.wallet.sample.util.Constants.ADMINISTRATIVE_NUMBER
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_BIRTH_YEAR
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_IN_YEARS
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_OVER_15
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_OVER_18
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_OVER_21
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_OVER_60
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_OVER_65
import eu.europa.ec.eudi.wallet.sample.util.Constants.AGE_OVER_68
import eu.europa.ec.eudi.wallet.sample.util.Constants.BIRTH_DATE
import eu.europa.ec.eudi.wallet.sample.util.Constants.BIRTH_PLACE
import eu.europa.ec.eudi.wallet.sample.util.Constants.DOCUMENT_NUMBER
import eu.europa.ec.eudi.wallet.sample.util.Constants.DRIVING_PRIVILEGES
import eu.europa.ec.eudi.wallet.sample.util.Constants.EXPIRY_DATE
import eu.europa.ec.eudi.wallet.sample.util.Constants.EYE_COLOUR
import eu.europa.ec.eudi.wallet.sample.util.Constants.FAMILY_NAME
import eu.europa.ec.eudi.wallet.sample.util.Constants.FAMILY_NAME_NATIONAL_CHARACTER
import eu.europa.ec.eudi.wallet.sample.util.Constants.GIVEN_NAME
import eu.europa.ec.eudi.wallet.sample.util.Constants.GIVEN_NAME_NATIONAL_CHARACTER
import eu.europa.ec.eudi.wallet.sample.util.Constants.HAIR_COLOUR
import eu.europa.ec.eudi.wallet.sample.util.Constants.HEIGHT
import eu.europa.ec.eudi.wallet.sample.util.Constants.ISSUE_DATE
import eu.europa.ec.eudi.wallet.sample.util.Constants.ISSUING_AUTHORITY
import eu.europa.ec.eudi.wallet.sample.util.Constants.ISSUING_COUNTRY
import eu.europa.ec.eudi.wallet.sample.util.Constants.ISSUING_JURISDICTION
import eu.europa.ec.eudi.wallet.sample.util.Constants.NATIONALITY
import eu.europa.ec.eudi.wallet.sample.util.Constants.PORTRAIT
import eu.europa.ec.eudi.wallet.sample.util.Constants.PORTRAIT_CAPTURE_DATE
import eu.europa.ec.eudi.wallet.sample.util.Constants.RESIDENT_ADDRESS
import eu.europa.ec.eudi.wallet.sample.util.Constants.RESIDENT_CITY
import eu.europa.ec.eudi.wallet.sample.util.Constants.RESIDENT_COUNTRY
import eu.europa.ec.eudi.wallet.sample.util.Constants.RESIDENT_POSTAL_CODE
import eu.europa.ec.eudi.wallet.sample.util.Constants.RESIDENT_STATE
import eu.europa.ec.eudi.wallet.sample.util.Constants.SEX
import eu.europa.ec.eudi.wallet.sample.util.Constants.SIGNATURE_USUAL_MARK
import eu.europa.ec.eudi.wallet.sample.util.Constants.UN_DISTINGUISHING_SIGN
import eu.europa.ec.eudi.wallet.sample.util.Constants.WEIGHT
import org.junit.Before
import org.junit.Test
import java.util.Locale

class MDLAllFieldsTest : BaseTest() {

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
        testMDLField(DRIVING_PRIVILEGES)
    }

    @Test
    fun `un_distinguishing_sign is valid`() {
        testUnicodeStringMDLField(UN_DISTINGUISHING_SIGN)
    }

    @Test
    fun `administrative_number is valid`() {
        testUnicodeStringMDLField(ADMINISTRATIVE_NUMBER)
    }

    @Test
    fun `sex is valid`() {
        val field = SEX
        val value = testNumMDLField(field).toInt()
        assert(value == 0 || value == 1 || value == 2 || value == 9)
    }

    @Test
    fun `height is valid`() {
        testNumMDLField(HEIGHT)
    }

    @Test
    fun `weight is valid`() {
        testNumMDLField(WEIGHT)
    }

    @Test
    fun `eye_colour is valid`() {
        val raw = testMDLField(EYE_COLOUR)

        val validColors = arrayOf(
            "black", "blue",
            "brown", "dichromatic", "grey", "green",
            "hazel", "maroon", "pink", "unknown"
        )
        val dataValue = CBORTestUtil.getStringValue(raw!!)

        assert(validColors.contains(dataValue))
    }

    @Test
    fun `hair_colour is valid`() {
        val raw = testMDLField(HAIR_COLOUR, MajorType.UNICODE_STRING)

        val validColors = arrayOf(
            "bald", "black",
            "blond", "brown", "grey", "red", "auburn",
            "sandy", "white", "unknown"
        )
        val dataValue = CBORTestUtil.getStringValue(raw!!)

        assert(validColors.contains(dataValue))
    }

    @Test
    fun `birth_place is valid`() {
        testUnicodeStringMDLField(BIRTH_PLACE)
    }

    @Test
    fun `resident_address is valid`() {
        testUnicodeStringMDLField(RESIDENT_ADDRESS)
    }

    @Test
    fun `portrait_capture_date is valid`() {
        testTDateOrFullDateMDLField(PORTRAIT_CAPTURE_DATE)
    }

    @Test
    fun `signature_usual_mark is valid`() {
        testByteStringMdlField(SIGNATURE_USUAL_MARK)
    }

    @Test
    fun `age_in_years is valid`() {
        testNumMDLField(AGE_IN_YEARS)
    }

    @Test
    fun `age_birth_year is valid`() {
        testNumMDLField(AGE_BIRTH_YEAR)
    }

    @Test
    fun `issuing_jurisdiction is valid`() {
        testUnicodeStringMDLField(ISSUING_JURISDICTION)
    }

    @Test
    fun `nationality is valid`() {
        val countryCodes = Locale.getISOCountries()
        val countryCode = testUnicodeStringMDLField(NATIONALITY)
        assert(countryCodes.contains(countryCode))
    }

    @Test
    fun `resident_city is valid`() {
        testUnicodeStringMDLField(RESIDENT_CITY)
    }

    @Test
    fun `resident_state is valid`() {
        testUnicodeStringMDLField(RESIDENT_STATE)
    }

    @Test
    fun `resident_postal_code is valid`() {
        testUnicodeStringMDLField(RESIDENT_POSTAL_CODE)
    }

    @Test
    fun `resident_country is valid`() {
        testUnicodeStringMDLField(RESIDENT_COUNTRY)
    }

    @Test
    fun `family_name_national_character is valid`() {
        testUnicodeStringMDLField(FAMILY_NAME_NATIONAL_CHARACTER)
    }

    @Test
    fun `given_name_national_character is valid`() {
        testUnicodeStringMDLField(GIVEN_NAME_NATIONAL_CHARACTER)
    }

    @Test
    fun `age_over_15 is valid`() {
        testBooleanMDLField(AGE_OVER_15)
    }

    @Test
    fun `age_over_18 is valid`() {
        testBooleanMDLField(AGE_OVER_18)
    }

    @Test
    fun `age_over_21 is valid`() {
        testBooleanMDLField(AGE_OVER_21)
    }

    @Test
    fun `age_over_60 is valid`() {
        testBooleanMDLField(AGE_OVER_60)
    }

    @Test
    fun `age_over_65 is valid`() {
        testBooleanMDLField(AGE_OVER_65)
    }

    @Test
    fun `age_over_68 is valid`() {
        testBooleanMDLField(AGE_OVER_68)
    }

}
