//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.provider](../index.md)/[WalletAttestationsProvider](index.md)

# WalletAttestationsProvider

[androidJvm]\
interface [WalletAttestationsProvider](index.md)

Interface defining the bridge between the Wallet Core SDK and the Wallet Provider Service.

Implementations of this interface are responsible for communicating with the Wallet Provider's backend to retrieve cryptographic proofs (Attestations) regarding the integrity of the Wallet Application and the security of the Device.

These attestations are required during the OpenID for Verifiable Credential Issuance (OID4VCI) flow.

## Functions

| Name | Summary |
|---|---|
| [getKeyAttestation](get-key-attestation.md) | [androidJvm]<br>abstract suspend fun [getKeyAttestation](get-key-attestation.md)(keys: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;KeyInfo&gt;, nonce: Nonce?): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>Retrieves the Wallet Unit Attestation (WUA) or Key Attestation. |
| [getWalletAttestation](get-wallet-attestation.md) | [androidJvm]<br>abstract suspend fun [getWalletAttestation](get-wallet-attestation.md)(keyInfo: KeyInfo): [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-result/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>Retrieves the Wallet Instance Attestation (WIA). |
