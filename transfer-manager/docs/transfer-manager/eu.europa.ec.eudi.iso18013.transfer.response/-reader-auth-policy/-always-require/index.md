//[transfer-manager](../../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response](../../index.md)/[ReaderAuthPolicy](../index.md)/[AlwaysRequire](index.md)

# AlwaysRequire

[release]\
data object [AlwaysRequire](index.md) : [ReaderAuthPolicy](../index.md)

Always require verified reader authentication. Documents are skipped when [ReaderAuth](../../-reader-auth/index.md) is null or [ReaderAuth.isVerified](../../-reader-auth/is-verified.md) is `false`. Only documents with verified reader authentication are included in the response.