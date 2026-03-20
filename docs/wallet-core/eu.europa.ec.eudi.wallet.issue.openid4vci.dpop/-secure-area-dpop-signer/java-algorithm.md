//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.dpop](../index.md)/[SecureAreaDpopSigner](index.md)/[javaAlgorithm](java-algorithm.md)

# javaAlgorithm

[androidJvm]\
open override val [javaAlgorithm](java-algorithm.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)

The Java algorithm identifier for the signing algorithm.

This property provides the Java Security API algorithm name (e.g., &quot;SHA256withECDSA&quot; for ES256) that corresponds to the DPoP signing algorithm. The value is derived from the algorithm used to create the key in [keyInfo](key-info.md) and is used internally by the OpenID4VCI library.

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://developer.android.com/reference/kotlin/java/lang/IllegalArgumentException.html) | if the key's algorithm is not a signing algorithm     or doesn't have a corresponding Java algorithm identifier |
