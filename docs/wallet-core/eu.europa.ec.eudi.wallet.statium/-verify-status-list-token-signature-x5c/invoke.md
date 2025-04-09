//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../index.md)/[VerifyStatusListTokenSignatureX5c](index.md)/[invoke](invoke.md)

# invoke

[androidJvm]\
open suspend operator override fun [invoke](invoke.md)(statusListToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), format: StatusListTokenFormat, at: Instant): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)&gt;

Verifies the signature of a status list token. It requires the [statusListToken](invoke.md) to be in JWT format and contains an x5c header.

#### Return

A [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html) success or failure.

#### Parameters

androidJvm

| | |
|---|---|
| statusListToken | The status list token to verify. |
| format | The format of the status list token. |
| at | The time at which the verification is performed. |
