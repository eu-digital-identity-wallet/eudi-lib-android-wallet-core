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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import java.io.Closeable
import kotlin.coroutines.resume

internal class SuspendedAuthorization(
    private val continuation: CancellableContinuation<Result<Response>>,
) : Closeable {


    fun resumeFromUri(uri: String) {
        try {
            resumeFromUri(Uri.parse(uri))
        } catch (e: Throwable) {
            continuation.resumeWith(Result.failure(e))
        }
    }

    fun resumeFromUri(uri: Uri) {
        try {
            uri.getQueryParameter("code")?.let { authorizationCode ->
                uri.getQueryParameter("state")?.let { serverState ->
                    continuation.resume(Result.success(Response(authorizationCode, serverState)))
                } ?: continuation.resumeWith(Result.failure(IllegalStateException("No server state found")))
            } ?: continuation.resumeWith(Result.failure(IllegalStateException("No authorization code found")))
        } catch (e: Throwable) {
            continuation.resumeWith(Result.failure(e))
        }
    }

    fun resumeFromIntent(intent: Intent) {
        intent.data?.let { resumeFromUri(it) }
            ?: continuation.resumeWith(Result.failure(IllegalStateException("No uri found in intent")))
    }

    override fun close() {
        continuation.takeIf { it.isActive }?.cancel(CancellationException("Authorization was cancelled"))
    }

    data class Response(val authorizationCode: String, val serverState: String)
}