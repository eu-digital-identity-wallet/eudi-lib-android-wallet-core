/*
 *  Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import org.junit.Assert.*
import org.junit.Test

class OpenId4VciManagerConfigBuilderTest {

    @Test
    fun `ConfigBuilder builds Config with valid issuerUrl, clientId and authFlowRedirectionURI`() {
        val builder = OpenId4VciManager.Config.Builder()
            .withIssuerUrl("https://issuer.example.com")
            .withClientId("testClientId")
            .withAuthFlowRedirectionURI("app://redirect")

        val config = builder.build()

        assertNotNull(config)
    }

    @Test
    fun `ConfigBuilder throws exception when issuerUrl is not set`() {
        val builder = OpenId4VciManager.Config.Builder()
            .clientId("testClientId")
            .authFlowRedirectionURI("app://redirect")

        assertThrows(IllegalStateException::class.java) {
            builder.build()
        }
    }

    @Test
    fun `ConfigBuilder throws exception when clientId is not set`() {
        val builder = OpenId4VciManager.Config.Builder()
            .issuerUrl("https://issuer.example.com")
            .authFlowRedirectionURI("app://redirect")

        assertThrows(IllegalStateException::class.java) {
            builder.build()
        }
    }

    @Test
    fun `ConfigBuilder throws exception when authFlowRedirectionURI is not set`() {
        val builder = OpenId4VciManager.Config.Builder()
            .issuerUrl("https://issuer.example.com")
            .clientId("testClientId")

        assertThrows(IllegalStateException::class.java) {
            builder.build()
        }
    }

    @Test
    fun `ConfigBuilder sets issuerUrl correctly`() {
        val builder = OpenId4VciManager.Config.Builder()
            .issuerUrl("https://issuer.example.com")
            .clientId("testClientId")
            .authFlowRedirectionURI("app://redirect")

        val config = builder.build()

        assertEquals("https://issuer.example.com", config.issuerUrl)
        assertEquals("testClientId", config.clientId)
        assertEquals("app://redirect", config.authFlowRedirectionURI)
        assertFalse(config.useStrongBoxIfSupported)
        assertFalse(config.useDPoPIfSupported)
    }

    @Test
    fun `ConfigBuilder sets useStrongBoxIfSupported correctly`() {
        val builder = OpenId4VciManager.Config.Builder()
            .issuerUrl("https://issuer.example.com")
            .clientId("testClientId")
            .authFlowRedirectionURI("app://redirect")
            .useStrongBoxIfSupported(true)

        val config = builder.build()

        assertTrue(config.useStrongBoxIfSupported)
    }

    @Test
    fun `ConfigBuilder sets useDPoPIfSupported correctly`() {
        val builder = OpenId4VciManager.Config.Builder()
            .issuerUrl("https://issuer.example.com")
            .clientId("testClientId")
            .authFlowRedirectionURI("app://redirect")
            .useDPoP(true)

        val config = builder.build()

        assertTrue(config.useDPoPIfSupported)
    }
}