package eu.europa.ec.eudi.wallet.issue.openid4vci.reissue

/**
 * Exception thrown during credential re-issuance when the stored tokens (access token and
 * refresh token) have expired and user authorization is required to obtain fresh tokens.
 *
 * This exception is only thrown when [eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.reissueDocument] is called with
 * `allowAuthorizationFallback = false` (background re-issuance mode). In this mode,
 * the library does not attempt to open a browser for interactive authorization.
 *
 * The wallet-ui can check for this exception in [eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Failure.cause] to distinguish
 * authorization failures from other errors and schedule an interactive re-authorization later.
 *
 * @param message A description of the authorization failure
 * @param cause The underlying cause, if any
 */
class ReissuanceAuthorizationException(
    message: String = "Re-issuance requires user authorization",
    cause: Throwable? = null,
) : Exception(message, cause)