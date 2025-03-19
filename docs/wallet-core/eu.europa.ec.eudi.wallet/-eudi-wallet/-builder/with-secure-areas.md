//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)/[withSecureAreas](with-secure-areas.md)

# withSecureAreas

[androidJvm]\
fun [withSecureAreas](with-secure-areas.md)(secureAreas: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;SecureArea&gt;): &lt;Error class: unknown class&gt;

Configure with the given SecureArea implementations to use for documents' keys management. If not set, the default secure area will be used which is AndroidKeystoreSecureArea.

#### Return

this [Builder](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| secureAreas | the secure areas |
