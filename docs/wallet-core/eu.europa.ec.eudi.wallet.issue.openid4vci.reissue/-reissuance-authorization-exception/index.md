//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.reissue](../index.md)/[ReissuanceAuthorizationException](index.md)

# ReissuanceAuthorizationException

class [ReissuanceAuthorizationException](index.md)(message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = &quot;Re-issuance requires user authorization&quot;, cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null) : [Exception](https://developer.android.com/reference/kotlin/java/lang/Exception.html)

Exception thrown during credential re-issuance when the stored tokens (access token and refresh token) have expired and user authorization is required to obtain fresh tokens.

This exception is only thrown when [eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.reissueDocument](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/reissue-document.md) is called with `allowAuthorizationFallback = false` (background re-issuance mode). In this mode, the library does not attempt to open a browser for interactive authorization.

The wallet-ui can check for this exception in [eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent.Failure.cause](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-issue-event/-failure/cause.md) to distinguish authorization failures from other errors and schedule an interactive re-authorization later.

#### Parameters

androidJvm

| | |
|---|---|
| message | A description of the authorization failure |
| cause | The underlying cause, if any |

## Constructors

| | |
|---|---|
| [ReissuanceAuthorizationException](-reissuance-authorization-exception.md) | [androidJvm]<br>constructor(message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = &quot;Re-issuance requires user authorization&quot;, cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null) |

## Properties

| Name | Summary |
|---|---|
| [cause](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#-654012527%2FProperties%2F1615067946) | [androidJvm]<br>open val [cause](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#-654012527%2FProperties%2F1615067946): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? |
| [message](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#1824300659%2FProperties%2F1615067946) | [androidJvm]<br>open val [message](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#1824300659%2FProperties%2F1615067946): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? |

## Functions

| Name | Summary |
|---|---|
| [addSuppressed](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#282858770%2FFunctions%2F1615067946) | [androidJvm]<br>fun [addSuppressed](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#282858770%2FFunctions%2F1615067946)(p0: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)) |
| [fillInStackTrace](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#-1102069925%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [fillInStackTrace](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#-1102069925%2FFunctions%2F1615067946)(): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html) |
| [getLocalizedMessage](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#1043865560%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [getLocalizedMessage](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#1043865560%2FFunctions%2F1615067946)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [getStackTrace](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#2050903719%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [getStackTrace](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#2050903719%2FFunctions%2F1615067946)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[StackTraceElement](https://developer.android.com/reference/kotlin/java/lang/StackTraceElement.html)&gt; |
| [getSuppressed](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#672492560%2FFunctions%2F1615067946) | [androidJvm]<br>fun [getSuppressed](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#672492560%2FFunctions%2F1615067946)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)&gt; |
| [initCause](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#-418225042%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [initCause](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#-418225042%2FFunctions%2F1615067946)(p0: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html) |
| [printStackTrace](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#-1769529168%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [printStackTrace](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#-1769529168%2FFunctions%2F1615067946)()<br>open fun [printStackTrace](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#1841853697%2FFunctions%2F1615067946)(p0: [PrintStream](https://developer.android.com/reference/kotlin/java/io/PrintStream.html))<br>open fun [printStackTrace](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#1175535278%2FFunctions%2F1615067946)(p0: [PrintWriter](https://developer.android.com/reference/kotlin/java/io/PrintWriter.html)) |
| [setStackTrace](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#2135801318%2FFunctions%2F1615067946) | [androidJvm]<br>open fun [setStackTrace](../../eu.europa.ec.eudi.wallet.statium/-signature-verification-error/index.md#2135801318%2FFunctions%2F1615067946)(p0: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[StackTraceElement](https://developer.android.com/reference/kotlin/java/lang/StackTraceElement.html)&gt;) |
