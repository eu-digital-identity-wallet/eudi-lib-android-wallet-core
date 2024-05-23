//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)

# OpenId4VciManager

interface [OpenId4VciManager](index.md)

OpenId4VciManager is the main entry point to issue documents using the OpenId4Vci protocol It provides methods to issue documents using a document type or an offer, and to resolve an offer

#### See also

| | |
|---|---|
| [OpenId4VciManager.Config](-config/index.md) | for the configuration options |

#### Inheritors

| |
|---|
| [DefaultOpenId4VciManager](../-default-open-id4-vci-manager/index.md) |

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [androidJvm]<br>class [Builder](-builder/index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html))<br>Builder to create an instance of [OpenId4VciManager](index.md) |
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |
| [Config](-config/index.md) | [androidJvm]<br>data class [Config](-config/index.md)(val issuerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val authFlowRedirectionURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val useStrongBoxIfSupported: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html))<br>Configuration for the OpenId4Vci issuer |
| [OnIssueEvent](-on-issue-event/index.md) | [androidJvm]<br>fun interface [OnIssueEvent](-on-issue-event/index.md) : [OpenId4VciManager.OnResult](-on-result/index.md)&lt;[IssueEvent](../-issue-event/index.md)&gt; <br>Callback to be called when a document is issued |
| [OnResolvedOffer](-on-resolved-offer/index.md) | [androidJvm]<br>fun interface [OnResolvedOffer](-on-resolved-offer/index.md) : [OpenId4VciManager.OnResult](-on-result/index.md)&lt;[OfferResult](../-offer-result/index.md)&gt; <br>Callback to be called when an offer is resolved |
| [OnResult](-on-result/index.md) | [androidJvm]<br>fun interface [OnResult](-on-result/index.md)&lt;[T](-on-result/index.md)&gt; |

## Functions

| Name | Summary |
|---|---|
| [issueDocumentByDocType](issue-document-by-doc-type.md) | [androidJvm]<br>abstract fun [issueDocumentByDocType](issue-document-by-doc-type.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), config: [OpenId4VciManager.Config](-config/index.md)? = null, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))<br>Issue a document using a document type |
| [issueDocumentByOffer](issue-document-by-offer.md) | [androidJvm]<br>abstract fun [issueDocumentByOffer](issue-document-by-offer.md)(offer: [Offer](../-offer/index.md), config: [OpenId4VciManager.Config](-config/index.md)? = null, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))<br>Issue a document using an offer |
| [issueDocumentByOfferUri](issue-document-by-offer-uri.md) | [androidJvm]<br>abstract fun [issueDocumentByOfferUri](issue-document-by-offer-uri.md)(offerUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), config: [OpenId4VciManager.Config](-config/index.md)? = null, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))<br>Issue a document using an offer URI |
| [resolveDocumentOffer](resolve-document-offer.md) | [androidJvm]<br>abstract fun [resolveDocumentOffer](resolve-document-offer.md)(offerUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onResolvedOffer: [OpenId4VciManager.OnResolvedOffer](-on-resolved-offer/index.md))<br>Resolve an offer using OpenId4Vci protocol |
| [resumeWithAuthorization](resume-with-authorization.md) | [androidJvm]<br>abstract fun [resumeWithAuthorization](resume-with-authorization.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))<br>abstract fun [resumeWithAuthorization](resume-with-authorization.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html))<br>abstract fun [resumeWithAuthorization](resume-with-authorization.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Resume the authorization flow after the user has been redirected back to the app |
