# Digital Credential API

The **EUDI Wallet Core** library includes support for the [Digital Credential API](https://w3c-fedid.github.io/digital-credentials/).

DCAPI supports two protocol families:

- **`org-iso-mdoc`** — ISO mdoc presentation, according to the [ISO/IEC TS 18013-7:2025](https://www.iso.org/standard/91154.html) **Annex C** (MSO mdoc documents).
- **[OpenID4VP 1.0 over the Digital Credential API](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#appendix-A)** — `openid4vp-v1-signed` and `openid4vp-v1-unsigned` (MSO mdoc and SD-JWT VC documents).

You choose which protocols the wallet accepts through the configuration described below.

> **Note:** DCAPI is **disabled by default**. You can enable it in your application by following the steps below.

## Enabling the Digital Credential API

### Add the runtime dependency

To enable DCAPI, add the following dependency to your app:

```groovy
dependencies {
    // Required only if DCAPI is enabled
    implementation "androidx.credentials.registry:registry-provider-play-services:1.0.0-alpha04"
}
```

Without this dependency, DCAPI registration fails at runtime
with the error `"no provider dependencies found - please ensure the desired provider dependencies are added"`.

#### Why you need to add this dependency

DCAPI registration relies on the AndroidX Credentials Registry, and the
`registry-provider-play-services` artifact is currently the only runtime
provider available — without it, DCAPI cannot function.

This artifact pulls in Google Play Services (GMS), which is not available
on every Android device.

For this reason `eudi-lib-android-wallet-core` does **not** bundle the
provider as a transitive dependency. Each consuming wallet is free to
decide whether to support DCAPI based on its target distribution:

- **GMS-enabled distributions** (e.g., apps shipped via Google Play): add
  the dependency to enable DCAPI alongside all other wallet-core features.
- **Non-GMS distributions** (e.g., **GrapheneOS**, AOSP-based government
  builds): omit the dependency. DCAPI will not function,
  but every other wallet-core capability — OpenID4VCI issuance, OpenID4VP
  remote presentation, BLE/NFC proximity presentation —
  continues to work normally.

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

Configure and initialize the `EudiWallet` with DCAPI enabled. At least one supported protocol must
be declared via `withSupportedProtocols(...)`:

```kotlin
val config = EudiWalletConfig()
    .configureDCAPI {
        withEnabled(true) // Enable DCAPI, by default it is disabled
        withSupportedProtocols(DCAPIProtocol.ISO_MDOC) // at least one protocol is required
    }
// ... Rest of your configurations


// Initialize the EudiWallet with the configuration
val eudiWallet = EudiWallet(context, config)
```

#### Supported protocols

The DCAPI protocols the wallet processes are declared with `withSupportedProtocols(...)`, using the
`DCAPIProtocol` enum:

- `DCAPIProtocol.ISO_MDOC` — ISO mdoc (`org-iso-mdoc`).
- `DCAPIProtocol.OPENID4VP_V1_SIGNED` — OpenID4VP 1.0, signed request.
- `DCAPIProtocol.OPENID4VP_V1_UNSIGNED` — OpenID4VP 1.0, unsigned request.

> **Note:** Declaring at least one protocol is mandatory when DCAPI is enabled. Enabling DCAPI
> without `withSupportedProtocols(...)` fails at configuration time. A request for a protocol that
> is not declared here is rejected.

Enabling an OpenID4VP protocol additionally requires the wallet's OpenID4VP configuration to be set
via `configureOpenId4Vp { ... }` (see [Initialize the library](README.md#initialize-the-library));
otherwise configuration fails. OpenID4VP over the DCAPI reuses that configuration.

Document formats follow the enabled protocols:

- **MSO mdoc** documents are presented over `org-iso-mdoc` and OpenID4VP.
- **SD-JWT VC** documents are presented over OpenID4VP only, so they are offered to verifiers only
  when an OpenID4VP protocol is enabled.

Example enabling both ISO mdoc and OpenID4VP:

```kotlin
val config = EudiWalletConfig()
    .configureOpenId4Vp {
        // OpenID4VP configuration — required when an OpenID4VP protocol is enabled
    }
    .configureDCAPI {
        withEnabled(true)
        withSupportedProtocols(
            DCAPIProtocol.ISO_MDOC,
            DCAPIProtocol.OPENID4VP_V1_SIGNED,
            DCAPIProtocol.OPENID4VP_V1_UNSIGNED,
        )
    }
```

In the `DCAPIConfig` you can also set up your own allowlist of privileged user agents
(typically web browsers) that the wallet trusts to deliver a website's origin on its
behalf. This allowlist is **not** for native verifier apps — those are handled
separately via the [App origin](#app-origin-native-android-verifiers) mechanism
described below.

The allowlist is provided as a JSON file in the following format:

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
        withSupportedProtocols(DCAPIProtocol.ISO_MDOC) // at least one protocol is required
        withPrivilegedAllowlist(customAllowListJson) // Override the bundled default allowlist
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

### Verifier Origin Handling

When the wallet receives a DCAPI request, it must determine the **origin** of the verifier and bind
it into the session transcript: for `org-iso-mdoc` according to ISO/IEC TS 18013-7:2025 **Annex C**,
and for OpenID4VP according to [OpenID4VP 1.0, §B.2.6](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#appendix-B.2.6), where the session transcript applies to **mdoc** documents only.

#### Web origin (privileged user agents)

When the request is delivered through a privileged user agent — typically a trusted
web browser acting on behalf of a website — the user agent provides the website's
origin via the Android Credential Manager API. The wallet retrieves it through
`CallingAppInfo.getOrigin(privilegedAllowlist)`, which returns the web origin
(e.g. `https://verifier.example.com`) only when the caller's package name and
signing certificate match an entry in the configured allowlist.

To register additional privileged user agents, provide a custom allowlist JSON via
`DCAPIConfig.withPrivilegedAllowlist(...)` as shown above.

#### App origin (native Android verifiers)

For native Android verifier apps, the wallet derives the origin from the calling app's
signing certificate, `CallingAppInfo.signingInfoCompat.signingCertificateHistory[0]`, in
the following form:

```
android:apk-key-hash:<encoded SHA 256 fingerprint>
```

See for more details [here](https://developer.android.com/identity/digital-credentials/credential-holder/credential-holder#check-verifier-origin).

**Note:** For interoperability, a native Android verifier app must use the same origin value in its
session transcript.

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
            // get the processed request — Success carries presentmentData / requester /
            // trustMetadata; Failure carries the error
            val success = event.processedRequest.getOrThrow()
                as RequestProcessor.ProcessedRequest.Success

            // `success.presentmentSelections` contains the options the consent UI can
            // render. In the DCAPI flow the underlying tree is already narrowed by the
            // OS picker, so there's a single option that gathers every match the wallet
            // should disclose.
            val selection = success.presentmentSelections.single()
            val matches = selection.matches

            // Per-credential unlock data, keyed by `match.credential.identifier`.
            val keyUnlockData: Map<String, KeyUnlockData> = matches.associate { match ->
                match.credential.identifier to
                    wallet.getDefaultKeyUnlockData(match.credential.identifier)
            }

            // generate the response
            val response = success.generateResponse(
                selection = selection,
                keyUnlockData = keyUnlockData,
            ).getOrThrow()

            wallet.sendResponse(response)

        } catch (e: Throwable) {
            // An error occurred — handle the error
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
The processed request can be accessed through `event.processedRequest`. On success, the
`RequestProcessor.ProcessedRequest.Success` carries `presentmentSelections` (the options the
consent UI renders), `presentmentData` (the underlying tree), `requester`, and `trustMetadata`.
The application picks a `CredentialPresentmentSelection` from `presentmentSelections` and
calls `success.generateResponse(...)` to produce the response. See more details in
[README](README.md#receiving-a-request-and-sending-a-response).

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

## Limitations

Some features are not yet supported over the Digital Credential API. These are tied to the
version of the credential matcher currently bundled with the library, and are expected to be
supported as the matcher is updated:

- **DCQL `multiple` matching** — a credential query with `"multiple": true` (returning more than
  one matching credential for a single query) is not supported; one credential per query is offered.
- **Array claim paths** — selecting individual array elements, by index (`["nationalities", 0]`) or
  with the `null` wildcard (`["nationalities", null]`), is not supported. Requesting the whole claim
  (`["nationalities"]`) works.
- **Request protocol order** — when a verifier offers several protocols as alternatives in a single
  request, the OS credential matcher commits to the first protocol it recognizes and does not fall
  back to a later alternative, even when a document does not support that first protocol. A document
  may therefore not be offered if a recognized-but-unsupported protocol is listed before one it does
  support. For example, a verifier may list the draft `openid4vp` protocol (OpenID4VP Draft 24)
  before `org-iso-mdoc`. An MSO mdoc document is offered over `org-iso-mdoc` but not over the draft
  `openid4vp`, so the matcher commits to `openid4vp`, finds no match, and never falls back to
  `org-iso-mdoc` — the document is not offered. Listing `org-iso-mdoc` first, or using the OpenID4VP
  1.0 protocols `openid4vp-v1-signed` / `openid4vp-v1-unsigned` (which the document does support),
  avoids this.

In addition, **`openid4vp-v1-multisigned`** (multi-signed OpenID4VP requests) is not supported yet.