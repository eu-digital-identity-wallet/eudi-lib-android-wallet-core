package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.AuthorizationRequestPrepared

interface AuthorizationHandler {
    suspend fun doAuthorization(
        authorizationCodeRequest: AuthorizationRequestPrepared
    ): Result<AuthorizationResponse>

    fun cancelAuthorization()
}