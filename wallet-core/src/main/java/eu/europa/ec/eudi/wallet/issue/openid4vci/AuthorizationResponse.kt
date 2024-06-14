package eu.europa.ec.eudi.wallet.issue.openid4vci

/**
 * Response of the authorization process.
 * @property authorizationCode the authorization code
 * @property serverState the server state
 * @constructor Creates a new [AuthorizationResponse] instance.
 * @param authorizationCode the authorization code
 * @param serverState the server state
 */
public data class AuthorizationResponse(val authorizationCode: String, val serverState: String)