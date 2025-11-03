//[wallet-core](../../../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../../../index.md)/[OpenId4VciManager](../../../index.md)/[Config](../../index.md)/[DPoPUsage](../index.md)/[IfSupported](index.md)

# IfSupported

data class [IfSupported](index.md)(val algorithm: Algorithm = Algorithm.ESP256) : [OpenId4VciManager.Config.DPoPUsage](../index.md)

Enables DPoP if the server supports it, using the specified algorithm.

#### See also

| |
|---|
| Algorithm.isSigning |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://developer.android.com/reference/kotlin/java/lang/IllegalArgumentException.html) | if the specified algorithm is not supported     or is not a signing algorithm. |

## Constructors

| | |
|---|---|
| [IfSupported](-if-supported.md) | [androidJvm]<br>constructor(algorithm: Algorithm = Algorithm.ESP256) |

## Properties

| Name | Summary |
|---|---|
| [algorithm](algorithm.md) | [androidJvm]<br>val [algorithm](algorithm.md): Algorithm<br>The cryptographic algorithm to use for DPoP token signing.     Defaults to ESP256 (ECDSA with P-256 curve and SHA-256). |
