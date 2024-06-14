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

import android.content.Intent
import android.net.Uri
import android.util.Log
import eu.europa.ec.eudi.wallet.issue.openid4vci.DefaultOpenId4VciManager.Companion.TAG
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import java.io.Closeable
import kotlin.coroutines.resume

/**
 * Suspended authorization. It is used to resume the authorization process.
 */
internal class SuspendedAuthorization(
    private val continuation: CancellableContinuation<Result<AuthorizationResponse>>,
) : Closeable {

    /**
     * Resumes the authorization process from the given uri.
     * @param uri the uri
     * @throws Throwable if the uri is invalid
     */
    fun resumeFromUri(uri: String) {
        try {
            resumeFromUri(Uri.parse(uri))
        } catch (e: Throwable) {
            continuation.resumeWith(Result.failure(e))
        }
    }

    /**
     * Resumes the authorization process from the given uri.
     * @param uri the uri
     * @throws Throwable if the uri is invalid
     */
    fun resumeFromUri(uri: Uri) {
        Log.d(TAG, "resumeFromUri: $uri")
        try {
            uri.getQueryParameter("code")?.let { authorizationCode ->
                uri.getQueryParameter("state")?.let { serverState ->
                    continuation.resume(Result.success(AuthorizationResponse(authorizationCode, serverState)))
                } ?: "No server state found".let { msg ->
                    Log.e(TAG, "resumeFromUri: msg")
                    continuation.resumeWith(Result.failure(IllegalStateException(msg)))
                }
            } ?: "No authorization code found".let { msg ->
                Log.e(TAG, "resumeFromUri: msg")
                continuation.resumeWith(Result.failure(IllegalStateException(msg)))
            }
        } catch (e: Throwable) {
            Log.e(TAG, "resumeFromUri exception", e)
            continuation.resumeWith(Result.failure(e))
        }
    }

    /**
     * Resumes the authorization process from the given intent.
     * @param intent the intent
     * @throws Throwable if the intent is invalid
     */
    fun resumeFromIntent(intent: Intent) {
        intent.data?.let { resumeFromUri(it) }
            ?: continuation.resumeWith(Result.failure(IllegalStateException("No uri found in intent")))
    }

    /**
     * Cancels the wrapped continuation.
     */
    override fun close() {
        continuation.takeIf { it.isActive }?.cancel(CancellationException("Authorization was cancelled"))
    }
}