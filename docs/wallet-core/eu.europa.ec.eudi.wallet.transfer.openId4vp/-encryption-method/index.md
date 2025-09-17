//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[EncryptionMethod](index.md)

# EncryptionMethod

enum [EncryptionMethod](index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[EncryptionMethod](index.md)&gt; 

Encryption method enumeration for OpenID4VP content encryption operations.

This enum defines the supported symmetric encryption methods used for protecting the actual content (payload) in OpenID4VP communications. The methods include both authenticated encryption (AEAD) modes and traditional cipher modes with separate authentication.

## Method Categories:

- 
   **AES-CBC + HMAC**: Combined cipher and MAC modes (A128CBC_HS256, A192CBC_HS384, A256CBC_HS512)
- 
   **AES-GCM**: Authenticated encryption modes (A128GCM, A192GCM, A256GCM)
- 
   **Deprecated**: Legacy methods for backward compatibility
- 
   **XC20P**: ChaCha20-Poly1305 authenticated encryption

## Security Recommendations:

- 
   Prefer GCM modes for new implementations due to better performance and security
- 
   Avoid deprecated methods unless required for backward compatibility
- 
   XC20P provides good security with different performance characteristics than AES

#### Since

1.0.0

#### See also

| |
|---|
| [OpenId4VpConfig.encryptionMethods](../-open-id4-vp-config/encryption-methods.md) |

## Entries

| | |
|---|---|
| [A128CBC_HS256](-a128-c-b-c_-h-s256/index.md) | [androidJvm]<br>[A128CBC_HS256](-a128-c-b-c_-h-s256/index.md)<br>AES-128 in CBC mode with HMAC-SHA256 authentication |
| [A192CBC_HS384](-a192-c-b-c_-h-s384/index.md) | [androidJvm]<br>[A192CBC_HS384](-a192-c-b-c_-h-s384/index.md)<br>AES-192 in CBC mode with HMAC-SHA384 authentication |
| [A256CBC_HS512](-a256-c-b-c_-h-s512/index.md) | [androidJvm]<br>[A256CBC_HS512](-a256-c-b-c_-h-s512/index.md)<br>AES-256 in CBC mode with HMAC-SHA512 authentication |
| [A128GCM](-a128-g-c-m/index.md) | [androidJvm]<br>[A128GCM](-a128-g-c-m/index.md)<br>AES-128 in Galois/Counter Mode (authenticated encryption) |
| [A192GCM](-a192-g-c-m/index.md) | [androidJvm]<br>[A192GCM](-a192-g-c-m/index.md)<br>AES-192 in Galois/Counter Mode (authenticated encryption) |
| [A256GCM](-a256-g-c-m/index.md) | [androidJvm]<br>[A256GCM](-a256-g-c-m/index.md)<br>AES-256 in Galois/Counter Mode (authenticated encryption) |
| [A128CBC_HS256_DEPRECATED](-a128-c-b-c_-h-s256_-d-e-p-r-e-c-a-t-e-d/index.md) | [androidJvm]<br>[A128CBC_HS256_DEPRECATED](-a128-c-b-c_-h-s256_-d-e-p-r-e-c-a-t-e-d/index.md)<br>AES-128 in CBC mode with HMAC-SHA256 authentication (deprecated). |
| [A256CBC_HS512_DEPRECATED](-a256-c-b-c_-h-s512_-d-e-p-r-e-c-a-t-e-d/index.md) | [androidJvm]<br>[A256CBC_HS512_DEPRECATED](-a256-c-b-c_-h-s512_-d-e-p-r-e-c-a-t-e-d/index.md)<br>AES-256 in CBC mode with HMAC-SHA512 authentication (deprecated). |
| [XC20P](-x-c20-p/index.md) | [androidJvm]<br>[XC20P](-x-c20-p/index.md)<br>ChaCha20-Poly1305 authenticated encryption |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [entries](entries.md) | [androidJvm]<br>val [entries](entries.md): [EnumEntries](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.enums/-enum-entries/index.html)&lt;[EncryptionMethod](index.md)&gt;<br>Returns a representation of an immutable list of all enum entries, in the order they're declared. |
| [name](-x-c20-p/index.md#-372974862%2FProperties%2F1615067946) | [androidJvm]<br>val [name](-x-c20-p/index.md#-372974862%2FProperties%2F1615067946): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [ordinal](-x-c20-p/index.md#-739389684%2FProperties%2F1615067946) | [androidJvm]<br>val [ordinal](-x-c20-p/index.md#-739389684%2FProperties%2F1615067946): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [androidJvm]<br>fun [valueOf](value-of.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [EncryptionMethod](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [androidJvm]<br>fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[EncryptionMethod](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |
