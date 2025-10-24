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
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import androidx.core.net.toUri
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume

/**
 * Default implementation of [AuthorizationHandler] that opens a browser for user authorization.
 *
 * This handler:
 * 1. Opens the authorization URL in the system browser
 * 2. Waits for the app to receive the authorization callback via deep link
 * 3. Extracts the authorization code and state from the callback URI
 *
 * @property context Android context used to start the browser activity
 * @property logger Optional logger for debugging
 */
class BrowserAuthorizationHandler(
    private val context: Context,
    private val logger: Logger? = null,
) : AuthorizationHandler {

    private var continuation: CancellableContinuation<Result<AuthorizationResponse>>? = null

    override suspend fun authorize(authorizationUrl: String): Result<AuthorizationResponse> {
        cancel() // close any previous suspensions
        val authorizationCodeUri = authorizationUrl.toUri()
        return suspendCancellableCoroutine { cont ->
            continuation = cont
            cont.invokeOnCancellation { continuation = null }
            context.startActivity(Intent(ACTION_VIEW, authorizationCodeUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    /**
     * Resumes the authorization from the given [Uri].
     * This should be called when the app receives the authorization callback via deep link.
     *
     * This method extracts the authorization code and state from the callback URI and
     * completes the suspended authorization coroutine with the result.
     *
     * The suspended [authorize] coroutine will receive:
     * - A successful [Result] containing [AuthorizationResponse] if both 'code' and 'state' parameters are present
     * - A failed [Result] with [IllegalArgumentException] if the authorization code parameter ('code') is missing from the URI
     * - A failed [Result] with [IllegalArgumentException] if the server state parameter ('state') is missing from the URI
     *
     * @param uri The callback URI containing the authorization code and state parameters
     * @throws IllegalStateException if no authorization is in progress
     *
     * @see authorize
     */
    fun resumeWithUri(uri: Uri) {
        logger?.d(TAG, "BrowserAuthorizationHandler.resumeWithUri($uri)")
        continuation?.let { cont ->
            val response = runCatching {
                val authorizationCode = uri.getQueryParameter("code")
                val serverState = uri.getQueryParameter("state")

                requireNotNull(authorizationCode) { "No authorization code found" }
                requireNotNull(serverState) { "No server state found" }

                AuthorizationResponse(authorizationCode, serverState)
            }
            cont.resume(response.onFailure {
                logger?.e(TAG, "resumeWithUri: ${it.message}", it)
            })
        } ?: throw IllegalStateException("No suspended authorization found").also {
            logger?.e(TAG, "BrowserAuthorizationHandler.resumeWithUri failed", it)
        }
    }

    /**
     * Cancels any ongoing authorization request.
     */
    fun cancel() {
        continuation?.cancel(
            cause = CancellationException("Authorization was cancelled")
        )
        continuation = null
    }
}

