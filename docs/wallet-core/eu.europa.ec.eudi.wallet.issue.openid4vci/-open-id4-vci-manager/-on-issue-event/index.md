//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[OpenId4VciManager](../index.md)/[OnIssueEvent](index.md)

# OnIssueEvent

[release]\
fun interface [OnIssueEvent](index.md) : [OpenId4VciManager.OnResult](../-on-result/index.md)&lt;[IssueEvent](../../-issue-event/index.md)&gt; 

Callback to be called when a document is issued

## Functions

| Name | Summary |
|---|---|
| [invoke](../-on-result/invoke.md) | [release]<br>open operator fun [invoke](../-on-result/invoke.md)(result: [IssueEvent](../../-issue-event/index.md)) |
| [onResult](../-on-result/on-result.md) | [release]<br>abstract fun [onResult](../-on-result/on-result.md)(result: [IssueEvent](../../-issue-event/index.md)) |