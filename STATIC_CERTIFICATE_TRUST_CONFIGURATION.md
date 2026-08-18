# Static Certificate Trust Configuration

Example of configuring the EUDI Wallet Core with **static certificates** (without LoTE)
for all trust areas, using the ETSI consultation library's composable API.

## Load your certificates

```kotlin
// Certificates of trusted PID issuers
val pidIssuerCerts: List<X509Certificate> = loadCertificates("pid-issuer-ca.pem")

// Certificates of trusted verifiers / relying parties (WRPAC)
val wrpacCerts: List<X509Certificate> = loadCertificates("verifier-ca.pem")
```

## Choose a certificate chain validator

```kotlin
val pkixValidator = ValidateCertificateChainUsingPKIXJvm.Default
```

## Create per-context trust validators from static certificates

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

## Compose into a single trust source

```kotlin
val composedTrust: ComposeChainTrust<List<X509Certificate>, VerificationContext, TrustAnchor> =
    ComposeChainTrust.of(pidValidator, wrpacValidator)
```

## Define attestation classifications

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

## Configure the wallet

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

    configureReaderTrustStore(composedTrust)
}
```
