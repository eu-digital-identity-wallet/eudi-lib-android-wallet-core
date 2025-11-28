//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.provider](../index.md)/[WalletAttestationsProvider](index.md)/[getWalletAttestation](get-wallet-attestation.md)

# getWalletAttestation

[androidJvm]\
abstract suspend fun [getWalletAttestation](get-wallet-attestation.md)(keyInfo: KeyInfo): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;

Retrieves the Wallet Instance Attestation (WIA).

This attestation proves that the Wallet Application is genuine, untampered with, and trusted by the Wallet Provider. It is typically used for **Client Authentication** at the Authorization Server's Token Endpoint (OAuth 2.0).

#### Return

A [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html) containing the WIA as a signed JWT string (e.g., Client Attestation JWT).

#### Parameters

androidJvm

| | |
|---|---|
| keyInfo | Information about the cryptographic key that will be bound to this attestation. The Wallet Provider must sign the WIA such that it confirms this key belongs to a valid app instance. |
