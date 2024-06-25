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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume


internal class IssuerAuthorization(
    private val config: OpenId4VciManager.Config,
    private val context: Context,
    private val logger: Logger? = null,
) : Closeable {

    private var suspendedAuthorization: SuspendedAuthorization? = null

    suspend fun authorize(issuer: Issuer): AuthorizedRequest {
        close() // close any previous suspensions
        return with(issuer) {
            val prepareAuthorizationCodeRequest = prepareAuthorizationRequest().getOrThrow()
            val authResponse = openBrowserForAuthorization(prepareAuthorizationCodeRequest).getOrThrow()
            prepareAuthorizationCodeRequest.authorizeWithAuthorizationCode(
                AuthorizationCode(authResponse.authorizationCode),
                authResponse.serverState
            ).getOrThrow()
        }
    }

    fun resumeFromUri(uri: Uri) {
        logger?.d(TAG, "IssuerAuthorization.resumeFromUri($uri)")
        suspendedAuthorization?.use { it.resumeFromUri(uri) }
            ?: logger?.e(
                TAG,
                "${this::class.simpleName}.resumeFromUri failed",
                IllegalStateException("No suspended authorization found")
            )

    }

    override fun close() {
        suspendedAuthorization?.close()
        suspendedAuthorization = null
    }

    /**
     * Opens a browser for authorization.
     * @param prepareAuthorizationCodeRequest The prepared authorization request.
     * @return The authorization response wrapped in a [Result].
     */
    private suspend fun openBrowserForAuthorization(prepareAuthorizationCodeRequest: AuthorizationRequestPrepared): Result<SuspendedAuthorization.Response> {
        val authorizationCodeUri =
            Uri.parse(prepareAuthorizationCodeRequest.authorizationCodeURL.value.toString())
        return suspendCancellableCoroutine { continuation ->
            suspendedAuthorization = SuspendedAuthorization(continuation, logger)
            continuation.invokeOnCancellation {
                suspendedAuthorization = null
            }
            context.startActivity(Intent(ACTION_VIEW, authorizationCodeUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    /**
     * Suspended authorization. It is used to resume the authorization process.
     */
    class SuspendedAuthorization(
        val continuation: CancellableContinuation<Result<Response>>,
        val logger: Logger? = null,
    ) : Closeable {


        /**
         * Resumes the authorization process from the given uri.
         * @param uri the uri
         * @throws Throwable if the uri is invalid
         */
        fun resumeFromUri(uri: Uri) {
            try {
                uri.getQueryParameter("code")?.let { authorizationCode ->
                    uri.getQueryParameter("state")?.let { serverState ->
                        continuation.resume(Result.success(Response(authorizationCode, serverState)))
                    } ?: "No server state found".let { msg ->
                        val exception = IllegalStateException(msg)
                        logger?.e(TAG, "resumeFromUri: $msg", exception)
                        continuation.resume(Result.failure(exception))
                    }
                } ?: "No authorization code found".let { msg ->
                    val exception = IllegalStateException(msg)
                    logger?.e(TAG, "resumeFromUri: $msg", exception)
                    continuation.resume(Result.failure(exception))
                }
            } catch (e: Throwable) {
                logger?.e(TAG, "resumeFromUri: ${e.message}", e)
                continuation.resume(Result.failure(e))
            }
        }

        /**
         * Cancels the wrapped continuation.
         */
        override fun close() {
            continuation.takeIf { it.isActive }?.cancel(CancellationException("Authorization was cancelled"))
        }

        /**
         * Response of the authorization process.
         * @property authorizationCode the authorization code
         * @property serverState the server state
         * @constructor Creates a new [Response] instance.
         * @param authorizationCode the authorization code
         * @param serverState the server state
         */
        data class Response(val authorizationCode: String, val serverState: String)
    }
}