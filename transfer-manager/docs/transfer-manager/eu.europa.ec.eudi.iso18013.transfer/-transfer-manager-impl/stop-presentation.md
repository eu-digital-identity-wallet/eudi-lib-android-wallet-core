//[transfer-manager](../../../index.md)/[eu.europa.ec.eudi.iso18013.transfer](../index.md)/[TransferManagerImpl](index.md)/[stopPresentation](stop-presentation.md)

# stopPresentation

[release]\
open override fun [stopPresentation](stop-presentation.md)(sendSessionTerminationMessage: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) = true, useTransportSpecificSessionTermination: [Boolean](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-boolean/index.html) = false)

Closes the connection and clears the data of the session Also, sends a termination message if there is a connected mdoc verifier

#### Parameters

release

| | |
|---|---|
| sendSessionTerminationMessage | Whether to send session termination message. |
| useTransportSpecificSessionTermination | Whether to use transport-specific session |