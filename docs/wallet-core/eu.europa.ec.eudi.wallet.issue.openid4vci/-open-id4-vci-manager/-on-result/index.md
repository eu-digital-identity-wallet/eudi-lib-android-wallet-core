//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[OpenId4VciManager](../index.md)/[OnResult](index.md)

# OnResult

fun interface [OnResult](index.md)&lt;[T](index.md) : [OpenId4VciResult](../../-open-id4-vci-result/index.md)&gt;

Callback to be called for [OpenId4VciManager.issueDocumentByDocType](../issue-document-by-doc-type.md), [OpenId4VciManager.issueDocumentByOffer](../issue-document-by-offer.md), [OpenId4VciManager.issueDocumentByOfferUri](../issue-document-by-offer-uri.md) and [OpenId4VciManager.resolveDocumentOffer](../resolve-document-offer.md) methods

#### Inheritors

| |
|---|
| [OnIssueEvent](../-on-issue-event/index.md) |
| [OnResolvedOffer](../-on-resolved-offer/index.md) |
| [OnDeferredIssueResult](../-on-deferred-issue-result/index.md) |

## Functions

| Name | Summary |
|---|---|
| [invoke](invoke.md) | [androidJvm]<br>open operator fun [invoke](invoke.md)(result: [T](index.md)) |
| [onResult](on-result.md) | [androidJvm]<br>abstract fun [onResult](on-result.md)(result: [T](index.md)) |
