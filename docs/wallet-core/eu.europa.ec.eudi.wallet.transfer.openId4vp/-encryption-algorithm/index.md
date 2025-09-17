//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[EncryptionAlgorithm](index.md)

# EncryptionAlgorithm

enum [EncryptionAlgorithm](index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[EncryptionAlgorithm](index.md)&gt; 

Encryption algorithm enumeration for OpenID4VP key agreement and key wrapping operations.

This enum defines the supported key agreement algorithms used for encrypting OpenID4VP communications. All algorithms are based on Elliptic Curve Diffie-Hellman Ephemeral Static (ECDH-ES) key agreement, with variations in key derivation and wrapping mechanisms.

## Algorithm Details:

- 
   **ECDH_ES**: Direct key agreement without key wrapping
- 
   **ECDH_ES_A128KW**: Key agreement with AES-128 key wrapping
- 
   **ECDH_ES_A192KW**: Key agreement with AES-192 key wrapping
- 
   **ECDH_ES_A256KW**: Key agreement with AES-256 key wrapping

All algorithms provide forward secrecy through ephemeral key generation and are suitable for protecting sensitive credential data during OpenID4VP exchanges.

#### Since

1.0.0

#### See also

| |
|---|
| [OpenId4VpConfig.encryptionAlgorithms](../-open-id4-vp-config/encryption-algorithms.md) |

## Entries

| | |
|---|---|
| [ECDH_ES](-e-c-d-h_-e-s/index.md) | [androidJvm]<br>[ECDH_ES](-e-c-d-h_-e-s/index.md)<br>Elliptic Curve Diffie-Hellman Ephemeral Static key agreement |
| [ECDH_ES_A128KW](-e-c-d-h_-e-s_-a128-k-w/index.md) | [androidJvm]<br>[ECDH_ES_A128KW](-e-c-d-h_-e-s_-a128-k-w/index.md)<br>ECDH-ES with AES-128 Key Wrap algorithm |
| [ECDH_ES_A192KW](-e-c-d-h_-e-s_-a192-k-w/index.md) | [androidJvm]<br>[ECDH_ES_A192KW](-e-c-d-h_-e-s_-a192-k-w/index.md)<br>ECDH-ES with AES-192 Key Wrap algorithm |
| [ECDH_ES_A256KW](-e-c-d-h_-e-s_-a256-k-w/index.md) | [androidJvm]<br>[ECDH_ES_A256KW](-e-c-d-h_-e-s_-a256-k-w/index.md)<br>ECDH-ES with AES-256 Key Wrap algorithm |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [entries](entries.md) | [androidJvm]<br>val [entries](entries.md): [EnumEntries](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.enums/-enum-entries/index.html)&lt;[EncryptionAlgorithm](index.md)&gt;<br>Returns a representation of an immutable list of all enum entries, in the order they're declared. |
| [name](../-encryption-method/-x-c20-p/index.md#-372974862%2FProperties%2F1615067946) | [androidJvm]<br>val [name](../-encryption-method/-x-c20-p/index.md#-372974862%2FProperties%2F1615067946): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [ordinal](../-encryption-method/-x-c20-p/index.md#-739389684%2FProperties%2F1615067946) | [androidJvm]<br>val [ordinal](../-encryption-method/-x-c20-p/index.md#-739389684%2FProperties%2F1615067946): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [androidJvm]<br>fun [valueOf](value-of.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [EncryptionAlgorithm](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [androidJvm]<br>fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[EncryptionAlgorithm](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |
