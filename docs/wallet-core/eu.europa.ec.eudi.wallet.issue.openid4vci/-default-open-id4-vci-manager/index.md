//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[DefaultOpenId4VciManager](index.md)

# DefaultOpenId4VciManager

[androidJvm]\
class [DefaultOpenId4VciManager](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), documentManager: DocumentManager, var config: [OpenId4VciManager.Config](../-open-id4-vci-manager/-config/index.md)) : [OpenId4VciManager](../-open-id4-vci-manager/index.md)

## Constructors

| | |
|---|---|
| [DefaultOpenId4VciManager](-default-open-id4-vci-manager.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), documentManager: DocumentManager, config: [OpenId4VciManager.Config](../-open-id4-vci-manager/-config/index.md)) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name                                                      | Summary                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|-----------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [issueDocumentByDocType](issue-document-by-doc-type.md)   | [androidJvm]<br>open override fun [issueDocumentByDocType](issue-document-by-doc-type.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)?, onIssueEvent: [OpenId4VciManager.OnIssueEvent](../-open-id4-vci-manager/-on-issue-event/index.md))<br>Issue a document using a document type                                                                                                                                                                   |
| [issueDocumentByOffer](issue-document-by-offer.md)        | [androidJvm]<br>open override fun [issueDocumentByOffer](issue-document-by-offer.md)(offer: [Offer](../-offer/index.md), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)?, onIssueEvent: [OpenId4VciManager.OnIssueEvent](../-open-id4-vci-manager/-on-issue-event/index.md))<br>Issue a document using an offer                                                                                                                                                                                                                                      |
| [issueDocumentByOfferUri](issue-document-by-offer-uri.md) | [androidJvm]<br>open override fun [issueDocumentByOfferUri](issue-document-by-offer-uri.md)(offerUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)?, onIssueEvent: [OpenId4VciManager.OnIssueEvent](../-open-id4-vci-manager/-on-issue-event/index.md))<br>Issue a document using an offer URI                                                                                                                                                                   |
| [resolveDocumentOffer](resolve-document-offer.md)         | [androidJvm]<br>open override fun [resolveDocumentOffer](resolve-document-offer.md)(offerUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)?, onResolvedOffer: [OpenId4VciManager.OnResolvedOffer](../-open-id4-vci-manager/-on-resolved-offer/index.md))<br>Resolve an offer using OpenId4Vci protocol                                                                                                                                                           |
| [resumeWithAuthorization](resume-with-authorization.md)   | [androidJvm]<br>open override fun [resumeWithAuthorization](resume-with-authorization.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))<br>open override fun [resumeWithAuthorization](resume-with-authorization.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html))<br>open override fun [resumeWithAuthorization](resume-with-authorization.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Resume the authorization flow after the user has been redirected back to the app |

## Properties

| Name | Summary |
|---|---|
| [config](config.md) | [androidJvm]<br>var [config](config.md): [OpenId4VciManager.Config](../-open-id4-vci-manager/-config/index.md) |
