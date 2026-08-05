//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.response](../index.md)/[ReaderAuthPolicy](index.md)

# ReaderAuthPolicy

sealed interface [ReaderAuthPolicy](index.md)

Policy for how reader authentication results are enforced during response generation.

Controls whether ProcessedDeviceRequest.generateResponse includes documents in the device response based on the [ReaderAuth](../-reader-auth/index.md) result of the corresponding [RequestedDocument](../-requested-document/index.md).

#### Inheritors

| |
|---|
| [DoNotEnforce](-do-not-enforce/index.md) |
| [EnforceIfPresent](-enforce-if-present/index.md) |
| [AlwaysRequire](-always-require/index.md) |

## Types

| Name | Summary |
|---|---|
| [AlwaysRequire](-always-require/index.md) | [release]<br>data object [AlwaysRequire](-always-require/index.md) : [ReaderAuthPolicy](index.md)<br>Always require verified reader authentication. Documents are skipped when [ReaderAuth](../-reader-auth/index.md) is null or [ReaderAuth.isVerified](../-reader-auth/is-verified.md) is `false`. Only documents with verified reader authentication are included in the response. |
| [DoNotEnforce](-do-not-enforce/index.md) | [release]<br>data object [DoNotEnforce](-do-not-enforce/index.md) : [ReaderAuthPolicy](index.md)<br>Do not enforce reader authentication results. Documents are always included in the response regardless of [ReaderAuth](../-reader-auth/index.md) status. |
| [EnforceIfPresent](-enforce-if-present/index.md) | [release]<br>data object [EnforceIfPresent](-enforce-if-present/index.md) : [ReaderAuthPolicy](index.md)<br>Enforce reader authentication when present. Documents are skipped when [ReaderAuth](../-reader-auth/index.md) is present but [ReaderAuth.isVerified](../-reader-auth/is-verified.md) is `false`. Documents with no reader authentication (null [ReaderAuth](../-reader-auth/index.md)) are still included. |