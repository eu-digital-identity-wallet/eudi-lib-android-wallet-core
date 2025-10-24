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

import android.net.Uri
import eu.europa.ec.eudi.openid4vci.AuthorizationCode
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger

/**
 * Authorizes an [Issuer] and provides the authorization code.
 * @property authorizationHandler The handler for authorization requests
 */
internal class IssuerAuthorization(
    private val authorizationHandler: AuthorizationHandler,
    private val logger: Logger? = null,
) {

    /**
     * Authorizes the given [Issuer] and returns the authorized request.
     * If txCode is provided, it will be used to authorize the issuer,
     * otherwise the authorization handler will be used for user authorization
     * @param issuer The issuer to authorize.
     * @param txCode The pre-authorization code.
     */
    suspend fun authorize(issuer: Issuer, txCode: String?): AuthorizedRequest {
        return with(issuer) {
            when {
                isPreAuthorized() -> authorizeWithPreAuthorizationCode(txCode)

                else -> {
                    val prepareAuthorizationCodeRequest = prepareAuthorizationRequest().getOrThrow()
                    val authorizationUrl = prepareAuthorizationCodeRequest.authorizationCodeURL.value.toString()
                    val authResponse = authorizationHandler.authorize(authorizationUrl).getOrThrow()
                    prepareAuthorizationCodeRequest.authorizeWithAuthorizationCode(
                        AuthorizationCode(authResponse.authorizationCode),
                        authResponse.serverState
                    )
                }
            }.getOrThrow()
        }
    }

    /**
     * Resumes the authorization from the given [Uri].
     * This method delegates to the [BrowserAuthorizationHandler] if it's being used.
     * @throws IllegalStateException if the authorization handler is not a [BrowserAuthorizationHandler]
     */
    fun resumeFromUri(uri: Uri) {
        logger?.d(TAG, "IssuerAuthorization.resumeFromUri($uri)")
        when (authorizationHandler) {
            is BrowserAuthorizationHandler -> authorizationHandler.resumeWithUri(uri)
            else -> {
                val exception = IllegalStateException(
                    "resumeFromUri is only supported with BrowserAuthorizationHandler. " +
                            "Custom authorization handlers should manage their own flow."
                )
                logger?.e(TAG, "IssuerAuthorization.resumeFromUri failed", exception)
                throw exception
            }
        }
    }

    companion object {
        /**
         * Checks if the issuer's credential offer is pre-authorized.
         * @receiver The issuer to check.
         */
        fun Issuer.isPreAuthorized(): Boolean = credentialOffer.grants?.preAuthorizedCode() != null
    }
}