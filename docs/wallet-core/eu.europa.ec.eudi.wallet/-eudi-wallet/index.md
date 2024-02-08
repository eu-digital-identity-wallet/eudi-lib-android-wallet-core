//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)

# EudiWallet

object [EudiWallet](index.md)

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
EudiWallet.init(context, config)
```

#### See also

| | |
|---|---|
| [EudiWalletConfig](../-eudi-wallet-config/index.md) | on how to configure the sdk |

## Functions

| Name | Summary |
|---|---|
| [addDocument](add-document.md) | [androidJvm]<br>fun [addDocument](add-document.md)(request: IssuanceRequest, data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): AddDocumentResult<br>Add a document to the wallet |
| [addTransferEventListener](add-transfer-event-listener.md) | [androidJvm]<br>fun [addTransferEventListener](add-transfer-event-listener.md)(listener: TransferEvent.Listener): [EudiWallet](index.md)<br>Adds a transfer event listener in order to be notified about transfer events |
| [createIssuanceRequest](create-issuance-request.md) | [androidJvm]<br>fun [createIssuanceRequest](create-issuance-request.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), hardwareBacked: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), attestationChallenge: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)? = null): CreateIssuanceRequestResult<br>Create an issuance request for the given [docType](create-issuance-request.md) |
| [deleteDocumentById](delete-document-by-id.md) | [androidJvm]<br>fun [deleteDocumentById](delete-document-by-id.md)(documentId: DocumentId): DeleteDocumentResult<br>Delete the document with the given [documentId](delete-document-by-id.md) |
| [disableNFCEngagement](disable-n-f-c-engagement.md) | [androidJvm]<br>fun [disableNFCEngagement](disable-n-f-c-engagement.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html))<br>Disables the NFC engagement functionality |
| [enableNFCEngagement](enable-n-f-c-engagement.md) | [androidJvm]<br>fun [enableNFCEngagement](enable-n-f-c-engagement.md)(activity: [ComponentActivity](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity.html))<br>Enables the NFC engagement functionality You must also add [DefaultNfcEngagementService](../../eu.europa.ec.eudi.wallet.util/-default-nfc-engagement-service/index.md) to your application's manifest file |
| [getDocumentById](get-document-by-id.md) | [androidJvm]<br>fun [getDocumentById](get-document-by-id.md)(documentId: DocumentId): Document?<br>Returns the document with the given [documentId](get-document-by-id.md) |
| [getDocuments](get-documents.md) | [androidJvm]<br>fun [getDocuments](get-documents.md)(): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Document&gt;<br>Returns the list of documents |
| [init](init.md) | [androidJvm]<br>fun [init](init.md)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), config: [EudiWalletConfig](../-eudi-wallet-config/index.md))<br>Initialize the sdk with the given [config](init.md) |
| [issueDocument](issue-document.md) | [androidJvm]<br>@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)<br>fun [issueDocument](issue-document.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? = null, callback: [OpenId4VciManager.OnIssueCallback](../../eu.europa.ec.eudi.wallet.document.issue.openid4vci/-open-id4-vci-manager/-on-issue-callback/index.md))<br>Issue a document with the given [docType](issue-document.md) using OpenId4Vci protocol |
| [loadSampleData](load-sample-data.md) | [androidJvm]<br>fun [loadSampleData](load-sample-data.md)(sampleData: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): LoadSampleResult<br>Loads sample data into the wallet's document manager |
| [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md) | [androidJvm]<br>fun [removeAllTransferEventListeners](remove-all-transfer-event-listeners.md)(): [EudiWallet](index.md)<br>Removes all transfer event listeners. |
| [removeTransferEventListener](remove-transfer-event-listener.md) | [androidJvm]<br>fun [removeTransferEventListener](remove-transfer-event-listener.md)(listener: TransferEvent.Listener): [EudiWallet](index.md)<br>Removes a transfer event listener. |
| [resolveRequestUri](resolve-request-uri.md) | [androidJvm]<br>fun [resolveRequestUri](resolve-request-uri.md)(openid4VpURI: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Start engagement for OpenId4Vp |
| [sendResponse](send-response.md) | [androidJvm]<br>fun [sendResponse](send-response.md)(disclosedDocuments: DisclosedDocuments): ResponseResult<br>Send a response by giving DisclosedDocuments, i.e. the list of documents to be disclosed. The method returns a `ResponseResult` object, which can be one of the following: |
| [setReaderTrustStore](set-reader-trust-store.md) | [androidJvm]<br>fun [setReaderTrustStore](set-reader-trust-store.md)(readerTrustStore: ReaderTrustStore): [EudiWallet](index.md)<br>Sets the reader trust store with the readers' certificates that are trusted by the wallet |
| [setTrustedReaderCertificates](set-trusted-reader-certificates.md) | [androidJvm]<br>fun [setTrustedReaderCertificates](set-trusted-reader-certificates.md)(@[RawRes](https://developer.android.com/reference/kotlin/androidx/annotation/RawRes.html)vararg rawRes: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)): [EudiWallet](index.md)<br>Sets the readers' certificates from raw resources that are trusted by the wallet<br>[androidJvm]<br>fun [setTrustedReaderCertificates](set-trusted-reader-certificates.md)(trustedReaderCertificates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[X509Certificate](https://developer.android.com/reference/kotlin/java/security/cert/X509Certificate.html)&gt;): [EudiWallet](index.md)<br>Sets the readers' certificates that are trusted by the wallet |
| [startEngagementFromIntent](start-engagement-from-intent.md) | [androidJvm]<br>fun [startEngagementFromIntent](start-engagement-from-intent.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))<br>Start engagement from an Intent. This will perform engagement for REST API or OpenId4Vp depending on the scheme. |
| [startEngagementToApp](start-engagement-to-app.md) | [androidJvm]<br>fun [startEngagementToApp](start-engagement-to-app.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))<br>Start engagement for REST API |
| [startQrEngagement](start-qr-engagement.md) | [androidJvm]<br>fun [startQrEngagement](start-qr-engagement.md)()<br>Starts the transfer process by engaging with the reader via QR code |
| [stopPresentation](stop-presentation.md) | [androidJvm]<br>fun [stopPresentation](stop-presentation.md)(sendSessionTerminationMessage: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, useTransportSpecificSessionTermination: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false)<br>Stops the transfer process |

## Properties

| Name | Summary |
|---|---|
| [config](config.md) | [androidJvm]<br>val [config](config.md): [EudiWalletConfig](../-eudi-wallet-config/index.md)<br>The Config that used to initialize the sdk |
| [documentManager](document-manager.md) | [androidJvm]<br>val [documentManager](document-manager.md): SampleDocumentManager<br>Document manager |
