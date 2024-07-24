package eu.europa.ec.eudi.wallet.issue.openid4vci

import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.europa.ec.eudi.openid4vci.AuthorizationRequestPrepared
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class DefaultBrowserAuthorizationHandler(
    private val context: Context
) : AuthorizationHandler {
    private var cancellableContinuation: CancellableContinuation<Result<AuthorizationResponse>>? =
        null

    /**
     * Resume the authorization flow after the user has been redirected back to the app
     * @param intent the intent that contains the authorization code
     * @throws [IllegalStateException] if no authorization request to resume
     *
     */
    fun resumeWithAuthorization(intent: Intent) {
        intent.data?.let { handleUri(it) }
            ?: throw IllegalStateException("No authorization uri found")
    }

    /**
     * Resume the authorization flow after the user has been redirected back to the app
     * @param uri the uri that contains the authorization code
     * @throws [IllegalStateException] if no authorization request to resume
     *
     */
    fun resumeWithAuthorization(uri: String) {
        handleUri(Uri.parse(uri))
    }

    /**
     * Resume the authorization flow after the user has been redirected back to the app
     * @param uri the uri that contains the authorization code
     * @throws [IllegalStateException] if no authorization request to resume
     *
     */
    fun resumeWithAuthorization(uri: Uri) {
        handleUri(uri)
    }

    private fun handleUri(uri: Uri) {
        val code = uri.getQueryParameter("code")
            ?: throw IllegalStateException("No authorization code found")
        val state =
            uri.getQueryParameter("state") ?: throw IllegalStateException("No server state found")

        cancellableContinuation?.resume(
            Result.success(
                AuthorizationResponse(
                    code,
                    state,
                    "dpopNonce"
                )
            )
        )
    }

    override suspend fun doAuthorization(
        authorizationCodeRequest: AuthorizationRequestPrepared
    ): Result<AuthorizationResponse> {
        cancellableContinuation?.cancel()

        return suspendCancellableCoroutine { continuation ->
            cancellableContinuation = continuation
            val authorizationCodeUri =
                Uri.parse(authorizationCodeRequest.authorizationCodeURL.value.toString())

            context.startActivity(Intent(Intent.ACTION_VIEW, authorizationCodeUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    override fun cancelAuthorization() {
        cancellableContinuation?.cancel()
    }
}