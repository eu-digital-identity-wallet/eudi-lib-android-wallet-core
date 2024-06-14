package eu.europa.ec.eudi.wallet.issue.openid4vci

import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.europa.ec.eudi.openid4vci.AuthorizationRequestPrepared
import kotlinx.coroutines.suspendCancellableCoroutine

class DefaultBrowserAuthorizationHandler(
    private val context: Context
) : AuthorizationHandler {
    private var suspendedAuthorization: SuspendedAuthorization? = null

    /**
     * Resume the authorization flow after the user has been redirected back to the app
     * @param intent the intent that contains the authorization code
     * @throws [IllegalStateException] if no authorization request to resume
     *
     */
    fun resumeWithAuthorization(intent: Intent) {
        suspendedAuthorization?.use { it.resumeFromIntent(intent) }
            ?: throw IllegalStateException("No authorization request to resume")
    }

    /**
     * Resume the authorization flow after the user has been redirected back to the app
     * @param uri the uri that contains the authorization code
     * @throws [IllegalStateException] if no authorization request to resume
     *
     */
    fun resumeWithAuthorization(uri: String) {
        suspendedAuthorization?.use { it.resumeFromUri(uri) }
            ?: throw IllegalStateException("No authorization request to resume")
    }

    /**
     * Resume the authorization flow after the user has been redirected back to the app
     * @param uri the uri that contains the authorization code
     * @throws [IllegalStateException] if no authorization request to resume
     *
     */
    fun resumeWithAuthorization(uri: Uri) {
        suspendedAuthorization?.use { it.resumeFromUri(uri) }
            ?: throw IllegalStateException("No authorization request to resume")
    }

    override suspend fun doAuthorization(
        authorizationCodeRequest: AuthorizationRequestPrepared
    ): Result<AuthorizationResponse> {
        return suspendCancellableCoroutine { continuation ->
            suspendedAuthorization = SuspendedAuthorization(continuation)
            continuation.invokeOnCancellation {
                suspendedAuthorization = null
            }

            val authorizationCodeUri = Uri.parse(authorizationCodeRequest.authorizationCodeURL.value.toString())

            context.startActivity(Intent(Intent.ACTION_VIEW, authorizationCodeUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    override fun cancelAuthorization() {
        suspendedAuthorization?.close()
    }
}