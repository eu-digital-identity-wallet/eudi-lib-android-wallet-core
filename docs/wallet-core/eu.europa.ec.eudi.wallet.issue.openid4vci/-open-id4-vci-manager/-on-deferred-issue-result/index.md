//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[OpenId4VciManager](../index.md)/[OnDeferredIssueResult](index.md)

# OnDeferredIssueResult

[release]\
fun interface [OnDeferredIssueResult](index.md) : [OpenId4VciManager.OnResult](../-on-result/index.md)&lt;[DeferredIssueResult](../../-deferred-issue-result/index.md)&gt; 

Callback to be called when a deferred document is issued

## Functions

| Name | Summary |
|---|---|
| [invoke](../-on-result/invoke.md) | [release]<br>open operator fun [invoke](../-on-result/invoke.md)(result: [DeferredIssueResult](../../-deferred-issue-result/index.md)) |
| [onResult](../-on-result/on-result.md) | [release]<br>abstract fun [onResult](../-on-result/on-result.md)(result: [DeferredIssueResult](../../-deferred-issue-result/index.md)) |