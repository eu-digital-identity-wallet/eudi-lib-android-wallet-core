//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWalletSDK](index.md)

# EudiWalletSDK

object [EudiWalletSDK](index.md)

Eudi wallet sdk object to access the sdk functionalities. This object must be initialized before using the sdk. The initialization can be done in the application class. The sdk can be initialized with a [EudiWalletConfig](../-eudi-wallet-config/index.md) object.

Initialize the sdk:

```kotlin
val config = EudiWalletConfig.Builder(appContext)
   .storageDir(appContext.filesDir)
   .encryptDocumentsInStorage(true)
   .useHardwareToStoreKeys(true)
   .userAuthenticationRequired(true)
   .userAuthenticationTimeOut(30_000L)
   .trustedReaderCertificates(listof<X509Certificate>(
     // add trusted reader certificates
   ))
   .bleTransferMode(EudiWalletConfig.BLE_SERVER_PERIPHERAL_MODE)
   .build()
EudiWalletSDK.init(context, config)
```

#### See also

| | |
|---|---|
| [EudiWalletConfig](../-eudi-wallet-config/index.md) | on how to configure the sdk |

## Functions

| Name | Summary |
|---|---|
| [addDocument](add-document.md) | [androidJvm]<br>fun [addDocument](add-document.md)(request: IssuanceRequest, data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): AddDocumentResult<br>Add a document to the wallet |
| [addTransferEventListener](add-transfer-event-listener.md) | [androidJvm]<br>fun [addTransferEventListener](add-transfer-event-listener.md)(listener: TransferEvent.Listener): [EudiWalletSDK](index.md)<br>Adds a transfer event listener in order to be notified about transfer events |
| [createIssuanceRequest](create-issuance-request.md) | [androidJvm]<br>fun [createIssuanceRequest](create-issuance-request.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), hardwareBacked: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), attestationChallenge: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)? = null): CreateIssuanceRequestResult<br>Create an issuance request for the given [docType](create-issuance-request.md) |
| [createResponse](create-response.md) | [androidJvm]<br>fun [createResponse](create-response.md)(disclosedDocuments: DisclosedDocuments): ResponseResult<br>Creates a response for the given [disclosedDocuments](create-response.md) that will be sent to the reader |
| [deleteDocumentById](delete-document-by-id.md) | [androidJvm]<br>fun [deleteDocumentById](delete-document-by-id.md)(documentId: DocumentId): DeleteDocumentResult<br>Delete the document with the given [documentId](delete-document-by-id.md) |
| [getDocumentById](get-document-by-id.md) | [androidJvm]<br>fun [getDocumentById](get-document-by-id.md)(documentId: DocumentId): Document?<br>Returns the document with the given [documentId](get-document-by-id.md) |
| [getDocuments](get-documents.md) | [androidJvm]<br>fun [getDocuments](get-documents.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Document&gt;<br>Returns the list of documents |
| [init](init.md) | [androidJvm]<br>fun [init](init.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), config: [EudiWalletConfig](../-eudi-wallet-config/index.md))<br>Initialize the sdk with the given [config](init.md) |
| [loadSampleData](load-sample-data.md) | [androidJvm]<br>fun [loadSampleData](load-sample-data.md)(sampleData: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): LoadSampleResult<br>Loads sample data into the wallet's document manager |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [androidJvm]<br>fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): [EudiWalletSDK](index.md)<br>Removes all transfer event listeners. |
| [removeTransferEventListener](remove-transfer-event-listener.md) | [androidJvm]<br>fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): [EudiWalletSDK](index.md)<br>Removes a transfer event listener. |
| [sendResponse](send-response.md) | [androidJvm]<br>fun [sendResponse](send-response.md)(responseBytes: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html))<br>Sends the given [responseBytes](send-response.md) to the reader |
| [setReaderTrustStore](set-reader-trust-store.md) | [androidJvm]<br>fun [setReaderTrustStore](set-reader-trust-store.md)(readerTrustStore: ReaderTrustStore): [EudiWalletSDK](index.md)<br>Sets the reader trust store with the readers' certificates that are trusted by the wallet |
| [setTrustedReaderCertificates](set-trusted-reader-certificates.md) | [androidJvm]<br>fun [setTrustedReaderCertificates](set-trusted-reader-certificates.md)(@[RawRes](https://developer.android.com/reference/kotlin/androidx/annotation/RawRes.html)vararg rawRes: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [EudiWalletSDK](index.md)<br>Sets the readers' certificates from raw resources that are trusted by the wallet<br>[androidJvm]<br>fun [setTrustedReaderCertificates](set-trusted-reader-certificates.md)(trustedReaderCertificates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [EudiWalletSDK](index.md)<br>Sets the readers' certificates that are trusted by the wallet |
| [startEngagementToApp](start-engagement-to-app.md) | [androidJvm]<br>fun [startEngagementToApp](start-engagement-to-app.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))<br>Starts the transfer process by engaging with the reader via appLink |
| [startQrEngagement](start-qr-engagement.md) | [androidJvm]<br>fun [startQrEngagement](start-qr-engagement.md)()<br>Starts the transfer process by engaging with the reader via QR code |
| [stopPresentation](stop-presentation.md) | [androidJvm]<br>fun [stopPresentation](stop-presentation.md)(sendSessionTerminationMessage: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, useTransportSpecificSessionTermination: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false)<br>Stops the transfer process |

## Properties

| Name | Summary |
|---|---|
| [config](config.md) | [androidJvm]<br>val [config](config.md): [EudiWalletConfig](../-eudi-wallet-config/index.md)<br>The Config that used to initialize the sdk |
| [documentManager](document-manager.md) | [androidJvm]<br>val [documentManager](document-manager.md): SampleDocumentManager<br>Document manager |
| [openId4vpManager](open-id4vp-manager.md) | [androidJvm]<br>val [openId4vpManager](open-id4vp-manager.md): [OpenId4vpManager](../../eu.europa.ec.eudi.wallet.transfer.openid4vp/-open-id4vp-manager/index.md)<br>OpenId4VP manager that can be used to verify OpenId4Vp requests |
| [transferManager](transfer-manager.md) | [androidJvm]<br>val [transferManager](transfer-manager.md): TransferManager<br>Transfer manager |
