//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletConfig](index.md)/[readerAuthPolicy](reader-auth-policy.md)

# readerAuthPolicy

[release]\
var [readerAuthPolicy](reader-auth-policy.md): ReaderAuthPolicy

The reader authentication enforcement policy for proximity and DCAPI presentations. This determines how the wallet handles reader authentication results when generating device responses.

The available policies are:

- 
   ReaderAuthPolicy.DoNotEnforce: Reader authentication is evaluated but never blocks document disclosure. This was the default behavior before version 0.27.0.
- 
   ReaderAuthPolicy.EnforceIfPresent: Documents are excluded from the response when reader authentication is present but fails verification (default).
- 
   ReaderAuthPolicy.AlwaysRequire: Documents are excluded unless reader authentication is present and verified.

The default is ReaderAuthPolicy.EnforceIfPresent.

#### See also

| |
|---|
| ReaderAuthPolicy |
| [configureReaderTrustStore](configure-reader-trust-store.md) |