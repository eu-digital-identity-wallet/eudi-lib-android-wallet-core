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

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import eu.europa.ec.eudi.openid4vci.AuthorizationCode
import eu.europa.ec.eudi.openid4vci.AuthorizationRequestPrepared
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.logging.d
import eu.europa.ec.eudi.wallet.logging.e
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

/**
 * Authorizes an [Issuer] and provides the authorization code.
 * @property continuation The continuation for the authorization.
 */
internal class IssuerAuthorization(
    private val context: Context,
    private val logger: Logger? = null,
) {

    var continuation: CancellableContinuation<Result<Response>>? = null

    suspend fun performPushAuthorizationRequest(issuer: Issuer): AuthorizationRequestPrepared {
        return issuer.prepareAuthorizationRequest().getOrThrow()
    }

    /**
     * Authorizes the given [Issuer] and returns the authorized request.
     * If txCode is provided, it will be used to authorize the issuer,
     * otherwise the browser will be opened for user authorization
     * @param issuer The issuer to authorize.
     * @param txCode The pre-authorization code.
     */
    suspend fun authorize(issuer: Issuer, txCode: String?): AuthorizedRequest {
        close() // close any previous suspensions
        return with(issuer) {
            when {
                isPreAuthorized() -> authorizeWithPreAuthorizationCode(txCode)

                else -> {
                    val prepareAuthorizationCodeRequest = prepareAuthorizationRequest().getOrThrow()
                    val authResponse = openBrowserForAuthorization(prepareAuthorizationCodeRequest).getOrThrow()
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
     */
    fun resumeFromUri(uri: Uri) {
        logger?.d(TAG, "IssuerAuthorization.resumeFromUri($uri)")
        continuation?.let { cont ->
            try {
                uri.getQueryParameter("code")?.let { authorizationCode ->
                    uri.getQueryParameter("state")?.let { serverState ->
                        cont.resume(Result.success(Response(authorizationCode, serverState)))
                    } ?: "No server state found".let { msg ->
                        val exception = IllegalStateException(msg)
                        logger?.e(TAG, "resumeFromUri: $msg", exception)
                        cont.resume(Result.failure(exception))
                    }
                } ?: "No authorization code found".let { msg ->
                    val exception = IllegalStateException(msg)
                    logger?.e(TAG, "resumeFromUri: $msg", exception)
                    cont.resume(Result.failure(exception))
                }
            } catch (e: Throwable) {
                logger?.e(TAG, "resumeFromUri: ${e.message}", e)
                cont.resume(Result.failure(e))
            }
        } ?: run {
            val exception = IllegalStateException("No suspended authorization found")
            logger?.e(TAG, "IssuerAuthorization.resumeFromUri failed", exception)
            throw exception
        }

    }

    /**
     * Cancels the continuation.
     */
    fun close() {
        continuation?.cancel(CancellationException("Authorization was cancelled"))
        continuation = null
    }

    /**
     * Opens a browser for authorization.
     * @param prepareAuthorizationCodeRequest The prepared authorization request.
     * @return The authorization response wrapped in a [Result].
     */
    suspend fun openBrowserForAuthorization(prepareAuthorizationCodeRequest: AuthorizationRequestPrepared): Result<Response> {
        val authorizationCodeUri =
            Uri.parse(prepareAuthorizationCodeRequest.authorizationCodeURL.value.toString())
        return suspendCancellableCoroutine { cont ->
            continuation = cont
            cont.invokeOnCancellation { continuation = null }
            context.startActivity(Intent(ACTION_VIEW, authorizationCodeUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    data class Response(val authorizationCode: String, val serverState: String)

    companion object {
        /**
         * Checks if the issuer's credential offer is pre-authorized.
         * @receiver The issuer to check.
         */
        fun Issuer.isPreAuthorized(): Boolean = credentialOffer.grants?.preAuthorizedCode() != null
    }
}