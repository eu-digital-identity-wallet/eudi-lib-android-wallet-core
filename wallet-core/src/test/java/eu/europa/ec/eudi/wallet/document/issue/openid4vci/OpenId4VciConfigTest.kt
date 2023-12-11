/*
 *  Copyright (c) 2023 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet.document.issue.openid4vci

import org.junit.Assert
import org.junit.Test

class OpenId4VciConfigTest {

    @Test
    fun testBuilder() {
        val issuerUrl = "https://example.issuer.com"
        val clientId = "client-id"

        val config = OpenId4VciConfig.Builder()
            .withIssuerUrl(issuerUrl)
            .withClientId(clientId)
            .build()

        Assert.assertEquals(issuerUrl, config.issuerUrl)
        Assert.assertEquals(clientId, config.clientId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBuilderWithEmptyIssuerUrl() {
        OpenId4VciConfig.Builder()
            .withIssuerUrl("")
            .withClientId("client-id")
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBuilderWithEmptyClientId() {
        OpenId4VciConfig.Builder()
            .withIssuerUrl("https://example.issuer.com")
            .withClientId("")
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBuilderWithoutIssuerUrl() {
        OpenId4VciConfig.Builder()
            .withClientId("client-id")
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBuilderWithoutClientId() {
        OpenId4VciConfig.Builder()
            .withIssuerUrl("https://example.issuer.com")
            .build()
    }
}