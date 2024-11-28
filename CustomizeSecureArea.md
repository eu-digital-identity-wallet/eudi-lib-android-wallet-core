# Use wallet core library with custom Key management

The Wallet Core Library supports custom key management through the implementation of SecureArea
interfaces. This document details the process of integrating custom key management with the library.

## Interfaces and classes for custom key management

To enable custom key management, you must implement the following interfaces and classes:

1. The `com.android.identity.securearea.SecureArea` interface abstracts the underlying secure area
   responsible for creating key material and other security-related objects used in identity
   documents. Authentication may be required to access a key, which is managed through the
   `com.android.identity.securearea.KeyLockedException` and
   `com.android.identity.securearea.KeyUnlockData` types. Applications must implement
   SecureArea-specific mechanisms to handle the necessary authentication.
2. The `com.android.identity.securearea.CreateKeySettings` class serves as the base for key creation
   settings. It is compatible with any SecureArea implementation, though most implementations will
   provide their own versions with additional, specific settings.
3. The `com.android.identity.securearea.KeyUnlockData` interface provides the information necessary
   to operate on a key that requires unlocking.
4. The `com.android.identity.securearea.KeyInfo` class contains details about a key. Specific
   SecureArea implementations may extend this class to include additional implementation-specific
   key information.

## Using EudiWallet with custom key management

Using a custom SecureArea implementation involves focusing on three key areas:

1. Configuring the wallet instance to integrate the custom SecureArea implementation.
2. Managing events during the issuance of documents with OpenId4VCI, where key creation and
   unlocking are necessary.
3. Managing events during document presentation with OpenId4VP or proximity presentation via BLE,
   where key unlocking is needed.

For the purposes of this document and the examples provided, we will refer to the following custom
SecureArea implementations:

1. `MySecureArea` class implementing the `SecureArea` interface.
2. `MyCreateKeySettings` class extending the `CreateKeySettings` class.
3. `MyKeyUnlockData` class implementing the `KeyUnlockData` interface.
4. `MyKeyInfo` class extending the `KeyInfo` class.

### Configure EudiWallet to use custom key management

To register the `MySecureArea` implementation with the Wallet Core Library, you must configure the
wallet instance as shown below:

```kotlin

val walletConfig = EudiWalletConfig()
    .configureOpenId4Vci {
        // Configuration for OpenId4VCI
    }
    .configureOpenId4Vp {
        // Configuration for OpenId4VP
    }
val wallet = EudiWallet(context, walletConfig) {
    withSecureAreas(
        listOf(
            MySecureArea()
        )
    )
}
```

For detailed information on EudiWallet configuration, please refer to
the [EudiWallet documentation](https://github.com/eu-digital-identity-wallet/eudi-lib-android-wallet-core?tab=readme-ov-file#initialize-the-library)

*Note*:If the `MySecureArea` instance is required, it can be retrieved from the wallet instance
using the following code:

```kotlin
val mySecureArea = wallet.secureAreaRepository.implementations
    .filterIsInstance<MySecureArea>()
    .first()
```

### How to use custom key management with OpenId4VCI

When issuing a document with OpenId4VCI, the Wallet Core Library triggers events that require custom
key management. There are two key events that necessitate this:

1. **`IssueEvent.DocumentRequiresCreateSettings`**: This event occurs when creating a document for a
   specific offered document that requires a key. In the case of custom key management, you must
   create the corresponding `CreateDocumentSettings` object to accompany the `SecureArea`
   implementation. This object defines in which `SecureArea` and how the key should be created.
   The `createDocumentSettings` is then used to resume the issuance process.

2. **`IssueEvent.DocumentRequiresUserAuth`**: This event is triggered when key unlocking is needed
   to sign the proof of possession for the document's key, which is required to issue the document.
   To unlock the key, you must create the appropriate `KeyUnlockData` object that corresponds to the
   `SecureArea` implementation. This object specifies how the key should be unlocked (e.g., via
   password, biometric data, or authorization tokens). The `keyUnlockData` is then used to resume
   the issuance process.

The following code snippet demonstrates how to handle these events with custom key management using
the custom `MySecureArea` implementation:

```kotlin
val onIssueEvent = OnIssueEvent { event ->
    when (event) {
        is IssueEvent.DocumentRequiresCreateSettings -> {
            // triggered when creating a document for a given offered document
            val offeredDocument = event.offeredDocument
            // create the createDocumentSettings for the document
            // using custom implementation MySecureArea for keys
            val mySecureArea = wallet.secureAreaRepository.implementations
                .filterIsInstance<MySecureArea>()
                .first()
            val createDocumentSettings = CreateDocumentSettings(
                // set the identifier of the desired secure area
                secureAreaIdentifier = mySecureArea.identifier,
                // set the CreateKeySettings for the key based on the secure area
                createKeySettings = MyCreateKeySettings(
                    // set custom settings for creating the key
                    // with MySecureArea implementation
                )
            )
            // resume the issuance process with the createDocumentSettings
            event.resume(createDocumentSettings)
            // or cancel the issuance process
            event.cancel("User canceled the issuance process")
        }
        is IssueEvent.DocumentRequiresUserAuth -> {
            // triggered when user authentication is required to issue a document
            // Holds the document object that requires user authentication
            // and the algorithm that is going to be used for signing the proof of possession,
            // as well as methods for resuming the issuance process or canceling it

            // Document and signing algorithm are provided 
            // to use if required for creating the keyUnlockData
            val signingAlgorithm = event.signingAlgorithm
            val document = event.document

            //
            // --> show prompt if needed to unlock the key <--
            //

            // create the keyUnlockData to unlock the key.
            val keyUnlockData = MyKeyUnlockData(
                // set the extra information for the key unlock data
                // e.g. password, biometric data, authorization tokens etc.
            )
            // to resume the issuance process, after authenticating user,  call
            // resume with the keyUnlockData
            event.resume(keyUnlockData)

            // or cancel the issuance process by calling cancel method
            event.cancel("User canceled the issuance process")
        }
        else -> {
            // handling rest of issuing events
        }
    }
}
```

### How to use custom key management with Presentation

When presenting a document using OpenId4VP or through proximity presentation with BLE, a
`TransferEvent.RequestReceived` event is triggered upon receiving a request. The request contains
the documents and items to be disclosed. To disclose the requested documents, you must unlock the
keys associated with those documents. To do this, you need to create the appropriate `KeyUnlockData`
object, which corresponds to the `SecureArea` implementation. This object specifies how the key
should be unlocked (e.g., using a password, biometric data, authorization tokens, etc.). The
`keyUnlockData` is then used to generate the response.

The following code snippet illustrates how to handle the `TransferEvent.RequestReceived` event and
disclose the requested documents with custom key management using a custom `SecureArea`
implementation when key unlocking is required:

```kotlin
/**
 * Keeps the processed request to be used later for generating the response
 */
lateinit var processedRequest: RequestProcessor.ProcessedRequest.Success

/**
 * Example function that demonstrates how to get information about the requested documents
 * and show them to the user. The function should be called when the request is received.
 */
fun showRequestedDocuments() {
    // shows requested documents to user

    processedRequest.requestedDocuments.forEach { requestedDocument ->
        // get document if needed to show more information such as document's name and docType
        val document =
            wallet.getDocumentById(requestedDocument.documentId) as? IssuedDocument
        val documentName = document?.name
        val docType = (document?.format as? MsoMdocFormat)?.docType

        // show requested fields for this document
        val docItems =
            requestedDocument.requestedItems.forEach { (docItem, intentToRetain) ->
                val nameSpace = docItem.namespace
                val elementIdentifier = docItem.elementIdentifier
            }
    }
}

/**
 * Example function that demonstrates how to generate and send a response based on the
 * selected documents. The function should be called after the user has selected the
 * documents to disclose.
 *
 * @param selectedDocuments A map of selected documents and the selected items to disclose
 */
fun discloseDocuments(selectedDocuments: Map<IssuedDocument, List<DocItem>>) {
    val disclosedDocuments = DisclosedDocuments(
        selectedDocuments.map { (document, docItems) ->

            // should block until the user has unlock the key to sign the response

            // One can use information from document to determine how to
            // create the MyKeyUnlockData object, such as:

            val secureArea = document.secureArea as MySecureArea
            val keyAlias = document.keyAlias
            val keyInfo = document.keyInfo as MyKeyInfo

            //
            // --> show prompt if needed to unlock the key <--
            //

            // create and return the keyUnlockData to unlock the key of the document
            val keyUnlockData = MyKeyUnlockData(
                // set the extra information for the key unlock data
                // e.g. password, biometric data, authorization tokens etc.
            )

            DisclosedDocument(
                documentId = document.id,
                disclosedItems = docItems,
                keyUnlockData = keyUnlockData
            )
        })

    val response = processedRequest.generateResponse(disclosedDocuments).getOrThrow()
    wallet.sendResponse(response)
}

/**
 * Example transfer event listener that demonstrates how to handle the request received event
 * and show the requested documents to the user.
 */
val transferEventListener = TransferEvent.Listener { event ->
    when (event) {

        is TransferEvent.RequestReceived -> try {
            // get the processed request
            processedRequest = event.processedRequest.getOrThrow()
            // the request has been received and processed

            showRequestedDocuments()

        } catch (e: Throwable) {
            // An error occurred
            // handle the error
        }
        // handle other events
        else -> {}
    }
}

// Add the transfer event listener to the wallet
wallet.addTransferEventListener(transferEventListener)
```
