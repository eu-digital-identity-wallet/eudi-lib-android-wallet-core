//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer.zkp](../index.md)/[ZkResponsePolicy](index.md)

# ZkResponsePolicy

sealed interface [ZkResponsePolicy](index.md)

Policy that determines behavior when ZK proof generation fails during response generation.

#### Inheritors

| |
|---|
| [Strict](-strict/index.md) |
| [FallbackToFullDisclosure](-fallback-to-full-disclosure/index.md) |

## Types

| Name | Summary |
|---|---|
| [FallbackToFullDisclosure](-fallback-to-full-disclosure/index.md) | [release]<br>data object [FallbackToFullDisclosure](-fallback-to-full-disclosure/index.md) : [ZkResponsePolicy](index.md)<br>Fall back to full document disclosure if ZK proof generation fails. This is the current default for backwards compatibility and will be changed to [Strict](-strict/index.md) in a future release. |
| [Strict](-strict/index.md) | [release]<br>data object [Strict](-strict/index.md) : [ZkResponsePolicy](index.md)<br>Abort disclosure for the document if ZK proof generation fails. Recommended for production use to prevent unintended full document disclosure. |