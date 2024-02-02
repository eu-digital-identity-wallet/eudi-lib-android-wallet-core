//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[loadSampleData](load-sample-data.md)

# loadSampleData

[androidJvm]\
fun [loadSampleData](load-sample-data.md)(sampleData: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): LoadSampleResult

Loads sample data into the wallet's document manager

#### Return

AddDocumentResult

#### Parameters

androidJvm

| | |
|---|---|
| sampleData | the sample data |

#### See also

| |
|---|
| SampleDocumentManager.loadSampleData |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
