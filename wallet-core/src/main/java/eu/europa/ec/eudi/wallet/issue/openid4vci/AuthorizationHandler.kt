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
 * to the user and obtaining the authorization response (code, state and, when present, `iss`).
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
     * 3. Return the authorization code, the server state and the `iss` parameter of the
     *    authorization response, if it has one
     *
     * The `iss` parameter ([RFC 9207](https://www.rfc-editor.org/rfc/rfc9207)) must be passed on
     * in [AuthorizationResponse.issuer]. By default
     * ([OpenId4VciManager.Config.authResponseIssChecking]), an authorization server that advertises
     * `authorization_response_iss_parameter_supported` is only trusted when its response carries
     * a matching `iss`, so a handler that drops it makes issuance from such a server fail.
     *
     * @param authorizationUrl The URL to present to the user for authorization
     * @return Result containing the [AuthorizationResponse] with authorization code, server state
     *         and issuer, or a failure if authorization fails or is cancelled
     */
    suspend fun authorize(authorizationUrl: String): Result<AuthorizationResponse>
}

/**
 * Response from the authorization flow containing the authorization code, server state and,
 * when the authorization server returns it, its issuer identifier.
 *
 * @property authorizationCode The authorization code received from the authorization server
 * @property serverState The state parameter from the authorization server used for CSRF protection
 * @property issuer The `iss` parameter of the authorization response ([RFC 9207](https://www.rfc-editor.org/rfc/rfc9207)),
 *           or null when the response has none. It is checked against the authorization server's
 *           issuer identifier according to [OpenId4VciManager.Config.authResponseIssChecking], to
 *           prevent mix-up attacks. Custom [AuthorizationHandler]s should pass it on.
 */
data class AuthorizationResponse @JvmOverloads constructor(
    val authorizationCode: String,
    val serverState: String,
    val issuer: String? = null,
)

