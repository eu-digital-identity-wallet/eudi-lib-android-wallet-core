//[transfer-manager](../../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response](../../index.md)/[ReaderAuthPolicy](../index.md)/[EnforceIfPresent](index.md)

# EnforceIfPresent

[release]\
data object [EnforceIfPresent](index.md) : [ReaderAuthPolicy](../index.md)

Enforce reader authentication when present. Documents are skipped when [ReaderAuth](../../-reader-auth/index.md) is present but [ReaderAuth.isVerified](../../-reader-auth/is-verified.md) is `false`. Documents with no reader authentication (null [ReaderAuth](../../-reader-auth/index.md)) are still included.

This is the default policy.