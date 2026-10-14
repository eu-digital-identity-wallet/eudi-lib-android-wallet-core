# Static Certificate Trust Configuration

The wallet-core is trust-source-agnostic, it never downloads trust anchors itself. The
LoTE (List of Trusted Entities) pipeline shown in the main README is one option, not a
requirement. Trust anchors can be provided from **static certificate files**, a
**keystore**, or any other source, with no `HttpClient` and no LoTE URL involved.

This document shows how to configure all trust areas using the ETSI consultation
library's composable API with locally-provided certificates.

## How it works

The ETSI library (`etsi-1196x2-consultation`) is built around composable building blocks:

| Building block | Role |
|---|---|
| `GetTrustAnchors` | Functional interface that returns trust anchors for a given query. Can be a lambda returning a static list. |
| `.validator()` | Extension that combines a `GetTrustAnchors` with a set of `VerificationContext`s and a chain validator into an `IsChainTrustedForContext`. |
| `ComposeChainTrust` | Aggregates multiple `IsChainTrustedForContext` instances (via the `+` operator or `ComposeChainTrust.of()`). Implements `IsChainTrustedForEUDIW`.|


The wallet-core accepts `ComposeChainTrust` / `IsChainTrustedForEUDIW` in all trust area
builders — the same type regardless of whether the anchors came from LoTE, files, or a
keystore.

## Trust areas and verification contexts

Each trust area uses specific `VerificationContext` values. Your trust source must cover
the contexts needed by the areas you want to enable:

| Trust area | Config entry point | Verification contexts |
|---|---|---|
| Credential issuance | `configureIssuerTrust { trustSource(...) }` | `PID`, `PubEAA`, `QEAA`, `EAA(useCase)` |
| Status list tokens | `configureDocumentStatusResolver { configureTrust { trustSource(...) } }` | `PIDStatus`, `PubEAAStatus`, `QEAAStatus`, `EAAStatus(useCase)` |
| Reader/verifier auth | `configureReaderAuthentication { trustSource(...) }` | `WalletRelyingPartyAccessCertificate` |
| Signed issuer metadata | Via `configureIssuerTrust { requireSignedMetadata() }` | `WalletRelyingPartyAccessCertificate` |

## Example: static certificates

### Load your certificates

```kotlin
// Certificates of trusted PID issuers
val pidIssuerCerts: List<X509Certificate> = loadCertificates("pid-issuer-ca.pem")

// Certificates of trusted verifiers / relying parties (WRPAC)
val wrpacCerts: List<X509Certificate> = loadCertificates("verifier-ca.pem")
```

### Choose a certificate chain validator

```kotlin
val pkixValidator = ValidateCertificateChainUsingPKIXJvm.Default
```

### Create per-context trust validators from static certificates

```kotlin
val pidTrustSource = GetTrustAnchors<VerificationContext, TrustAnchor> { _ ->
    NonEmptyList(pidIssuerCerts.map { TrustAnchor(it, null) })
}

val pidValidator = pidTrustSource.validator(
    supportedQueries = setOf(
        VerificationContext.PID,        // credential issuance trust
        VerificationContext.PIDStatus,  // status list token trust
    ),
    validateCertificateChain = pkixValidator
)

val wrpacTrustSource = GetTrustAnchors<VerificationContext, TrustAnchor> { _ ->
    NonEmptyList(wrpacCerts.map { TrustAnchor(it, null) })
}

val wrpacValidator: IsChainTrustedForContext<List<X509Certificate>, VerificationContext, TrustAnchor> =
    wrpacTrustSource.validator(
        supportedQueries = setOf(
            VerificationContext.WalletRelyingPartyAccessCertificate
        ),
        validateCertificateChain = pkixValidator
    )
```

### Compose into a single trust source

```kotlin
val composedTrust: ComposeChainTrust<List<X509Certificate>, VerificationContext, TrustAnchor> =
    ComposeChainTrust.of(pidValidator, wrpacValidator)
```

### Define attestation classifications

```kotlin
val classifications = AttestationClassifications(
    pids = AttestationIdentifierPredicate.any(
        identifiers = setOf(
            AttestationIdentifier.MDoc(
                docType = DocumentIdentifier.MdocPid.formatType
            ),
            AttestationIdentifier.SDJwtVc(
                vct = DocumentIdentifier.SdJwtPid.formatType
            ),
        )
    )
)
```

### Configure the wallet

```kotlin
config = EudiWalletConfig {
    // ...

    configureIssuerTrust {
        trustSource(composedTrust)
        classifications(classifications)
    }

    configureDocumentStatusResolver {
        configureTrust {
            trustSource(composedTrust)
            classifications(classifications)
        }
    }

    configureReaderAuthentication {
        trustSource(composedTrust)
        enforceIfPresent()
        revocationPolicy(RevocationPolicy.HardFail)
    }
}
```

## Example: keystore-backed trust source

The ETSI library ships `IsChainTrustedForContext.usingKeyStore()` which reads trust
anchors directly from a Java `KeyStore`. This is a single call that replaces the manual
`GetTrustAnchors` lambda:

  ```kotlin
  val keystore = KeyStore.getInstance("PKCS12").apply {
      File("trust.p12").inputStream().use { load(it, password) }
  }

  val trustSource = IsChainTrustedForContext.usingKeyStore(
      keystore = keystore,
      supportedVerificationContexts = setOf(
          VerificationContext.PID,
          VerificationContext.PIDStatus,
          VerificationContext.WalletRelyingPartyAccessCertificate,
      ),
      validateCertificateChain = ValidateCertificateChainUsingPKIXJvm(
          customization = { isRevocationEnabled = false },
      ),
      regexPerVerificationContext = { Regex(".*") }, // which aliases apply to each context
  )
  ```

The `regexPerVerificationContext` parameter controls which keystore aliases are used for
each context. Use `Regex(".*")` to include all aliases, or provide context-specific
patterns (e.g., `Regex("pid-.*")` for PID anchors) when the keystore contains certificates
for multiple contexts under different alias naming conventions.

The resulting `trustSource` can be wrapped with `ComposeChainTrust(trustSource)` and
passed to the wallet configuration in the same way as the static certificate example
