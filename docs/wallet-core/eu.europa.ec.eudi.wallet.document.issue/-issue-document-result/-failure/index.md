//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.document.issue](../../index.md)/[IssueDocumentResult](../index.md)/[Failure](index.md)

# Failure

[androidJvm]\
data class [Failure](index.md)(val
error: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) : [IssueDocumentResult](../index.md)

The document issuance failed.

## Constructors

|                        |                                                                                                                            |
|------------------------|----------------------------------------------------------------------------------------------------------------------------|
| [Failure](-failure.md) | [androidJvm]<br>constructor(error: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) |

## Properties

| Name              | Summary                                                                                                                                                            |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [error](error.md) | [androidJvm]<br>val [error](error.md): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)<br>the error that caused the failure |
