//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openid4vp](../index.md)/[OpenId4vpManager](index.md)/[setExecutor](set-executor.md)

# setExecutor

[androidJvm]\
fun [setExecutor](set-executor.md)(executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html))

Setting the `executor` is optional and defines the executor that will be used to execute the callback. If the `executor` is not defined, the callback will be executed on the main thread.

#### Parameters

androidJvm

| | |
|---|---|
| Executor | the executor to use for callbacks. If null, the main executor will be used. |
