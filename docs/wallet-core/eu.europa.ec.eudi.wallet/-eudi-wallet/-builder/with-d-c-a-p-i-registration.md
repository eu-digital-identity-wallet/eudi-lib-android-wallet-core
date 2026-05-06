//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)/[withDCAPIRegistration](with-d-c-a-p-i-registration.md)

# withDCAPIRegistration

[release]\
fun [withDCAPIRegistration](with-d-c-a-p-i-registration.md)(dcapiRegistration: [DCAPIRegistration](../../../eu.europa.ec.eudi.wallet.dcapi/-d-c-a-p-i-registration/index.md)): [EudiWallet.Builder](index.md)

Configure with the given [DCAPIRegistration](../../../eu.europa.ec.eudi.wallet.dcapi/-d-c-a-p-i-registration/index.md) to use for registering credentials with the Digital Credential API (DCAPI). If not set, the default DCAPIIsoMdocRegistration will be used when the DCAPI is enabled in the configuration.

#### Return

this [Builder](index.md) instance

#### Parameters

release

| | |
|---|---|
| dcapiRegistration | the DCAPI registration |