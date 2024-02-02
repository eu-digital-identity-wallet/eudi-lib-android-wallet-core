//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)

# OpenId4VciManager

class [OpenId4VciManager](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), config: [OpenId4VciConfig](../-open-id4-vci-config/index.md), documentManager: DocumentManager)

Manager for issuing documents using OpenID4VCI.

#### Parameters

androidJvm

| | |
|---|---|
| context | the application context |
| config | the configuration for OpenID4VCI |
| documentManager | the document manager to use for issuing documents |
| executor | the executor to use for callbacks. If null, the main executor will be used. |

## Constructors

| | |
|---|---|
| [OpenId4VciManager](-open-id4-vci-manager.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), config: [OpenId4VciConfig](../-open-id4-vci-config/index.md), documentManager: DocumentManager) |

## Types

| Name | Summary |
|---|---|
| [OnIssueCallback](-on-issue-callback/index.md) | [androidJvm]<br>fun interface [OnIssueCallback](-on-issue-callback/index.md) |

## Functions

| Name | Summary |
|---|---|
| [issueDocument](issue-document.md) | [androidJvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [issueDocument](issue-document.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, callback: [OpenId4VciManager.OnIssueCallback](-on-issue-callback/index.md))<br>Issues a document of the given type. |
