# EUDI ISO 18013-5 Wallet Transfer library for Android

:heavy_exclamation_mark: **Important!** Before you proceed, please read
the [EUDI Wallet Reference Implementation project description](https://github.com/eu-digital-identity-wallet/.github/blob/main/profile/reference-implementation.md)

## Overview

This library provides a set of classes to manage the transfer of documents in an EUDI ISO 18013-5
Android Wallet.

It defines the interfaces for TransferManager and Transfer classes and provides a standard
implementation of the TransferManager.

Currently, the library supports the following devices engagement methods:

- [x] QR
- [x] NFC tag

and the transfer of the documents is done using Bluetooth Low Energy (BLE).

Additionally, Transfer Manager incorporates reader authentication, ensuring the trustworthiness of
the verifier (reader) device.

Reader authentication is accomplished by verifying the following:

- Certificate **path validation**: Verifying the certificate path from the device's certificate to a
  trusted root certificate
- Certificate's **profile validation**: Examines the attributes and constraints defined in the
  certificate profile to ensure that they meet the predefined criteria for a trusted certificate
- **Revocation checking**: Optional certificate revocation checking via CRL, configurable through
  `RevocationPolicy` (disabled by default for backwards compatibility).
- **Signature verification**: Verifying the signature by generating the `ReaderAuthentication` structure and validating it 
  against the certificate.

The library is written in Kotlin and is available for Android.

## :heavy_exclamation_mark: Disclaimer

The released software is an initial development release version:

- The initial development release is an early endeavor reflecting the efforts of a short timeboxed
  period, and by no
  means can be considered as the final product.
- The initial development release may be changed substantially over time, might introduce new
  features but also may
  change or remove existing ones, potentially breaking compatibility with your existing code.
- The initial development release is limited in functional scope.
- The initial development release may contain errors or design flaws and other problems that could
  cause system or other
  failures and data loss.
- The initial development release has reduced security, privacy, availability, and reliability
  standards relative to
  future releases. This could make the software slower, less reliable, or more vulnerable to attacks
  than mature
  software.
- The initial development release is not yet comprehensively documented.
- Users of the software must perform sufficient engineering and additional testing in order to
  properly evaluate their
  application and determine whether any of the open-sourced components is suitable for use in that
  application.
- We strongly recommend not putting this version of the software into production use.
- Only the latest version of the software will be supported

## Requirements

- Android 8 (API level 26) or higher

### Dependencies

To use snapshot versions, add the following to your project's settings.gradle file:

```groovy
dependencyResolutionManagement {
    repositories {
        // ...
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            mavenContent { snapshotsOnly() }
        }
        // ...
    }
}
```

To include the library in your project, add the following dependencies to your app's build.gradle
file.

```groovy
dependencies {
    implementation "eu.europa.ec.eudi:eudi-lib-android-iso18013-data-transfer:0.14.0"
}
```

## How to Use

Below is a quick overview of how to use the library.

For source code documentation, see in the [docs](docs/index.md) directory.

### Instantiating the TransferManager

The library provides a `TransferManager` implementation that can be used to present documents
using the ISO 18013-5 for proximity presentation.

To create a new instance of the `TransferManager`, you can use the `TransferManager.getDefault`
method.

The following example demonstrates how to create a new instance of the `TransferManager`

```kotlin
val documentManager: DocumentManager =
    TODO("The document manager to retrieve the requested documents")

val readerTrustStore = ReaderTrustStore.getDefault(
    trustedCertificates = listOf(
        // trustedReaderCertificate1,
        // trustedReaderCertificate2
    )
)

val transferManager = TransferManager.getDefault(
    context = context,
    documentManager = documentManager,
    retrievalMethods = listOf(
        BleRetrievalMethod(
            peripheralServerMode = true,
            centralClientMode = false,
            clearBleCache = true,
        )
    ),
    readerTrustStore = readerTrustStore,
    readerAuthPolicy = ReaderAuthPolicy.EnforceIfPresent, // default
)
``` 

### Attaching a TransferEvent.Listener

The transfer process is event-driven. To receive events, you need to attach a
`TransferEvent.Listener` to the `TransferManager`.

The available events are:

1. `TransferEvent.QrEngagementReady`:
   The QR code is ready to be displayed. Get the QR code from
   `TransferEvent.QrEngagementReady.qrCode`.
2. `TransferEvent.Connecting`: The devices are connecting. Use this event to display a progress
   indicator.
3. `TransferEvent.Connected`: the devices are connected.
4. `TransferEvent.RequestReceived`: A request is received. Get the processed request with the
   `TransferEvent.RequestReceived.processedRequest` and the initial raw request as received
   `TransferEvent.RequestReceived.request`.
5. `TransferEvent.ResponseSent`: A response is sent.
6. `TransferEvent.Redirect`: This event prompts to redirect the user to the given Redirect URI. Get
   the Redirect URI with `TransferEvent.Redirect.redirectUri`.
7. `TransferEvent.IntentToSend`: This event indicates that an intent is ready to be sent. Get the
   intent with `TransferEvent.IntentToSend.intent`. This is used mainly for Digital Credential API
   implementation.
8. `TransferEvent.Disconnected`: The devices are disconnected.
9. `TransferEvent.Error`: An error occurred. Get the `Throwable` error with
   `TransferEvent.Error.error`.

The following example demonstrates how to attach a `TransferEvent.Listener` to the
`TransferManager`.
It also demonstrates how to handle the different events.

```kotlin
transferManager.addTransferEventListener { event ->
    when (event) {
        is TransferEvent.QrEngagementReady -> {
            // Qr code is ready to be displayed
            val qrCodeBitmap = event.qrCode.asBitmap(size = 800)
            // or
            val qrCodeView = event.qrCode.asView(context, size = 800)
        }

        TransferEvent.Connecting -> {
            // Informational event that devices are connecting
        }

        TransferEvent.Connected -> {
            // Informational event that the transfer has been connected
        }

        is TransferEvent.RequestReceived -> try {
            // Assuming DeviceRequest is the request type (ISO 18013-5).
            val processedRequest = event.processedRequest.getOrThrow() as ProcessedDeviceRequest

            // Verifier identity & trust verdict — single source of truth.
            val verifierName = processedRequest.trustMetadata?.displayName
            val verifierIsTrusted = processedRequest.trustMetadata != null

            // Walk the presentment tree to surface candidate matches. In ISO 18013-5
            // the tree is effectively flat: one option and one member per credential set.
            val matches = processedRequest.presentmentData
                .credentialSets
                .flatMap { set -> set.options.flatMap { o -> o.members.flatMap { m -> m.matches } } }

            // Build the user's confirmed selection — here we disclose everything matched.
            // For selective (per-claim) disclosure, see the "Receiving a request" section.
            val selection = CredentialPresentmentSelection(matches = listOf(matches.first()))

            // Generate and send the response.
            val response = processedRequest.generateResponse(selection, emptyMap())
                .getOrThrow() as DeviceResponse
            transferManager.sendResponse(response)

        } catch (e: Throwable) {
            // An error occurred
            // handle the error
        }

        TransferEvent.ResponseSent -> {
            // Informational event that the response has been sent
        }
        is TransferEvent.Redirect -> {
            // A redirect is needed. Used mainly for the OpenId4VP implementation
            val redirectUri = event.redirectUri // the redirect URI
        }
        is TransferEvent.IntentToSend -> {
            // An intent is ready to be sent
            // Used mainly for the DCAPI implementation
            val intent = event.intent // the intent to send.
        }
        TransferEvent.Disconnected -> {
            // Informational event that device has been disconnected
        }
        is TransferEvent.Error -> {
            // An error occurred
            val cause = event.error
        }
    }
}
```

### Initiating transfer

Transfer Manager provides the following methods for enabling device engagement and initiating the
transfer:

1. Scanning QR code
2. Using NFC

These engagement methods offer seamless device pairing and data transfer initiation.

#### Using QR Code

With the `TransferManager` instance created, you can initiate the transfer with QR code by calling
the `TransferManager.startQrEngagement()` method. The method initiates the transfer process and
triggers the `TransferEvent.QrEngagementReady` event when the QR code is ready to be displayed.

#### Using NFC

To use NFC, you must create a service that extends the abstract `NfcEngagementService` and override
the `NfcEngagementService.transferManager` property.

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

You can enable or disable the NFC device engagement in your app by calling the
`NfcEngagementService.enable()` and `NfcEngagementService.disable()` methods.

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

Optionally, in the `NfcEngagementService.enable()` method you can define your class that implements
the abstract `NfcEngagementService`.

For example:

```kotlin
 NfcEngagementService.enable(this, NfcEngagementServiceImpl::class.java)
```

This way, you can define the `NfcEngagementServiceImpl` service to be preferred while this activity
is in the foreground.

### Receiving a request and sending a response

When a request is received, the `TransferManager` triggers `TransferEvent.RequestReceived`.
The event carries the processed request and the original raw request bytes.

A successful `ProcessedDeviceRequest` exposes three pieces of state that drive consent and
response generation:

- **`presentmentData: CredentialPresentmentData`** — a tree of candidate credentials that
  satisfy the verifier's request. The wallet UI typically lets the user pick and confirm
  what to share.
- **`requester: Requester`** — who is asking (X.509 certificate chain, optional `appId` and
  web `origin`).
- **`trustMetadata: TrustMetadata?`** — `null` when the requester isn't trust-verified;
  otherwise carries display info (e.g. `displayName`, icon) for the UI.
  `trustMetadata != null` means that the requester is trusted.

The response is produced via
`ProcessedDeviceRequest.generateResponse(selection, keyUnlockData, signatureAlgorithm?)`:

- `selection: CredentialPresentmentSelection` — the user's confirmed picks. Each entry is
  a `CredentialPresentmentSetOptionMemberMatch` whose `claims` map carries the (filtered)
  data elements to disclose.
- `keyUnlockData: Map<String, KeyUnlockData>` — per-credential unlock data, keyed by
  `match.credential.identifier`. Pass an empty map when no key unlock is required.

Finally, the wire response is sent with `TransferManager.sendResponse(response)`.

> **Note:** Currently, only a single request-response cycle per session is supported.
> Sending a response automatically terminates the presentation session. To perform another
> exchange, a new session must be started.

#### Inspecting the request

```kotlin
val processedRequest = event.processedRequest.getOrThrow() as ProcessedDeviceRequest

// Verifier identity & trust
val verifierName = processedRequest.trustMetadata?.displayName     // null if not trusted
val verifierIsTrusted = processedRequest.trustMetadata != null
val requesterCertChain = processedRequest.requester.certChain      // null if no reader auth

// Walk the presentment tree. For ISO 18013-5 it is effectively flat — one option, one
// member per credential set — so a single `flatMap` chain yields all candidate matches.
val matches: List<CredentialPresentmentSetOptionMemberMatch> = processedRequest.presentmentData
    .credentialSets
    .flatMap { set -> set.options.flatMap { o -> o.members.flatMap { m -> m.matches } } }
```

> **ℹ️ Soft matching**: a candidate credential appears in `presentmentData` if it has
> **at least one** of the verifier's requested data elements. Missing elements are silently
> omitted from `match.claims`. The response may therefore be a partial disclosure.

#### Building the disclosure selection

For full disclosure (the user agrees to share everything that matched), just wrap the
chosen match in a selection:

```kotlin
val fullDisclosure = CredentialPresentmentSelection(matches = listOf(matches.first()))
```

For **selective disclosure** (the user picks a subset of the matched claims), narrow the
`match.claims` map with `match.copy`:

```kotlin
val userPickedElements = setOf("given_name", "family_name")
val narrowedMatch = matches.first().copy(
    claims = matches.first().claims.filterKeys { req ->
        req is MdocRequestedClaim && req.dataElementName in userPickedElements
    }
)
val selectiveDisclosure = CredentialPresentmentSelection(matches = listOf(narrowedMatch))
```

The response generator will only sign over the claims that survive this filter.

#### Handling locked credentials

If the credential's signing key requires unlocking (e.g. PIN-protected), provide a
`KeyUnlockData` keyed by `match.credential.identifier`:

```kotlin
val match = matches.first()
val keyUnlockData: Map<String, KeyUnlockData> = mapOf(
    match.credential.identifier to SoftwareKeyUnlockData(passphrase = "1234")
)
val selection = CredentialPresentmentSelection(matches = listOf(match))

val response = processedRequest.generateResponse(selection, keyUnlockData)
    .getOrThrow() as DeviceResponse
```

When no key unlock is needed, pass `emptyMap()`.

#### Sending the response

```kotlin
val response = processedRequest.generateResponse(selection, emptyMap())
    .getOrThrow() as DeviceResponse
transferManager.sendResponse(response)
```

#### Error handling

`generateResponse(...)` can fail when the inputs or wallet state don't satisfy the contract.
The failure is wrapped in `ResponseResult.Failure`; common causes:

| Throwable                       | When it is raised                                                                                                                          |
|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `IllegalArgumentException`      | ISO 18013-5 mDL constraint violated — e.g. more than two `age_over_NN` claims selected for disclosure from the `org.iso.18013.5.1` namespace. |
| `KeyLockedException`            | The credential's signing key is locked and the matching entry in `keyUnlockData` is missing or its passphrase is wrong.                    |
| `IllegalStateException`         | Internal invariant violated — typically credential data that could not be decoded, or a multipaz API contract not met.                     |

Use `result.toKotlinResult()` to convert the result to Kotlin's `Result<ResponseResult.Success>`
if you prefer the standard fold/onSuccess plumbing.

#### End-to-end example

```kotlin
transferManager.addTransferEventListener { event ->
    when (event) {
        is TransferEvent.RequestReceived -> try {
            // DeviceRequest assumed is received
            val processedRequest = event.processedRequest.getOrThrow() as ProcessedDeviceRequest

            // Display verifier info to the user before consent.
            val verifierName = processedRequest.trustMetadata?.displayName

            // Surface matches to the consent UI.
            val matches = processedRequest.presentmentData
                .credentialSets
                .flatMap { it.options.flatMap { o -> o.members.flatMap { m -> m.matches } } }

            // After the user confirms (here: a single match, locked key, software unlock).
            val match = matches.first()
            val selection = CredentialPresentmentSelection(matches = listOf(match))
            val keyUnlockData = mapOf(
                match.credential.identifier to SoftwareKeyUnlockData("passphrase_from_ui")
            )

            val response = processedRequest.generateResponse(selection, keyUnlockData)
                .getOrThrow() as DeviceResponse
            transferManager.sendResponse(response)

        } catch (e: Throwable) {
            // Handle the error — see "Error handling" above for the common types.
        }

        else -> {
            // ... rest of the events
        }
    }
}
```

### Reader authentication

Reader authentication is a critical security component that verifies the authenticity and trustworthiness of the verifier (reader) device before sharing any personal data. It ensures that only legitimate and authorized entities can request and receive sensitive document information.

The library performs three key verification steps:

1. **Certificate Path Validation**: Validates the complete certificate chain from the reader's certificate up to a trusted root certificate, ensuring a chain of trust is established.

2. **Certificate Profile Validation**: Examines multiple attributes and constraints defined in the certificate to ensure it meets predefined security criteria, including:
   - Key Usage validation
   - Extended Key Usage validation
   - Validity period verification
   - Critical extensions check
   - Mandatory extensions verification
   - Authority Key Identifier validation
   - Subject Key Identifier validation
   - Common Name verification
   - Signature algorithm verification

3. **Certificate Revocation Checking**: When enabled via `RevocationPolicy`, the library checks if certificates in the chain have been revoked using CRL distribution points. Revocation checking uses Java's `PKIXRevocationChecker` which verifies CRL signatures, rejects expired CRLs, and checks all certificates in the chain (not just the leaf). Revocation checking is disabled by default for backwards compatibility.

#### Default Implementation

By default, the library provides a standard implementation of the `ReaderTrustStore` interface through `ReaderTrustStoreImpl`, which performs certificate path and profile validation. Revocation checking is disabled by default:

```kotlin
// Create a default ReaderTrustStore with your trusted certificates
val readerTrustStore = ReaderTrustStore.getDefault(
    trustedCertificates = listOf(
        // List your trusted root certificates here
        trustedRootCertificate1,
        trustedRootCertificate2
    )
)

// Assign the trust store to the TransferManager
transferManager = TransferManager.getDefault(
    context = context,
    documentManager = documentManager,
    retrievalMethods = retrievalMethods,
    readerTrustStore = readerTrustStore
)
```

To enable revocation checking, pass a `RevocationPolicy`:

```kotlin
// HardFail: reject if certificate is revoked OR CRL is unavailable
val readerTrustStore = ReaderTrustStore.getDefault(
    trustedCertificates = trustedCertificates,
    revocationPolicy = RevocationPolicy.HardFail
)

// SoftFail: reject if certificate is revoked, but tolerate CRL unavailability
val readerTrustStore = ReaderTrustStore.getDefault(
    trustedCertificates = trustedCertificates,
    revocationPolicy = RevocationPolicy.SoftFail
)
```

#### Reader Authentication Enforcement Policy

The `ReaderAuthPolicy` controls how reader-authentication results gate response generation in
`ProcessedDeviceRequest.generateResponse()`. Reader auth in ISO 18013-5 is signed at the
`DeviceRequest` level (one signature for all `DocRequest`s), so the verdict is **all-or-nothing
per request** — when a policy is violated, the response is an empty `DeviceResponse` with status
`STATUS_GENERAL_ERROR` and no signed documents.

Three policies are available:

| Policy | Behavior |
|---|---|
| `ReaderAuthPolicy.DoNotEnforce` | The response is always produced regardless of reader-auth state. |
| `ReaderAuthPolicy.EnforceIfPresent` | **(Default)** If the verifier supplied a reader cert chain, that chain **must** also be trust-verified (`trustMetadata != null`); otherwise an empty `STATUS_GENERAL_ERROR` response is returned. Requests without any reader auth are allowed through. |
| `ReaderAuthPolicy.AlwaysRequire` | The request must be trust-verified (`trustMetadata != null`); otherwise an empty `STATUS_GENERAL_ERROR` response is returned. |

The policy is configured when creating the `TransferManager`:

```kotlin
val transferManager = TransferManager.getDefault(
    context = context,
    documentManager = documentManager,
    readerTrustStore = readerTrustStore,
    readerAuthPolicy = ReaderAuthPolicy.AlwaysRequire, // strictest mode
)
```

Or using the builder:

```kotlin
val transferManager = TransferManagerImpl.Builder(context)
    .documentManager(documentManager)
    .readerTrustStore(readerTrustStore)
    .readerAuthPolicy(ReaderAuthPolicy.AlwaysRequire)
    .build()
```

#### Custom Implementation

For advanced use cases, you can create your own custom implementation of the `ReaderTrustStore` interface:

```kotlin
class CustomReaderTrustStore(
    private val trustedCertificates: List<X509Certificate>
) : ReaderTrustStore {

    override fun createCertificationTrustPath(chain: List<X509Certificate>): List<X509Certificate>? {
        // Your custom implementation for creating trust path
        // Return the certification path or null if no path can be created
    }

    override fun validateCertificationTrustPath(chainToDocumentSigner: List<X509Certificate>): Boolean {
        // Your custom validation logic
        // Return true if the certification path is valid, false otherwise
    }
}

// Use your custom implementation
val customTrustStore = CustomReaderTrustStore(trustedCertificates)
transferManager.readerTrustStore = customTrustStore
```

You can also implement custom profile validations by implementing the `ProfileValidation` interface and providing it to the `ReaderTrustStoreImpl`:

```kotlin
class CustomProfileValidation : ProfileValidation {
    override fun validate(chain: List<X509Certificate>, trustCA: X509Certificate): Boolean {
        // Your custom validation logic
        return true
    }
}

// Create a ReaderTrustStoreImpl with custom profile validation
val customValidation = CustomProfileValidation()
val readerTrustStore = ReaderTrustStoreImpl(
    trustedCertificates = trustedCertificates,
    profileValidation = customValidation
)
```

Reader-authentication state is exposed on `ProcessedDeviceRequest` through two complementary
properties:

- **`requester: Requester`** — identifies who is asking. `requester.certChain` is the X.509
  chain the verifier presented (or `null` if no reader auth was attempted); `requester.appId`
  and `requester.origin` are populated for app-bound and web flows respectively.
- **`trustMetadata: TrustMetadata?`** — present (non-null) when the request is fully trust-verified.
  Carries displayName, icon, and privacy-policy URL for the consent UI.

```kotlin
is TransferEvent.RequestReceived -> {
    val processedRequest = event.processedRequest.getOrThrow() as ProcessedDeviceRequest

    val requester = processedRequest.requester
    val trustMetadata = processedRequest.trustMetadata

    when {
        trustMetadata != null -> {
            // Trusted reader — display name, icon, privacy policy, etc.
            val verifierName = trustMetadata.displayName
            // Proceed with the trusted reader
        }
        requester.certChain != null -> {
            // Reader auth was provided but did not validate (untrusted CA or bad signature).
            // Handle as untrusted — typically warn the user before disclosing.
        }
        else -> {
            // Anonymous request — no reader auth attempted.
            // Treat as untrusted; some deployments may still allow disclosure here.
        }
    }
}
```

### Zero-Knowledge Proof Support

The library supports Zero-Knowledge Proof (ZKP) systems for selective disclosure. When configured,
the wallet can generate ZK proofs instead of disclosing raw document data, providing enhanced privacy.

To enable ZKP support, provide a `ZkSystemRepository` and optionally configure the `ZkResponsePolicy`:

```kotlin
val transferManager = TransferManager.getDefault(
    context = context,
    documentManager = documentManager,
    zkSystemRepository = zkSystemRepository,
    zkResponsePolicy = ZkResponsePolicy.Strict,
)
```

The `ZkResponsePolicy` determines behavior when ZK proof generation fails:

| Policy | Behavior |
|--------|----------|
| `ZkResponsePolicy.Strict` | Aborts disclosure for the document. **Recommended for production.** |
| `ZkResponsePolicy.FallbackToFullDisclosure` | Falls back to sending the full document. Current default for backwards compatibility. |

> **Note:** The default policy is `FallbackToFullDisclosure` for backwards compatibility. This will
> change to `Strict` in a future release. We recommend explicitly setting `ZkResponsePolicy.Strict`
> in production to prevent unintended full document disclosure when proof generation fails.

When a ZK proof is successfully generated, the credential is **not consumed** — the credential policy
(e.g., one-time-use limits) is not applied, since the actual credential key is never sent to the
verifier. Credential consumption only occurs when the full document is disclosed (regular path or
fallback).

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
