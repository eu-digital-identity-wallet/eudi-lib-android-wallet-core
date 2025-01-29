//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)

# OpenId4VciManager

interface [OpenId4VciManager](index.md)

OpenId4VciManager is the main entry point to issue documents using the OpenId4Vci protocol It provides methods to issue documents using a document type or an offer, and to resolve an offer

#### See also

| | |
|---|---|
| [OpenId4VciManager.Config](-config/index.md) | for the configuration options |

## Types

| Name | Summary |
|---|---|
| [Builder](-builder/index.md) | [androidJvm]<br>class [Builder](-builder/index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html))<br>Builder to create an instance of [OpenId4VciManager](index.md) |
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |
| [Config](-config/index.md) | [androidJvm]<br>data class [Config](-config/index.md)@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-overloads/index.html)constructor(val issuerUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val clientId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val authFlowRedirectionURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val useDPoPIfSupported: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = true, val parUsage: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = IF_SUPPORTED)<br>Configuration for the OpenId4Vci issuer |
| [OnDeferredIssueResult](-on-deferred-issue-result/index.md) | [androidJvm]<br>fun interface [OnDeferredIssueResult](-on-deferred-issue-result/index.md) : [OpenId4VciManager.OnResult](-on-result/index.md)&lt;[DeferredIssueResult](../-deferred-issue-result/index.md)&gt; <br>Callback to be called when a deferred document is issued |
| [OnIssueEvent](-on-issue-event/index.md) | [androidJvm]<br>fun interface [OnIssueEvent](-on-issue-event/index.md) : [OpenId4VciManager.OnResult](-on-result/index.md)&lt;[IssueEvent](../-issue-event/index.md)&gt; <br>Callback to be called when a document is issued |
| [OnResolvedOffer](-on-resolved-offer/index.md) | [androidJvm]<br>fun interface [OnResolvedOffer](-on-resolved-offer/index.md) : [OpenId4VciManager.OnResult](-on-result/index.md)&lt;[OfferResult](../-offer-result/index.md)&gt; <br>Callback to be called when an offer is resolved |
| [OnResult](-on-result/index.md) | [androidJvm]<br>fun interface [OnResult](-on-result/index.md)&lt;[T](-on-result/index.md) : [OpenId4VciResult](../-open-id4-vci-result/index.md)&gt;<br>Callback to be called for [OpenId4VciManager.issueDocumentByDocType](issue-document-by-doc-type.md), [OpenId4VciManager.issueDocumentByOffer](issue-document-by-offer.md), [OpenId4VciManager.issueDocumentByOfferUri](issue-document-by-offer-uri.md) and [OpenId4VciManager.resolveDocumentOffer](resolve-document-offer.md) methods |

## Functions

| Name | Summary |
|---|---|
| [getIssuerMetadata](get-issuer-metadata.md) | [androidJvm]<br>abstract suspend fun [getIssuerMetadata](get-issuer-metadata.md)(): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;CredentialIssuerMetadata&gt;<br>Provides the issuer metadata |
| [issueDeferredDocument](issue-deferred-document.md) | [androidJvm]<br>abstract fun [issueDeferredDocument](issue-deferred-document.md)(deferredDocument: DeferredDocument, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueResult: [OpenId4VciManager.OnDeferredIssueResult](-on-deferred-issue-result/index.md))<br>Issue a deferred document |
| [issueDocumentByConfigurationIdentifier](issue-document-by-configuration-identifier.md) | [androidJvm]<br>abstract fun [issueDocumentByConfigurationIdentifier](issue-document-by-configuration-identifier.md)(credentialConfigurationId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), txCode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))<br>Issue a document using a configuration identifier. |
| [issueDocumentByDocType](issue-document-by-doc-type.md) | [androidJvm]<br>abstract fun [~~issueDocumentByDocType~~](issue-document-by-doc-type.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), txCode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))<br>Issue a document using a document type |
| [issueDocumentByFormat](issue-document-by-format.md) | [androidJvm]<br>abstract fun [issueDocumentByFormat](issue-document-by-format.md)(format: DocumentFormat, txCode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))<br>Issue a document using a document format |
| [issueDocumentByOffer](issue-document-by-offer.md) | [androidJvm]<br>abstract fun [issueDocumentByOffer](issue-document-by-offer.md)(offer: [Offer](../-offer/index.md), txCode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))<br>Issue a document using an offer |
| [issueDocumentByOfferUri](issue-document-by-offer-uri.md) | [androidJvm]<br>abstract fun [issueDocumentByOfferUri](issue-document-by-offer-uri.md)(offerUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), txCode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onIssueEvent: [OpenId4VciManager.OnIssueEvent](-on-issue-event/index.md))<br>Issue a document using an offer URI |
| [resolveDocumentOffer](resolve-document-offer.md) | [androidJvm]<br>abstract fun [resolveDocumentOffer](resolve-document-offer.md)(offerUri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onResolvedOffer: [OpenId4VciManager.OnResolvedOffer](-on-resolved-offer/index.md))<br>Resolve an offer using OpenId4Vci protocol |
| [resumeWithAuthorization](resume-with-authorization.md) | [androidJvm]<br>abstract fun [resumeWithAuthorization](resume-with-authorization.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html))<br>abstract fun [resumeWithAuthorization](resume-with-authorization.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))<br>Resume the authorization flow after the user has been redirected back to the app |
