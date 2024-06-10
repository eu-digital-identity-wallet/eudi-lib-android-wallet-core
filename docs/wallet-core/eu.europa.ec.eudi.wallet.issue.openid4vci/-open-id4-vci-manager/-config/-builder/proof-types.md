//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../../index.md)/[OpenId4VciManager](../../index.md)/[Config](../index.md)/[Builder](index.md)/[proofTypes](proof-types.md)

# proofTypes

[androidJvm]\
fun [proofTypes](proof-types.md)(vararg
proofType: [OpenId4VciManager.Config.ProofType](../-proof-type/index.md)): [OpenId4VciManager.Config.Builder](index.md)

Set the proof types. The supported proof types are [ProofType](../-proof-type/index.md). The order of the proof types is
the order in which they will be used.

#### Parameters

androidJvm

|           |                 |
|-----------|-----------------|
| proofType | the proof types |

#### Throws

|                                                                                                                        |                                            |
|------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|
| [IllegalArgumentException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-argument-exception/index.html) | if a proof type is provided more than once |
