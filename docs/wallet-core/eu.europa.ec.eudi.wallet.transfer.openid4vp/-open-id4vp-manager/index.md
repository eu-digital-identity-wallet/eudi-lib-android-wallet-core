//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openid4vp](../index.md)/[OpenId4vpManager](index.md)

# OpenId4vpManager

class [OpenId4vpManager](index.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), openId4VpConfig: [OpenId4VpConfig](../-open-id4-vp-config/index.md), val responseGenerator: [OpenId4VpCBORResponseGeneratorImpl](../-open-id4-vp-c-b-o-r-response-generator-impl/index.md)) : TransferEvent.Listenable

OpenId4vp manager. This class is used to manage the OpenId4vp transfer method. It is used to resolve the request uri and
send the response.

Example:

```kotlin
val certificates = listOf<X509Certificate>(
    // put trusted reader certificates here
)
val readerTrustStore = ReaderTrustStore.getDefault(
    listOf(context.applicationContext.getCertificate(certificates))
)

val documentedResolver = DocumentResolver { docRequest: DocRequest ->
    // put your code here to resolve the document
    // usually document resolution is done based on `docRequest.docType`
}

val openid4VpCBORResponseGenerator = OpenId4VpCBORResponseGeneratorImpl.Builder(context)
                .readerTrustStore(readerTrustStore)
                .documentsResolver(documentedResolver)
                .build()

val openId4vpManager = OpenId4vpManager(
   context,
   OpenId4VpConfig.Builder()
            .withClientIdSchemes(
            listOf(
                ClientIdScheme.Preregistered(
                    listOf(
                        PreregisteredVerifier(
                            "VerifierClientId",
                            "VerifierLegalName",
                            "https://example.com"
                        )
                    )
                ),
                ClientIdScheme.X509SanDns
            ))
            .withEncryptionAlgorithms(listOf(EncryptionAlgorithm.ECDH_ES))
            .withEncryptionMethods(listOf(EncryptionMethod.A128CBC_HS256))
            .build(),
   openid4VpCBORResponseGenerator
)
val transferEventListener = TransferEvent.Listener { event ->
  when (event) {
     is TransferEvent.Connecting -> {
         // inform user
     }
     is Transfer.Redirect -> {
         val redirect_uri = event.redirectUri
         // redirect user to the given URI
     }
     is TransferEvent.RequestReceived -> {
         val request = openId4vpManager.resolveRequestUri(event.request)
         // handle request and demand from user the documents to be disclosed
         val disclosedDocuments = listOf<DisclosedDocument>()
         val response = openid4VpCBORResponseGenerator.createResponse(disclosedDocuments)
         openId4vpManager.sendResponse(response.deviceResponseBytes)
     }
  }
}
openId4vpManager.addTransferEventListener(transferEventListener)

// resolve a request URI
openId4vpManager.resolveRequestUri(requestURI)
```

#### Parameters

androidJvm

|                   |                                                  |
|-------------------|--------------------------------------------------|
| context           | the application context                          |
| openId4VpConfig   | the configuration for OpenId4Vp                  |
| responseGenerator | that parses the request and creates the response |

## Constructors

| | |
|---|---|
| [OpenId4vpManager](-open-id4vp-manager.md) | [androidJvm]<br>constructor(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), openId4VpConfig: [OpenId4VpConfig](../-open-id4-vp-config/index.md), responseGenerator: [OpenId4VpCBORResponseGeneratorImpl](../-open-id4-vp-c-b-o-r-response-generator-impl/index.md)) |

## Types

| Name                             | Summary                                                 |
|----------------------------------|---------------------------------------------------------|
| [Companion](-companion/index.md) | [androidJvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [addTransferEventListener](add-transfer-event-listener.md) | [androidJvm]<br>open override fun [addTransferEventListener](add-transfer-event-listener.md)(listener: TransferEvent.Listener): [OpenId4vpManager](index.md) |
| [close](close.md) | [androidJvm]<br>fun [close](close.md)()<br>Closes the OpenId4VpManager |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [androidJvm]<br>open override fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): [OpenId4vpManager](index.md) |
| [removeTransferEventListener](remove-transfer-event-listener.md) | [androidJvm]<br>open override fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): [OpenId4vpManager](index.md) |
| [resolveRequestUri](resolve-request-uri.md) | [androidJvm]<br>fun [resolveRequestUri](resolve-request-uri.md)(openid4VPURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Resolve a request uri |
| [sendResponse](send-response.md) | [androidJvm]<br>fun [sendResponse](send-response.md)(deviceResponse: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html))<br>Sends a response to the verifier |
| [setExecutor](set-executor.md) | [androidJvm]<br>fun [setExecutor](set-executor.md)(executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html))<br>Setting the `executor` is optional and defines the executor that will be used to execute the callback. If the `executor` is not defined, the callback will be executed on the main thread. |

## Properties

| Name                                                 | Summary                                                                                                                                                                                                            |
|------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [ktorHttpClientFactory](ktor-http-client-factory.md) | [androidJvm]<br>var [ktorHttpClientFactory](ktor-http-client-factory.md): () -&gt; HttpClient<br>the factory to create the http client. By default, it uses the DefaultHttpClientFactory                           |
| [logger](logger.md)                                  | [androidJvm]<br>var [logger](logger.md): [Logger](../../eu.europa.ec.eudi.wallet.logging/-logger/index.md)?<br>the logger                                                                                          |
| [responseGenerator](response-generator.md)           | [androidJvm]<br>val [responseGenerator](response-generator.md): [OpenId4VpCBORResponseGeneratorImpl](../-open-id4-vp-c-b-o-r-response-generator-impl/index.md)<br>that parses the request and creates the response |
