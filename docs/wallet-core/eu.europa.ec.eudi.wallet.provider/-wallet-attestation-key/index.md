//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.provider](../index.md)/[WalletAttestationKey](index.md)

# WalletAttestationKey

[release]\
open class [WalletAttestationKey](index.md)(val keyInfo: KeyInfo, val signFunction: suspend ([ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)) -&gt; [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html))

## Constructors

| | |
|---|---|
| [WalletAttestationKey](-wallet-attestation-key.md) | [release]<br>constructor(keyInfo: KeyInfo, signFunction: suspend ([ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)) -&gt; [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [keyInfo](key-info.md) | [release]<br>val [keyInfo](key-info.md): KeyInfo |
| [signFunction](sign-function.md) | [release]<br>val [signFunction](sign-function.md): suspend ([ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html)) -&gt; [ByteArray](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-byte-array/index.html) |

## Functions

| Name | Summary |
|---|---|
| [toClientAuthentication](to-client-authentication.md) | [release]<br>suspend fun [WalletAttestationsProvider](../-wallet-attestations-provider/index.md).[toClientAuthentication](to-client-authentication.md)(): [Result](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/index.html)&lt;ClientAuthentication.AttestationBased&gt; |