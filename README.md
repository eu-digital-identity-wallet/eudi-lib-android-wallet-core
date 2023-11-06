# EUDI Wallet Reference Implementation

:heavy_exclamation_mark: **Important!** Before you proceed, please read
the [EUDI Wallet Reference Implementation project description](https://github.com/eu-digital-identity-wallet/.github-private/blob/main/profile/reference-implementation.md)

----

# EUDI ISO 18013-5 Wallet Core library for Android

## Overview

This repository contains the EUDI ISO 18013-5 Wallet Core library for Android. The library is a part
of the EUDI Wallet Reference Implementation project.

The library provides the following functionality:

- Secure Document storage
- Managing documents
- Issuing/Adding documents
- Loading sample documents for testing purposes
- Offline Document transfer between devices over BLE according to the ISO 18013-5 specification
- Device Retrieval to a website according to the ISO 18013-7 specification
- Device Retrieval using OpenID4VP for preregistered verifiers

## Disclaimer

The released software is a initial development release version:

- The initial development release is an early endeavor reflecting the efforts of a short timeboxed
  period, and by no means can be considered as the final product.
- The initial development release may be changed substantially over time, might introduce new
  features but also may change or remove existing ones, potentially breaking compatibility with your
  existing code.
- The initial development release is limited in functional scope.
- The initial development release may contain errors or design flaws and other problems that could
  cause system or other failures and data loss.
- The initial development release has reduced security, privacy, availability, and reliability
  standards relative to future releases. This could make the software slower, less reliable, or more
  vulnerable to attacks than mature software.
- The initial development release is not yet comprehensively documented.
- Users of the software must perform sufficient engineering and additional testing in order to
  properly evaluate their application and determine whether any of the open-sourced components is
  suitable for use in that application.
- We strongly recommend to not put this version of the software into production use.
- Only the latest version of the software will be supported

## Requirements

- Android 8 (API level 26) or higher

### Dependencies

To include the library in your project, add the following dependencies to your app's build.gradle
file.

```groovy
dependencies {
    implementation "eu.europa.ec.eudi:eudi-lib-android-wallet-core:0.2.0-SNAPSHOT"
    implementation "androidx.biometric:biometric-ktx:1.2.0-alpha05"
}
```

## How to Use

### Initialize the library

The library must be initialized before it can be used. The initialization must be done only once in
the application.

In order to initialize the library, you need to provide a configuration object. The configuration
object is created using the `EudiWalletConfig.Builder` class. The builder allows you to configure
the following options:

- `documentsStorageDir` method allows you to specify the directory where the documents are stored.
  The default value is the application's no-backup files directory.
- `encryptDocumentsInStorage` method allows you to specify if the documents should be encrypted in
  the storage. The default value is `true`.
- `useHardwareToStoreKeys` method allows you to specify if the StrongBox Android Keystore should be
  used, if available. The default value is `true`.
- `bleTransferMode` method allows you to specify the BLE transfer mode. The BLE transfer mode can
  be one of the following:
    + `EudiWalletConfig.BLE_SERVER_PERIPHERAL_MODE` - the device will act as a BLE server
    + `EudiWalletConfig.BLE_CLIENT_CENTRAL_MODE` - the device will act as a BLE client
      The default value is `EudiWalletConfig.BLE_SERVER_PERIPHERAL_MODE`.
- `bleClearCacheEnabled` method allows you to specify if the BLE cache should be cleared after the
  transfer. The default value is `false`.
- `userAuthenticationRequired` method allows you to specify if the user authentication is required
  when using documents' keys. The default value is `false`.
- `userAuthenticationTimeOut` method allows you to specify the user authentication timeout in
  milliseconds. If the value is 0, the user authentication is required for every use of the key,
  otherwise it's required within the given amount of milliseconds. The default value is 30000.
- `trustedReaderCertificates` method allows you to specify the list of trusted reader certificates.
  The default value is an empty list.
- `openId4VpVerifierApiUri` method allows you to specify the verifier api uri for OpenID4VP. The
  default value is null.

The following example shows how to initialize the library:

```kotlin
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.EudiWalletSDK
import java.security.cert.X509Certificate

val storageDir = applicationContext.filesDir
val verifierApiUri = "https://verifier-api-uri"
val config = EudiWalletConfig.Builder(applicationContext)
    .bleTransferMode(
        EudiWalletConfig.BLE_SERVER_PERIPHERAL_MODE,
        EudiWalletConfig.BLE_CLIENT_CENTRAL_MODE
    )
    .trustedReaderCertificates(
        listOf<X509Certificate>(
            // list of trusted reader certificates
        )
    )
    .documentsStorageDir(storageDir)
    .encryptDocumentsInStorage(true)
    .userAuthenticationRequired(false)
    .userAuthenticationTimeOut(30_000L)
    .useHardwareToStoreKeys(true)
    .openId4VpVerifierApiUri(verifierApiUri)
    .build()

EudiWalletSDK.init(applicationContext, config)
```

To initialize the library with the default configuration, you can use the following code:

```kotlin
val config = EudiWalletConfig.Builder(applicationContext).build()
EudiWalletSDK.init(applicationContext, config)
```

Library initialization is recommended to be done in the `Application.onCreate` method.

### Manage documents

Document is a data structure that contains the following information:

- `id` document's unique identifier
- `docType` document's docType (example: "eu.europa.ec.eudiw.pid.1")
- `name` document's name. This is a human readable name.
- `hardwareBacked` document's storage is hardware backed
- `createdAt` document's creation date
- `requiresUserAuth` flag that indicates if the document requires user authentication to be accessed
- `nameSpacedData` retrieves the document's data, grouped by nameSpace. Values are in CBOR bytes

The library provides a set of methods to work with documents.

#### Listing documents

The `EudiWalletSDK.getDocuments` method that returns the list of documents stored in the library.

The following example shows how to list documents:

```kotlin
val documents = EudiWalletSDK.getDocuments()
```

#### Retrieving a document

The `EudiWalletSDK.getDocumentById` method that returns a document with the given id.

The following example shows how to retrieve a document:

```kotlin
val document = EudiWalletSDK.getDocumentById(documentId)
```

#### Deleting a document

The `EudiWalletSDK.deleteDocumentById` method that deletes a document with the given id.

The following example shows how to delete a document:

```kotlin
val result = EudiWalletSDK.deleteDocumentById(documentId)

when (result) {
    is EudiWalletSDK.DeleteDocumentResult.Success -> {
        // document deleted
    }
    is EudiWalletSDK.DeleteDocumentResult.Failure -> {
        // error
        val cause = result.throwable
    }
}
```

#### Issuing/Adding a document

Adding a document is a two step process. First, you need to create an issuanceRequest using the
method `EudiWalletSDK.createIssuanceRequest`. The issuanceRequest holds the public certificate
that will be used from the issuer to sign the document.

Later, when document's data is available, you can create the document using the
method `EudiWalletSDK.addDocument` to add the document to document storage.

The following example shows how to add a document:

```kotlin
val docType = "eu.europa.ec.eudiw.pid.1"
val hardwareBacked = false
val attestationChallenge = byteArrayOf(
    // attestation challenge bytes
    // provided by the issuer
)
val requestResult =
    EudiWalletSDK.createIssuanceRequest(docType, hardwareBacked, attestationChallenge)
when (requestResult) {
    is CreateIssuanceRequestResult.Failure -> {
        val error = requestResult.throwable
        // handle error while creating issuance request
    }
    is CreateIssuanceRequestResult.Success -> {
        val request = requestResult.issuanceRequest
        val docType = request.docType
        // the device certificate that will be used in the signing of the document
        // from the issuer while creating the MSO (Mobile Security Object)
        val certificateNeedAuth = request.certificateNeedAuth

        // ... code that sends certificate to issuer and receives document's data

        val issuerData: ByteArray = byteArrayOf() // CBOR bytes received from issuer

        val addResult = EudiWalletSDK.addDocument(request, issuerData)

        when (addResult) {
            is AddDocumentResult.Failure -> {
                val error = addResult.throwable
                // handle error while adding document
            }
            is AddDocumentResult.Success -> {
                val documentId = addResult.documentId
                // the documentId of the newly added document
                // use the documentId to retrieve the document
                documentManager.getDocumentById(documentId)
            }
        }
    }
}
```

Document's data must be in CBOR bytes that has the following structure:

```cddl
Data = {
 "documents" : [+Document] ; Returned documents
}
Document = {
 "docType" : DocType, ; Document type returned
 "issuerSigned" : IssuerSigned ; Returned data elements signed by the issuer
}
IssuerSigned = {
 "nameSpaces" : IssuerNameSpaces, ; Returned data elements
 "issuerAuth" : IssuerAuth ; Contains the mobile security object (MSO) for issuer data authentication
}
IssuerNameSpaces = { ; Returned data elements for each namespace
 + NameSpace => [ + IssuerSignedItemBytes ]
}
IssuerSignedItemBytes = #6.24(bstr .cbor IssuerSignedItem)
IssuerSignedItem = {
 "digestID" : uint, ; Digest ID for issuer data authentication
 "random" : bstr, ; Random value for issuer data authentication
 "elementIdentifier" : DataElementIdentifier, ; Data element identifier
 "elementValue" : DataElementValue ; Data element value
}
IssuerAuth = COSE_Sign1 ; The payload is MobileSecurityObjectBytes
```

Currently, only one document can be added at a time.

#### Loading sample documents

The library provides the `EudiWalletSDK.loadSampleData` method that allows to load sample documents
in document storage, without the need to issue them. The signing of the documents is done by the
library internally, using a predefined key pair.

This method is intended for testing purposes only.

The following example shows how to load sample documents:

```kotlin
val sampleData = byteArrayOf() // CBOR bytes with sample documents
val result = EudiWalletSDK.loadSampleData(sampleData)

when (result) {
    is LoadSampleResult.Success -> {
        // sample documents loaded
    }
    is LoadSampleResult.Failure -> {
        // error
        val cause = result.throwable
    }
}
```

Sample documents must be in CBOR format with the following structure:

```cddl
Data = {
 "documents" : [+Document] ; Sample documents
}
Document = {
 "docType" : DocType, ; Document type returned
 "issuerSigned" : IssuerSigned ; Data elements
}
IssuerSigned = {
 "nameSpaces" : IssuerNameSpaces, ; Returned data elements
}
IssuerNameSpaces = { ; Returned data elements for each namespace
 + NameSpace => [ + IssuerSignedItemBytes ]
}
IssuerSignedItem = {
 "digestID" : uint, ; Digest ID for issuer data authentication
 "random" : bstr, ; Random value for issuer data authentication
 "elementIdentifier" : DataElementIdentifier, ; Data element identifier
 "elementValue" : DataElementValue ; Data element value
}
```

### Transfer documents

The library supports the following 3 ways to transfer documents:

1. Offline document transfer between devices over BLE, according to the ISO 18013-5 specification
    - Device engagement using QR code
    - NFC device engagement
2. Document retrieval to a website according to the ISO 18013-7 specification
    - RestAPI using app link
3. Document retrieval using OpenID4VP for preregistered verifiers

Transfer process is asynchronous. During the transfer, events are emitted that indicate the current
state of the transfer. The following events are emitted:

1. `TransferEvent.QrEngagementReady`: The QR code is ready to be displayed. Get the QR code from
   `event.qrCode`.
2. `TransferEvent.Connecting`: The devices are connecting. Use this event to display a progress
   indicator.
3. `TransferEvent.Connected`: The devices are connected.
4. `TransferEvent.RequestReceived`: A request is received. Get the request from `event.request`.
5. `TransferEvent.ResponseSent`: A response is sent.
6. `TransferEvent.Disconnected`: The devices are disconnected.
7. `TransferEvent.Error`: An error occurred. Get the `Throwable` error from `event.error`.

#### Attaching a TransferEvent.Listener

To receive events from the `EudiWalletSDK`, you must attach a `TransferEvent.Listener` to it:

The following example demonstrates how to implement a `TransferEvent.Listener` and attach it to the
`EudiWalletSDK` object.

```kotlin

val transferEventListener = TransferEvent.Listener { event ->
    when (event) {
        is TransferEvent.QrEngagementReady -> {
            // event when the qr code is ready to be displayed. Get the qr code from event.qrCode
        }

        is TransferEvent.Connected -> {
            // event when the devices are connected
        }

        is TransferEvent.RequestReceived -> {
            // event when a request is received. Get the request from event.request
            // use the received request to generate the appropriate response

            val disclosedDocuments = DisclosedDocuments(
                listOf(
                    // add the disclosed documents here
                )
            )
            when (val responseResult = transferManager.createResponse(disclosedDocuments)) {
                is ResponseResult.Failure -> {
                    // handle the failure
                }
                is ResponseResult.Response -> {
                    val responseBytes = responseResult.bytes
                    EudiWalletSDK.sendResponse(responseBytes)
                }
                is ResponseResult.UserAuthRequired -> {
                    // user authentication is required. Get the crypto object from responseResult.cryptoObject
                    val cryptoObject = responseResult.cryptoObject
                }
            }
        }

        is TransferEvent.ResponseSent -> {
            // event when a response is sent
        }

        is TransferEvent.Disconnected -> {
            // event when the devices are disconnected
            // presentation can be stopped here
            EudiWalletSDK.stopPresentation()
        }
        is TransferEvent.Error -> {
            // event when an error occurs. Get the error from event.error
            val error: Throwable = event.error
            // handle error 
            // stop presentation
            EudiWalletSDK.stopPresentation()
        }
    }
}

EudiWalletSDK.addTransferEventListener(transferEventListener)
```

#### Initiating transfer

1. BLE transfer using QR Engagement

   Once the a transfer event listener is attached, use the `EudiWalletSDK.startQrEngagement()`
   method to start the QR code engagement.

    ```kotlin
    EudiWalletSDK.startQrEngagement()
    
    //... other code
    
    // in event listener when the qr code is ready to be displayed
    when (event) {
        is TransferEvent.QrEngagementReady -> {
            // show the qr code to the user
            val qrCode: QrCode = event.qrCode
            val qrBitmap = qrCode.asBitmap(/* size */) // get the qr code as bitmap
            // - or -
            val qrView = qrCode.asView(/* context, width */) // get the qr code as view
        }
        // ... rest of the code
    }
    ```
2. BLE transfer using NFC Engagement

   In order to use NFC, you must create a service that extends `NfcEngagementService` and override
   the `transferManager` property.

   For example:

    ```kotlin
    package com.example.myapp
    
    import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
    
    class NfcEngagementServiceImpl : NfcEngagementService() {
        override val transferManager: TransferManager
            get() = TODO("Return the transfer manager here")
    }
    ```

   Then add the service to your application's manifest file, like shown below:

    ```xml
    
    <application>
        <!-- rest of manifest -->
        <service android:exported="true" android:label="@string/nfc_engagement_service_desc"
            android:name="com.example.myapp.NfcEngagementServiceImpl"
            android:permission="android.permission.BIND_NFC_SERVICE">
            <intent-filter>
                <action android:name="android.nfc.action.NDEF_DISCOVERED" />
                <action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE" />
            </intent-filter>
    
            <!-- the following "@xml/nfc_engagement_apdu_service" in meta-data is provided by the library -->
            <meta-data android:name="android.nfc.cardemulation.host_apdu_service"
                android:resource="@xml/nfc_engagement_apdu_service" />
        </service>
    
    </application>
    ```

   You can enable or disable the NFC device engagement in your app by calling the `enable()`
   and `disable()` methods of the `NfcEngagementService` class.

   In the example below, the NFC device engagement is enabled when activity is resumed and disabled
   when the activity is paused.

    ```kotlin
    import androidx.appcompat.app.AppCompatActivity
    import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService
    
    class MainActivity : AppCompatActivity() {
    
        override fun onResume() {
            super.onResume()
            NfcEngagementService.enable(this)
        }
    
        override fun onPause() {
            super.onPause()
            NfcEngagementService.disable(this)
        }
    }
    ```
   Optionally, in the `enable()` method you can define your class that
   implements `NfcEngagementService`, e.g.:

    ```kotlin
     NfcEngagementService.enable(this, NfcEngagementServiceImpl::class.java)
    ```

   This way, you can define the `NfcEngagementServiceImpl` service to be preferred while this
   activity is in the foreground.

3. RestAPI using app link

   To enable 18013-7 REST API functionality, declare to your app's manifest file
   (AndroidManifest.xml) an Intent Filter for your MainActivity:

    ```xml
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="mdoc" android:host="*" />
    </intent-filter>
    ```

   and set `launchMode="singleTask"` for this activity.

   To initiate the transfer using an app link (reverse engagement), use
   the `EudiWalletSDK.startEngagementToApp(Intent)` method.

   The method receives as a parameter an `Intent` that contains the data for the device engagement.

   The example below demonstrates how to use the `EudiWalletSDK.startEngagementToApp(Intent)` method
   to initiate the device engagement and transfer.

    ```kotlin
    class MainActivity : AppCompatActivity() {
    
        // ... rest of activity code
    
        override fun onResume() {
            super.onResume()
            EudiWalletSDK.startEngagementToApp(intent)
        }
    
        override fun onNewIntent(intent: Intent) {
            super.onNewIntent(intent)
            EudiWalletSDK.startEngagementToApp(intent)
        }
    }
    ```

4. OpenID4VP

   To use the OpenID4VP functionality, the configuration that is used to initialize the library
   must contain the `openId4VpVerifierApiUri`. See
   the [Initialize the library](#initialize-the-library) section.

   Then, declare to your app's manifest file (AndroidManifest.xml) the following Intent Filters for
   your MainActivity:

   ```xml
   <intent-filter>
       <action android:name="android.intent.action.VIEW" />
       <category android:name="android.intent.category.DEFAULT" />
       <category android:name="android.intent.category.BROWSABLE" />
       <data android:scheme="mdoc-openid4vp" android:host="*" />
   </intent-filter> 
   ```

   Also set `launchMode="singleTask"` for this activity.

   Then your MainActivity use the `EudiWalletSDK.openId4vpManager` property to get
   the `OpenId4VpManager` object and use it to initiate the transfer, as shown in the example
   below:

   ```kotlin
   import android.content.Intent
   import android.os.Bundle
   import androidx.appcompat.app.AppCompatActivity
   import eu.europa.ec.eudi.iso18013.transfer.DisclosedDocument
   import eu.europa.ec.eudi.iso18013.transfer.DisclosedDocuments
   import eu.europa.ec.eudi.iso18013.transfer.ResponseResult
   import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
   import eu.europa.ec.eudi.wallet.EudiWalletSDK
   
   class MainActivity : AppCompatActivity() {
    
      val transferEventListener = TransferEvent.Listener { event ->
          when (event) {
              is TransferEvent.RequestReceived -> {
                 val disclosedDocuments = DisclosedDocuments(
                     listOf<DisclosedDocument>(
                         // get disclosed documents from user
                     )
                 )
                 val result = EudiWalletSDK.createResponse(disclosedDocuments)
                 when (result) {
                     is ResponseResult.Response -> {
                         EudiWalletSDK.openId4vpManager.sendResponse(result.bytes)
                         EudiWalletSDK.openId4vpManager.close()
                     }
                     is ResponseResult.UserAuthRequired -> {
                         // perform user authentication 
                         // and try send data again
                     }
   
                     is ResponseResult.Failure -> {
                         val cause = result.throwable
                         // handle failure
                            
                         // close connection
                         EudiWalletSDK.openId4vpManager.close()
                     }
                 }
   
             }
   
             else -> {
                 // rest of event handling
             }
         }
     }
     // ... rest of activity code
    
     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         EudiWalletSDK.openId4vpManager.addTransferEventListener(transferEventListener)
     }
    
     override fun onResume() {
         super.onResume()
         handleOpenId4VpIntent(intent)
     }
    
     override fun onNewIntent(intent: Intent) {
         super.onNewIntent(intent)
         setIntent(null)
         handleOpenId4VpIntent(intent)
     }
   
     private fun handleOpenId4VpIntent(intent: Intent) {
         when (intent.scheme) {
             "mdoc-openid4vp" -> EudiWalletSDK.openId4vpManager.resolveRequestUri(intent.toUri(0))
             else -> {
                 // do nothing
             }
         }
     }
   }
   ```

#### Receiving request and sending response

When a request is received, the `TransferEvent.RequestReceived` event is triggered. The request can
be retrieved from `event.request`.

The request contains a list of `RequestedDocument` objects, which can be used to show the user what
documents are requested. Also, a selectively disclosure option can be implemented using the
requested documents, so user can choose which of the documents to share.

Then, a `DisclosedDocuments` object must be created with the list of documents to be disclosed and
hhe response can be created using the `EudiWalletSDK.createResponse(DisclosedDocuments)` method.

The method returns a `ResponseResult` object, which can be one of the following:

1. `ResponseResult.Failure`: The response creation failed. The error can be retrieved from
   `responseResult.error`.
2. `ResponseResult.Response`: The response was created successfully. The response bytes can be
   retrieved from `responseResult.bytes`.
3. `ResponseResult.UserAuthRequired`: The response creation requires user authentication. The
   `CryptoObject` can be retrieved from `responseResult.cryptoObject`. After success authentication
   the response can be created again, using the `EudiWalletSDK.createResponse(DisclosedDocuments)`
   method.

Finally, when `createResponse(DisclosedDocuments)` returns a `ResponseResult.Response`, the response
can be sent using the `EudiWalletSDK.sendResponse(ByteArray)` method, by getting the response
bytes from `responseResult.bytes`.

The following example demonstrates the above steps:

```kotlin

val transferEventListener = TransferEvent.Listener { event ->
    when (event) {

        is TransferEvent.RequestReceived -> {
            // event when a request is received. Get the request from event.request
            // use the received request to generate the appropriate response

            val disclosedDocuments = DisclosedDocuments(
                listOf(
                    // add the disclosed documents here
                )
            )
            when (val responseResult = EudiWalletSDK.createResponse(disclosedDocuments)) {
                is ResponseResult.Failure -> {
                    // handle the failure
                }
                is ResponseResult.Response -> {
                    val responseBytes = responseResult.bytes
                    EudiWalletSDK.sendResponse(responseBytes)
                }
                is ResponseResult.UserAuthRequired -> {
                    // user authentication is required. Get the crypto object from responseResult.cryptoObject
                    val cryptoObject = responseResult.cryptoObject
                }
            }
        }
        // handle other events
    }
}
```

## How to contribute

We welcome contributions to this project. To ensure that the process is smooth for everyone
involved, follow the guidelines found in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

### Third-party component licenses

See [licenses.md](licenses.md) for details.

### License details

Copyright (c) 2023 European Commission

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.