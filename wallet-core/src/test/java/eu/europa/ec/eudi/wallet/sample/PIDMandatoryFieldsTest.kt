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

import eu.europa.ec.eudi.wallet.sample.util.BaseTest
import eu.europa.ec.eudi.wallet.sample.util.Constants.BIRTH_DATE
import eu.europa.ec.eudi.wallet.sample.util.Constants.EXPIRY_DATE
import eu.europa.ec.eudi.wallet.sample.util.Constants.FAMILY_NAME
import eu.europa.ec.eudi.wallet.sample.util.Constants.GIVEN_NAME
import eu.europa.ec.eudi.wallet.sample.util.Constants.ISSUANCE_DATE
import eu.europa.ec.eudi.wallet.sample.util.Constants.ISSUING_AUTHORITY
import eu.europa.ec.eudi.wallet.sample.util.Constants.ISSUING_COUNTRY
import org.junit.Before
import org.junit.Test

class PIDMandatoryFieldsTest : BaseTest() {

    @Before
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun `family_name is valid`() {
        testUnicodeStringPIDField(FAMILY_NAME)
    }

    @Test
    fun `given_name is valid`() {
        testUnicodeStringPIDField(GIVEN_NAME)
    }

    @Test
    fun `birth_date is valid`() {
        testFullDatePIDField(BIRTH_DATE)
    }

    @Test
    fun `issue_date is valid`() {
        testTDateOrFullDatePIDField(ISSUANCE_DATE)
    }

    @Test
    fun `expiry_date is valid`() {
        testTDateOrFullDatePIDField(EXPIRY_DATE)
    }

    @Test
    fun `issuing_authority is valid`() {
        testUnicodeStringPIDField(ISSUING_AUTHORITY)
    }

    @Test
    fun `issuing_country is valid`() {
        testUnicodeStringPIDField(ISSUING_COUNTRY)
    }

}
