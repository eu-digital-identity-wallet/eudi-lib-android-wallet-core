//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.statium](../index.md)/[VerifyStatusListTokenSignatureX5c](index.md)

# VerifyStatusListTokenSignatureX5c

[androidJvm]\
class [VerifyStatusListTokenSignatureX5c](index.md) : VerifyStatusListTokenJwtSignature

Verifies the signature of a status list token using the x5c header.

## Constructors

| | |
|---|---|
| [VerifyStatusListTokenSignatureX5c](-verify-status-list-token-signature-x5c.md) | [androidJvm]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [invoke](invoke.md) | [androidJvm]<br>open suspend operator override fun [invoke](invoke.md)(statusListToken: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), at: [Instant](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.time/-instant/index.html)): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)&gt;<br>Verifies the signature of a status list token. It requires the [statusListToken](invoke.md) to be in JWT format and contains an x5c header. |
