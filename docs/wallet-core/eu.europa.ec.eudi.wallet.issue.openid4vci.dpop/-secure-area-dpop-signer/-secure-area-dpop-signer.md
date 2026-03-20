//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci.dpop](../index.md)/[SecureAreaDpopSigner](index.md)/[SecureAreaDpopSigner](-secure-area-dpop-signer.md)

# SecureAreaDpopSigner

[androidJvm]\
constructor(config: [DPopConfig.Custom](../-d-pop-config/-custom/index.md), algorithms: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;Algorithm&gt;, logger: [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)? = null)

Creates a new DPoP signer with a fresh key in the specified secure area.

A new key is created immediately during construction using the provided algorithms and configuration settings. The configuration's [DPopConfig.Custom.createKeySettingsBuilder](../-d-pop-config/-custom/create-key-settings-builder.md) function receives the list of supported algorithms and selects an appropriate one for key creation.

#### Parameters

androidJvm

| | |
|---|---|
| config | The DPoP configuration containing the secure area and key settings builder |
| algorithms | The list of cryptographic algorithms supported by both server and secure area |
| logger | Optional logger for debugging |
