//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../index.md)/[IssueEvent](../index.md)/[Failure](index.md)

# Failure

[release]\
data class [Failure](index.md)(val cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) : [IssueEvent](../index.md), [OpenId4VciResult.Erroneous](../../-open-id4-vci-result/-erroneous/index.md)

The issuance failed.

## Constructors

| | |
|---|---|
| [Failure](-failure.md) | [release]<br>constructor(cause: [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [cause](cause.md) | [release]<br>open override val [cause](cause.md): [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html)<br>the error that caused the failure |