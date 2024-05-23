//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[issueDocumentByOffer](issue-document-by-offer.md)

# issueDocumentByOffer

[androidJvm]\
fun [issueDocumentByOffer](issue-document-by-offer.md)(offer: [Offer](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-offer/index.md), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, onEvent: [OpenId4VciManager.OnIssueEvent](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-on-issue-event/index.md))

Issue a document using an offer and the OpenId4VCI protocol

#### Parameters

androidJvm

| | |
|---|---|
| offer | the offer to issue |
| executor | the executor defines the thread on which the callback will be called. If null, the callback will be called on the main thread |
| onEvent | the callback to be called when the document is issued |

#### See also

| |
|---|
| [OpenId4VciManager.issueDocumentByOffer](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/issue-document-by-offer.md) |
| [OpenId4VciManager.OnIssueEvent](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/-on-issue-event/index.md) | on how to handle the result |
| [IssueEvent.DocumentRequiresUserAuth](../../eu.europa.ec.eudi.wallet.issue.openid4vci/-issue-event/-document-requires-user-auth/index.md) | on how to handle user authentication |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWalletConfig.openId4VciConfig](../-eudi-wallet-config/open-id4-vci-config.md) is not set |
