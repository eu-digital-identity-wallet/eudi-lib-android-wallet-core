//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)/[withReaderTrustStore](with-reader-trust-store.md)

# withReaderTrustStore

[androidJvm]\
fun [withReaderTrustStore](with-reader-trust-store.md)(readerTrustStore: ReaderTrustStore): &lt;Error class: unknown class&gt;

Configure with the given ReaderTrustStore to use for performing reader authentication. If not set, the default reader trust store will be used which is initialized with the certificates provided in the [EudiWalletConfig.configureReaderTrustStore](../../-eudi-wallet-config/configure-reader-trust-store.md) methods.

#### Return

this [Builder](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| readerTrustStore | the reader trust store |
