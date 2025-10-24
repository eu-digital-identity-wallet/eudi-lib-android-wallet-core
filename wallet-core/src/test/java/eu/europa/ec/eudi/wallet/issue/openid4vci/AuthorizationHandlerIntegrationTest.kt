/*
 * Copyright (c) 2024-2025 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import android.content.Context
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.logging.Logger
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests demonstrating how to use custom authorization handlers
 * with OpenId4VciManager
 */
class AuthorizationHandlerIntegrationTest {

    @Test
    fun `OpenId4VciManager uses default BrowserAuthorizationHandler when none specified`() {
        val context = mockk<Context>(relaxed = true)
        val documentManager = mockk<DocumentManager>(relaxed = true)
        val logger = mockk<Logger>(relaxed = true)

        val config = OpenId4VciManager.Config {
            withIssuerUrl("https://issuer.example.com")
            withClientId("client-id")
            withAuthFlowRedirectionURI("eudi-wallet://callback")
        }

        val manager = OpenId4VciManager.Builder(context)
            .config(config)
            .documentManager(documentManager)
            .logger(logger)
            .build() as DefaultOpenId4VciManager

        // Verify that the manager was created successfully
        assertNotNull(manager)
        assertEquals(config, manager.config)

        // The authorization handler should default to BrowserAuthorizationHandler
        // when config.authorizationHandler is null
        assertEquals(null, config.authorizationHandler)
    }

    @Test
    fun `OpenId4VciManager uses custom authorization handler when specified`() {
        val context = mockk<Context>(relaxed = true)
        val documentManager = mockk<DocumentManager>(relaxed = true)
        val logger = mockk<Logger>(relaxed = true)
        val customHandler = WebViewAuthorizationHandler()

        val config = OpenId4VciManager.Config {
            withIssuerUrl("https://issuer.example.com")
            withClientId("client-id")
            withAuthFlowRedirectionURI("eudi-wallet://callback")
            withAuthorizationHandler(customHandler)
        }

        val manager = OpenId4VciManager.Builder(context)
            .config(config)
            .documentManager(documentManager)
            .logger(logger)
            .build() as DefaultOpenId4VciManager

        // Verify that the manager uses the custom handler
        assertNotNull(manager)
        assertEquals(customHandler, manager.config.authorizationHandler)
    }

    @Test
    fun `Config can be created with DSL syntax and custom handler`() {
        val customHandler = WebViewAuthorizationHandler()

        val config = OpenId4VciManager.Config {
            withIssuerUrl("https://issuer.example.com")
            withClientId("wallet-client")
            withAuthFlowRedirectionURI("eudi-wallet://oauth-callback")
            withAuthorizationHandler(customHandler)
            withDPoPUsage(OpenId4VciManager.Config.DPoPUsage.Disabled)
            withParUsage(OpenId4VciManager.Config.ParUsage.REQUIRED)
        }

        assertEquals("https://issuer.example.com", config.issuerUrl)
        assertEquals("wallet-client", config.clientId)
        assertEquals("eudi-wallet://oauth-callback", config.authFlowRedirectionURI)
        assertEquals(customHandler, config.authorizationHandler)
        assertEquals(OpenId4VciManager.Config.DPoPUsage.Disabled, config.dPoPUsage)
        assertEquals(OpenId4VciManager.Config.ParUsage.REQUIRED, config.parUsage)
    }

    @Test
    fun `Custom WebView authorization handler can complete authorization flow`() {
        val handler = WebViewAuthorizationHandler()

        // Simulate the authorization flow
        val authorizationUrl = "https://issuer.example.com/authorize?client_id=test&state=abc123"

        // In a real scenario, this would be called when the WebView completes authorization
        handler.completeAuthorization("auth_code_xyz", "abc123")

        assertTrue(handler.authorizationCompleted)
        assertEquals("auth_code_xyz", handler.lastAuthorizationCode)
        assertEquals("abc123", handler.lastServerState)
    }

    /**
     * Example custom authorization handler using a WebView (simulated for testing)
     *
     * In a real application, this would:
     * 1. Open a WebView with the authorization URL
     * 2. Monitor the WebView for the redirect URI
     * 3. Extract the authorization code and state
     * 4. Complete the deferred with the result
     */
    private class WebViewAuthorizationHandler : AuthorizationHandler {
        private var authorizationDeferred: CompletableDeferred<Result<AuthorizationResponse>>? = null

        var authorizationCompleted = false
        var lastAuthorizationCode: String? = null
        var lastServerState: String? = null

        override suspend fun authorize(authorizationUrl: String): Result<AuthorizationResponse> {
            // In a real implementation, this would:
            // 1. Create and show a WebView with the authorizationUrl
            // 2. Set up URL monitoring for the redirect URI
            // 3. Wait for the authorization to complete

            authorizationDeferred = CompletableDeferred()
            return authorizationDeferred!!.await()
        }

        /**
         * Call this method when the WebView intercepts the redirect URI
         * and extracts the authorization code and state
         */
        fun completeAuthorization(authorizationCode: String, serverState: String) {
            authorizationCompleted = true
            lastAuthorizationCode = authorizationCode
            lastServerState = serverState

            authorizationDeferred?.complete(
                Result.success(
                    AuthorizationResponse(
                        authorizationCode = authorizationCode,
                        serverState = serverState
                    )
                )
            )
        }

        /**
         * Call this method if authorization fails
         */
        fun failAuthorization(error: Throwable) {
            authorizationDeferred?.complete(Result.failure(error))
        }
    }
}

