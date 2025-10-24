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

/**
 * Handler for authorization requests during the OpenID4VCI flow.
 *
 * Implementations of this interface are responsible for presenting the authorization URL
 * to the user and obtaining the authorization response (code and state).
 *
 * The default implementation [BrowserAuthorizationHandler] opens a browser for user authorization.
 * Custom implementations can provide alternative authorization flows (e.g., in-app WebView,
 * custom UI, embedded browsers).
 */
fun interface AuthorizationHandler {
    /**
     * Handles the authorization request by presenting the authorization URL to the user
     * and eventually returning the authorization response.
     *
     * This is a suspending function that should:
     * 1. Present the authorization URL to the user (e.g., open a browser)
     * 2. Wait for the user to complete authorization
     * 3. Return the authorization code and server state
     *
     * @param authorizationUrl The URL to present to the user for authorization
     * @return Result containing the [AuthorizationResponse] with authorization code and server state,
     *         or a failure if authorization fails or is cancelled
     */
    suspend fun authorize(authorizationUrl: String): Result<AuthorizationResponse>
}

/**
 * Response from the authorization flow containing the authorization code and server state.
 *
 * @property authorizationCode The authorization code received from the authorization server
 * @property serverState The state parameter from the authorization server used for CSRF protection
 */
data class AuthorizationResponse(
    val authorizationCode: String,
    val serverState: String,
)

