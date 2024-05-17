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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import java.io.Closeable
import kotlin.coroutines.resume

internal class SuspendedAuthorization(
    private val continuation: CancellableContinuation<Result<Response>>,
) : Closeable {

    fun resumeFromIntent(intent: Intent) {
        continuation.let { cont ->
            try {
                intent.data?.getQueryParameter("code")?.let { authorizationCode ->
                    intent.data?.getQueryParameter("state")?.let { serverState ->
                        cont.resume(Result.success(Response(authorizationCode, serverState)))
                    } ?: cont.resumeWith(Result.failure(IllegalStateException("No server state found")))
                } ?: cont.resumeWith(Result.failure(IllegalStateException("No authorization code found")))
            } catch (e: Throwable) {
                cont.resumeWith(Result.failure(e))
            }
        }
    }

    override fun close() {
        continuation.takeIf { it.isActive }?.cancel(CancellationException("Authorization was cancelled"))
    }

    data class Response(val authorizationCode: String, val serverState: String)
}