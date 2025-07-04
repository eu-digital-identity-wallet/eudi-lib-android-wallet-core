# Digital Credential API

The **EUDI Wallet Core** library includes support for the [Digital Credential API](https://w3c-fedid.github.io/digital-credentials/).

The current implementation of DCAPI follows the protocol `org-iso-mdoc`, 
according to the [ISO/IEC TS 18013-7:2025](https://www.iso.org/standard/91154.html) **Annex C**.

> **Note:** DCAPI is **disabled by default**. You can enable it in your application by following the steps below.

## Enabling the Digital Credential API

### Register the Intent

In the application's `AndroidManifest.xml` file define an Activity to listen the 
`androidx.credentials.registry.provider.action.GET_CREDENTIAL` intent filter:

```xml
<activity 
    android:name=".MainActivity"
    android:exported="true">
    
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    
    <!--Required for DCAPI -->
    <intent-filter>
        <action android:name="androidx.credentials.registry.provider.action.GET_CREDENTIAL" />
        <action android:name="androidx.identitycredentials.action.GET_CREDENTIALS" />
    
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
    
</activity>
```

### Configure the EudiWallet

Configure and initialize the `EudiWallet` with DCAPI enabled:

```kotlin
val config = EudiWalletConfig()
    .configureDCAPI {
        withEnabled(true) // Enable DCAPI, by default it is disabled
    }
    // ... Rest of your configurations


// Initialize the EudiWallet with the configuration
val eudiWallet = EudiWallet(context, config)
```

In the `DCAPIConfig` you can also set up your own allowlist of privileged browsers/apps that you trust, 
providing a json file of the following format:

```json
{"apps": [
  {
    "type": "android",
    "info": {
      "package_name": "com.example.app",
      "signatures" : [
        {"build": "release",
          "cert_fingerprint_sha256": "59:0D:2D:7B:33:6A:BD:FB:54:CD:3D:8B:36:8C:5C:3A:7D:22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32"
        },
        {"build": "userdebug",
          "cert_fingerprint_sha256": "59:0D:2D:7B:33:6A:BD:FB:54:CD:3D:8B:36:8C:5C:3A:7D:22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32"
        }]
    }
  }
]}
```

You can provide it as a JSON String in the `DCAPIConfig`:

```kotlin
val config = EudiWalletConfig()
    .configureDCAPI {
        withEnabled(true) // Enable DCAPI
        withAllowlistJsonFile(customAllowList) // Provide your custom allowlist
    }
```

Optionally, you can provide your own implementation of `DCAPIRegistration` to handle the 
registration of credentials:

```kotlin
val customWallet = EudiWallet(context, config) {
    // custom DCAPIRegistration implementation
    withDCAPIRegistration(myCustomDCAPIRegistration())
}
```

### Starting the DCAPI Presentation

Call the `startDCAPIPresentation` by passing the `Intent`, e.g.:

```kotlin
fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Initialize EudiWallet
    val eudiWallet = EudiWallet(context, config)

    // Handle the intent for Digital Credential API
    eudiWallet.startDCAPIPresentation(intent)
    
}
```
This method will handle the intent and start the DCAPI presentation process, if it contains a valid DCAPI Request.

### Handling the Transfer Events

Attach a `TransferEvent.Listener` to handle the events during the DCAPI presentation:

```kotlin
eudiWallet.addTransferEventListener { event ->
    when (event) {
        is TransferEvent.RequestReceived -> try {
            // get the processed request
            val processedRequest = event.processedRequest.getOrThrow()
            // the request has been received and processed

            // the request processing was successful
            // requested documents can be shown in the application
            val requestedDocuments = processedRequest.requestedDocuments
            // ...
            // application must create the DisclosedDocuments object
            val disclosedDocuments = DisclosedDocuments(
                // assume that the document is in mso_mdoc format
                DisclosedDocument(
                    documentId = "document-id",
                    disclosedItems = listOf(
                        MsoMdocItem(
                            namespace = "eu.europa.ec.eudi.pid.1",
                            elementIdentifier = "first_name"
                        ),
                    ),
                    // keyUnlockData is required if needed to unlock the key
                    // in order to sign the response
                    keyUnlockData = wallet.getDefaultKeyUnlockData("document-id")
                ),
                // ... rest of the disclosed documents
            )
            // generate the response
            val response = processedRequest.generateResponse(
                disclosedDocuments = disclosedDocuments,
                signatureAlgorithm = Algorithm.ES256
            ).getOrThrow()

            wallet.sendResponse(response)

        } catch (e: Throwable) {
            // An error occurred
            // handle the error
        }
        
        TransferEvent.IntentToSend -> {
            // The response intent is ready
            // You can send the response intent and finish the activity
            setResult(RESULT_OK, event.intent)
            finish()
        }

        is TransferEvent.Error -> {
            // An error has occurred during the Digital Credential API presentation
            // If the error is a DCAPIException, you can handle it
            // by sending the error intent and finishing the activity
            val error = event.error
            if(error is DCAPIException) {
                setResult(RESULT_OK, error.toIntent())
                finish()
            }
        }
        
        else -> { }
    }
}
```

During the DCAPI presentation, the application will receive various events that indicate the state
of the transfer process:

`TransferEvent.RequestReceived`: Indicates that a request has been received and processed.
The processed request can be accessed through `event.processedRequest`. You can get the requested
documents `processedRequest.requestedDocuments` and generate a response using the `processedRequest.generateResponse`
method. See more details in [README](README.md#receiving-a-request-and-sending-a-response).

`TransferEvent.IntentToSend`: Indicates that the response intent `event.intent` is ready. Then you
can send the response intent and finish the activity, as follows:

```kotlin
setResult(RESULT_OK, event.intent)
finish()
```

`TransferEvent.Error`: Indicates that an error has occurred during the DCAPI presentation.
If the `event.error` is a `DCAPIException` you can handle it by sending the error
intent `event.error.toIntent()` and finishing the activity:

```kotlin
val error = event.error
if (error is DCAPIException) {
    setResult(RESULT_OK, error.toIntent())
    finish()
}
```