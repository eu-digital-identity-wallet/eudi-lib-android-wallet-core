//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpManager](index.md)/[resolveRequestUri](resolve-request-uri.md)

# resolveRequestUri

[androidJvm]\
fun [resolveRequestUri](resolve-request-uri.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))

Resolves a request URI. This method is asynchronous and the result is emitted through the TransferEvent.Listener interface. Every time it is called it cancels any previous request that is being resolved. This will lead to the TransferEvent.Disconnected event being emitted.

#### See also

| |
|---|
| [OpenId4VpManager.addTransferEventListener](add-transfer-event-listener.md) |
