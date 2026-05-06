//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)/[configureReaderAuthPolicy](configure-reader-auth-policy.md)

# configureReaderAuthPolicy

[release]\
fun [configureReaderAuthPolicy](configure-reader-auth-policy.md)(readerAuthPolicy: ReaderAuthPolicy): [EudiWalletConfig](index.md)

Configure the reader authentication enforcement policy. This policy controls how reader authentication results affect document disclosure during proximity and DCAPI presentations.

When a verifier's DeviceRequest includes reader authentication and the verifier's certificate is not in the configured ReaderTrustStore, the policy determines whether the document is included in the response or excluded.

Per ISO 18013-5, when all documents are excluded due to reader authentication failure, the wallet returns a DeviceResponse with status 10 (General Error).

#### Return

the [EudiWalletConfig](index.md) instance

#### Parameters

release

| | |
|---|---|
| readerAuthPolicy | the reader authentication enforcement policy |

#### See also

| |
|---|
| ReaderAuthPolicy |
| [configureReaderTrustStore](configure-reader-trust-store.md) |