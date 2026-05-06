//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.reissue](../index.md)/[ReissuanceAuthorizationException](index.md)

# ReissuanceAuthorizationException

class [ReissuanceAuthorizationException](index.md)(message: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) = &quot;Re-issuance requires user authorization&quot;, cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)? = null) : [Exception](https://developer.android.com/reference/kotlin/java/lang/Exception.html)

Exception thrown during credential re-issuance when the stored tokens (access token and refresh token) have expired and user authorization is required to obtain fresh tokens.

This exception is only thrown when [eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.reissueDocument](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/reissue-document.md) is called with `allowAuthorizationFallback = false` (background re-issuance mode). In this mode, the library does not attempt to open a browser for interactive authorization.

The wallet-ui can check for this exception in [eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Failure.cause](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-issue-event/-failure/cause.md) to distinguish authorization failures from other errors and schedule an interactive re-authorization later.

#### Parameters

release

| | |
|---|---|
| message | A description of the authorization failure |
| cause | The underlying cause, if any |

## Constructors

| | |
|---|---|
| [ReissuanceAuthorizationException](-reissuance-authorization-exception.md) | [release]<br>constructor(message: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html) = &quot;Re-issuance requires user authorization&quot;, cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)? = null) |

## Properties

| Name | Summary |
|---|---|
| [cause](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#-654012527%2FProperties%2F-946843593) | [release]<br>open val [cause](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#-654012527%2FProperties%2F-946843593): [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)? |
| [message](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#1824300659%2FProperties%2F-946843593) | [release]<br>open val [message](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#1824300659%2FProperties%2F-946843593): [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? |