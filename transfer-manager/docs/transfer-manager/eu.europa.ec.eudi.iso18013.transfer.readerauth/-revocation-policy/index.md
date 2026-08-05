//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.readerauth](../index.md)/[RevocationPolicy](index.md)

# RevocationPolicy

sealed interface [RevocationPolicy](index.md)

Policy that controls how certificate revocation is checked during reader authentication trust path validation.

#### Inheritors

| |
|---|
| [NoCheck](-no-check/index.md) |
| [HardFail](-hard-fail/index.md) |
| [SoftFail](-soft-fail/index.md) |

## Types

| Name | Summary |
|---|---|
| [HardFail](-hard-fail/index.md) | [release]<br>data object [HardFail](-hard-fail/index.md) : [RevocationPolicy](index.md)<br>Validation fails if a certificate is revoked OR if the CRL cannot be retrieved. |
| [NoCheck](-no-check/index.md) | [release]<br>data object [NoCheck](-no-check/index.md) : [RevocationPolicy](index.md)<br>No revocation checking is performed. This is the default for backwards compatibility. |
| [SoftFail](-soft-fail/index.md) | [release]<br>data object [SoftFail](-soft-fail/index.md) : [RevocationPolicy](index.md)<br>Validation fails if a certificate is revoked, but tolerates CRL unavailability. |