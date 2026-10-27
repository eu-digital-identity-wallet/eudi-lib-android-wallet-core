# Registration Certificate

The **EUDI Wallet Core** library validates the **registration certificate** of the entity the
wallet is interacting with, and surfaces the outcome to the application so the user can be informed
before proceeding. There are two such entities, and the library handles both:

- a **relying party**, when it requests attributes from the wallet (presentation),
- a **credential issuer**, when it offers to issue attestations to the wallet (issuance).

A registration certificate is a signed data object issued by a registration-certificate **provider**.
It describes the entity's registered identity and the scope it is registered to operate in.
Both sides share the same data model ([ETSI TS 119 475](https://www.etsi.org/standards)) and the same
validation in this library; they differ in how the certificate reaches the wallet, what its scope is
compared against, and how the outcome is read.

|  | Presentation | Issuance |
|---|---|---|
| Entity | Relying party (WRP) | Credential issuer (PID/EAA Provider) |
| Certificate | Wallet-Relying Party Registration Certificate (WRPRC) | The issuer's registration certificate |
| Specification | ETSI TS 119 472-2 | ETSI TS 119 472-3 |
| Protocol | ISO/IEC 18013-5, OpenID4VP, DC API | OpenID4VCI |
| Bound to | The access certificate (WRPAC) that signed the request | The certificate that signed the issuer metadata |
| Registered scope | `credentials` — what it may request | `provides_attestations` — what it may issue |
| Out-of-scope finding | Over-**asking** (claim level) | Over-**providing** (attestation level) |
| Policy | `WrpRegistrationPolicy` | `IssuerRegistrationPolicy` |
| Trust anchors | `configureWrpRegistrationTrust`, else `configureEtsiTrust` | `configureIssuerRegistrationTrust`, else `configureEtsiTrust` |
| Outcome read from | `ProcessedRequest.Success.wrpRegistration` | `Offer.issuerRegistration`, `OpenId4VciManager.resolveIssuerRegistration(…)` |

> **Note:** Registration-certificate handling is **enabled by default** on both paths. An enabled path
> needs trust for registration certificates — the issuance path when signed issuer metadata is
> required — otherwise building the wallet fails; see
> [Trust for registration certificates](#trust-for-registration-certificates).

The relying party's registration certificate is distinct from its **access certificate (WRPAC)** —
the X.509 e-seal that authenticates the request itself — and is bound to it. The same relationship
holds on the issuance path between the registration certificate and the certificate that signs the
issuer metadata.

---

## How the wallet validates a registration certificate

### Two stages

1. **Authentication** — parse the certificate, verify its signature, and check that the signer chain
   is trusted. A certificate that fails this stage does not carry an established registration: its
   content could be attacker-supplied, so it is **not shown to the user**.
2. **Evaluation** — take the authenticated registration and check it against the operation at hand.

Authentication is always performed by the library and cannot be replaced. Evaluation can be replaced
with a custom policy on either path.

### The validity checks

Both default evaluators run the same certificate-validity checks, in this order, before the
scope check:

1. **Binding** — the certificate is bound to the entity's access certificate (the organization
   identifier for a legal person, or the serial number for a natural person; matched against the
   intermediary when the certificate names one).
2. **Expiry** — the certificate has not expired.
3. **Status reference present** — a status-list reference is included (mandatory).
4. **Revocation** — the status list is checked; a check that cannot be completed is treated as a
   failure, so a certificate is never presented as validated without a confirmed status.

Each check derives from:

| Check | Failure reason | Source |
|---|---|---|
| Binding | `NOT_BOUND_TO_REQUESTER` | ETSI TS 119 475 §4.5 (the certificate is linked to the access certificate through the registered identifier), §5.1.1 GEN-5.1.1-03/-04 (linkage relies solely on the matching identifier; where the certificate carries several, at least one must correspond to the access certificate), GEN-5.2.4-02 (the `sub` claim matches the registered identifier), §5.1.2 / §5.1.4 (identifier mapping) |
| Expiry | `EXPIRED` | ETSI TS 119 475 §5.2.4 (`exp`) |
| Status reference present | `STATUS_MISSING` | ETSI TS 119 475 §6.2.6.2, REV-6.2.6.2-03 (status list mandatory) |
| Revocation | `REVOKED` / `REVOCATION_STATUS_UNKNOWN` | ETSI TS 119 475 §6.2.6.2 |

The checks that follow are path-specific. On the issuance path the issuer's **entitlement** is checked
next, and then over-providing; on the presentation path, over-asking.

| Check | Failure reason | Source |
|---|---|---|
| Entitlement (issuance only) | `ENTITLEMENT_MISSING` | ARF ISSU_24a (the PID Provider is registered as a PID Provider), ISSU_34a (the Attestation Provider is registered as a QEAA, PuB-EAA or EAA Provider); the entitlement URIs are those of ETSI TS 119 475, listed in TS5 |

### The outcome

Both paths produce a `RegistrationCertificateResult`:

- **`Verified(registration, overAskedClaims, overProvidedAttestations)`** — the certificate was
  authenticated and validated.
- **`Failed(reason, registration?)`** — validation did not pass; `reason` is a
  `RegistrationFailureReason`. `registration` carries the parsed content when it was read before the
  failing check (for example an expired or revoked certificate), so the user can still be shown who
  the entity is. It is null when the certificate could not be authenticated at all.
- **`requiresExplicitApproval`** — `true` when the result is `Failed` or when anything was found
  outside the registered scope. Per WRP-VALIDATION-02 / WRP-OVERASKING-02 the user must actively
  approve.

### Failure reasons

| Reason | Meaning | Stage |
|---|---|---|
| `CERTIFICATE_ABSENT` | No registration certificate was carried | Authentication |
| `MALFORMED` | The certificate could not be parsed | Authentication |
| `SIGNATURE_INVALID` | The signature could not be verified | Authentication |
| `UNTRUSTED_PROVIDER` | The signer chain is not trusted | Authentication |
| `NOT_BOUND_TO_REQUESTER` | The certificate is not bound to the entity's access certificate | Evaluation |
| `EXPIRED` | The certificate has expired | Evaluation |
| `STATUS_MISSING` | The certificate carries no status-list reference (mandatory) | Evaluation |
| `REVOKED` | The status list reports the certificate as revoked | Evaluation |
| `REVOCATION_STATUS_UNKNOWN` | The revocation status could not be determined | Evaluation |
| `ENTITLEMENT_MISSING` | The issuer is not registered for the provider role that issuing the offered attestations requires (issuance path only) | Evaluation |

---

## Trust for registration certificates

Validating a registration certificate checks two certificate chains against trust anchors:

- **Provider trust** — the signer chain of the certificate (the registration-certificate provider's
  e-seal).
- **Status-list trust** — the signer chain of the status-list token used for the revocation check.

For relying parties and for credential issuers separately, configured certificates take precedence
over the ETSI Trusted Lists:

1. **Configured certificates** — `configureWrpRegistrationTrust` for relying parties,
   `configureIssuerRegistrationTrust` for credential issuers. When they are set, the ETSI Trusted
   Lists are not used for that path.
2. **ETSI Trusted Lists** — `configureEtsiTrust`, when no certificates are configured: provider
   trust from `WalletRelyingPartyRegistrationCertificate` and status-list trust from
   `WalletRelyingPartyRegistrationCertificateStatus`. The same trusted lists serve both paths.

The reader authentication trust (`configureReaderAuthentication`) is not used for registration
certificates.

With a path's policy `Enabled` (the default) and neither source configured, building the wallet fails
with an `IllegalArgumentException` that names the missing configuration — on the issuance path when
the wallet requires signed issuer metadata, since only then is the certificate validated. Set the
path's policy to `Disabled` to opt out. When the ETSI Trusted Lists are the source and the
`wrprcProviders` list is not configured, the wallet builds and registration certificates fail with
`UNTRUSTED_PROVIDER`.

### `configureWrpRegistrationTrust` and `configureIssuerRegistrationTrust`

Both methods take the same parameters:

```kotlin
// relying parties (presentation)
fun configureWrpRegistrationTrust(
    trustedCertificates: List<X509Certificate>,
    trustedStatusListCertificates: List<X509Certificate> = trustedCertificates,
    revocationPolicy: RevocationPolicy = RevocationPolicy.HardFail
): EudiWalletConfig

// credential issuers (issuance)
fun configureIssuerRegistrationTrust(
    trustedCertificates: List<X509Certificate>,
    trustedStatusListCertificates: List<X509Certificate> = trustedCertificates,
    revocationPolicy: RevocationPolicy = RevocationPolicy.HardFail
): EudiWalletConfig
```

| Parameter | Meaning |
|---|---|
| `trustedCertificates` | The trust anchors for the certificate's signer chain: the CA certificates of the registration-certificate provider. |
| `trustedStatusListCertificates` | The trust anchors for the status-list token's signer chain. Defaults to `trustedCertificates`; set it when the status lists are signed under a different certificate authority, otherwise their revocation status cannot be confirmed and the result is `REVOCATION_STATUS_UNKNOWN`. |
| `revocationPolicy` | Revocation checking of both chains. `HardFail` (default): the validation fails when a certificate is revoked or its revocation status cannot be determined, for example when it names no CRL distribution point or its CRL cannot be retrieved. `SoftFail`: the validation fails when a certificate is revoked; a failure to retrieve the revocation information, for example an unreachable CRL, is tolerated. `NoCheck`: no revocation checking. |

Neither list may be empty (`IllegalArgumentException`). The chains are validated with PKIX against the
given certificates as trust anchors.

**Precedence.** Configured certificates take precedence over the ETSI Trusted Lists for that path:
once set, the path's registration certificates and their status lists are validated against them
alone. The ETSI Trusted Lists still serve the other registration path unless it is configured too,
and the other trust areas that take their trust from `configureEtsiTrust`.

**Custom evaluators.** With `configureWrpRegistrationEvaluator` or
`configureIssuerRegistrationEvaluator`, the configured `trustedCertificates` still authenticate the
certificate before the evaluator runs. The revocation check is then the custom evaluator's own, so
`trustedStatusListCertificates` applies only to the built-in evaluators.

**Issuance.** The issuer's registration certificate is carried in the signed issuer metadata, so it is
validated only when signed metadata is required; otherwise it is skipped
(see [Issuance](#issuance--the-credential-issuers-registration-certificate)).

### Examples

**A complete configuration without the ETSI Trusted Lists.** Access certificates and relying-party
registration certificates have their own trust. Without `configureIssuerTrust` no signed issuer
metadata is required, so issuer registration certificates are not validated and need no trust; with
it, see *The same registrar for both paths* below:

```kotlin
val config = EudiWalletConfig()
    .configureReaderAuthentication {
        trustedCertificates(accessCertificateCa)           // access certificates (WRPAC)
    }
    .configureWrpRegistrationTrust(listOf(registrarCa))    // relying-party registration certificates
// ... rest of the configuration, without configureIssuerTrust
```

The examples that start from `config.` show only the registration-certificate trust; `config` is
configured as above.

**Status lists signed under a different certificate authority:**

```kotlin
config.configureWrpRegistrationTrust(
    trustedCertificates = listOf(registrarCa),
    trustedStatusListCertificates = listOf(statusListCa),
)
```

**The same registrar for both paths,** with signed issuer metadata so that issuer registration
certificates are validated:

```kotlin
val config = EudiWalletConfig()
    .configureReaderAuthentication {
        trustedCertificates(accessCertificateCa)
    }
    .configureIssuerTrust { /* … */ }                       // signed issuer metadata, required by default
    .configureWrpRegistrationTrust(listOf(registrarCa))
    .configureIssuerRegistrationTrust(listOf(registrarCa))
```

**Selected registrars, with the ETSI Trusted Lists for everything else:**

```kotlin
val config = EudiWalletConfig()
    .configureEtsiTrust { /* … */ }
    .configureIssuerTrust { /* … */ }
    .configureWrpRegistrationTrust(listOf(registrarCa))
// relying-party registration certificates: registrarCa
// issuer registration certificates, reader authentication, issuer trust: the ETSI Trusted Lists
```

**Revocation information that cannot be retrieved is tolerated:**

```kotlin
config.configureWrpRegistrationTrust(
    trustedCertificates = listOf(registrarCa),
    revocationPolicy = RevocationPolicy.SoftFail,
)
```

**Certificates bundled as raw resources (Android):**

```kotlin
fun Context.certificate(@RawRes id: Int): X509Certificate =
    resources.openRawResource(id).use {
        CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
    }

config.configureWrpRegistrationTrust(listOf(context.certificate(R.raw.registrar_ca)))
```

**Java:**

```java
config.configureWrpRegistrationTrust(registrarCas);
config.configureWrpRegistrationTrust(registrarCas, statusListCas);
config.configureWrpRegistrationTrust(registrarCas, registrarCas, RevocationPolicy.SoftFail.INSTANCE);
```

---

## Presentation — the relying party's registration certificate

### How it reaches the wallet

The certificate is carried differently per channel, but all channels hand it to the same validation,
so the checks and the returned result are the same regardless of channel.

| Channel | Carrier |
|---|---|
| Proximity (ISO/IEC 18013-5) and DC API (ISO mdoc) | `euWrprc` byte string in each `ItemsRequest` `requestInfo` (ETSI TS 119 472-2 §5.3.2) |
| Remote (OpenID4VP) and DC API (OpenID4VP) | `verifier_info` element with `format = "registration_cert"` (ETSI TS 119 472-2 §6.3.2.2) |

### Enabling and disabling

Handling is controlled by `WrpRegistrationPolicy`, `Enabled` by default:

```kotlin
val config = EudiWalletConfig()
    // Enabled by default; call this only to turn handling off:
    .configureWrpRegistrationPolicy(WrpRegistrationPolicy.Disabled)
// ... rest of your configuration

val eudiWallet = EudiWallet(context, config)
```

- `WrpRegistrationPolicy.Enabled` (default) — the certificate is validated and its outcome is
  surfaced to the user. On the OpenID4VP path, the OpenID4VP library also applies its own checks to
  the registration certificate and may reject the request.
- `WrpRegistrationPolicy.Disabled` — the certificate is neither validated nor surfaced.

### Trust anchors

Provider trust and status-list trust come from `configureWrpRegistrationTrust`, else from the ETSI
Trusted Lists; see [Trust for registration certificates](#trust-for-registration-certificates).

**ETSI Trusted Lists.** The intended production source. The relying-party registration-certificate
provider list must be configured. Reader authentication takes its trust (the access-certificate
provider list) from the same `configureEtsiTrust` configuration when `configureReaderAuthentication`
sets no trust source of its own.

```kotlin
val config = EudiWalletConfig()
    .configureEtsiTrust {
        loteLocations(SupportedLists(
            // … pidProviders / wrpacProviders …
            wrprcProviders = Uri("https://trustedlist.../WRPRCProviders.jwt"), // required for WRPRC validation
        ))
    }
    .configureReaderAuthentication {
        enforceIfPresent()
    }
```

> **Note:** Binding needs the request to be reader-authenticated: the access certificate is taken from
> the proximity reader auth or the signed remote request. A request with no reader authentication has
> no requester identity, so binding fails (`NOT_BOUND_TO_REQUESTER`).

### Custom evaluation (advanced)

The default evaluator (`DefaultWrpRegistrationEvaluator`) runs the shared validity checks and then
compares the requested attributes against the registered `credentials` scope, reporting anything
outside it as `overAskedClaims` (WRP-OVERASKING-01/02; CIR (EU) 2025/848 Article 8(2); ETSI
TS 119 475 §5.2.4 `credentials`, GEN-5.2.4-06).

You can replace the evaluation stage with your own policy, shared across all channels, via
`configureWrpRegistrationEvaluator`:

```kotlin
val config = EudiWalletConfig()
    .configureWrpRegistrationEvaluator(myEvaluator)
```

A `WrpRegistrationEvaluator` receives an **already authenticated** registration, so a custom
evaluator governs the identity and scope policy only — it cannot weaken the signature or trust
checks:

```kotlin
fun interface WrpRegistrationEvaluator {
    suspend fun evaluate(
        registration: RegistrationCertificate,                  // already parsed, signed and trusted
        accessCertificate: X509Certificate?,                    // the requester's access certificate, when present
        requestedAttestations: List<RequestedAttestationInfo>,  // what the request asks for
    ): RegistrationCertificateResult                            // Verified(…, overAskedClaims) or Failed(reason, …)
}
```

### Reading the outcome

The result is exposed uniformly on the processed request via `Success.wrpRegistration`, typed as
`WrpRegistrationInfo` and produced as a `RegistrationCertificateResult`:

```kotlin
eudiWallet.addTransferEventListener { event ->
    if (event is TransferEvent.RequestReceived) {
        when (val processed = event.processedRequest) {
            is RequestProcessor.ProcessedRequest.Failure -> {
                // The request could not be processed.
                val error = processed.error
            }

            is RequestProcessor.ProcessedRequest.Success -> {
                when (val result = processed.wrpRegistration as? RegistrationCertificateResult) {
                    is RegistrationCertificateResult.Verified -> {
                        // result.registration — who the relying party is (name, country, purpose, …)
                        // result.overAskedClaims — attributes requested outside the registered scope
                    }
                    is RegistrationCertificateResult.Failed -> {
                        // result.reason — why validation failed (authentication or evaluation)
                        // result.registration — the parsed registration, when read before the failure
                    }
                    // Not handled: the policy is Disabled — nothing is validated or surfaced.
                    null -> Unit
                }

                // Convenience gate: true when the user must explicitly approve before sharing
                // (validation failed, or the request is over-asking).
                val needsApproval = (processed.wrpRegistration as? RegistrationCertificateResult)
                    ?.requiresExplicitApproval == true
            }
        }
    }
}
```

### Validity and over-asking are two separate results

A request raises two independent questions, and you should check **both**:

1. **Is the certificate valid?** — `Verified` vs `Failed`.
2. **Does the request stay within the registered scope?** — `overAskedClaims`.

A `Verified` result does **not** mean the request is within scope: a fully valid certificate can
still be **over-asking**. Testing only for `Verified` is therefore not enough — inspect
`overAskedClaims` on it as well.

```kotlin
when (val result = success.wrpRegistration as? RegistrationCertificateResult) {
    is RegistrationCertificateResult.Verified ->
        if (result.overAskedClaims.isEmpty()) {
            // valid AND within scope — nothing to warn about
        } else {
            // valid, but the request is over-asking — warn and require approval
        }
    is RegistrationCertificateResult.Failed ->
        // not valid — warn (result.reason) and require approval
        Unit
    null -> Unit
}
```

Over-asking is reported only on a `Verified` result: when validation fails the declared scope cannot
be relied on, so no over-asked claims are computed on a `Failed` result. When you only need a single
yes/no for the consent UI, `requiresExplicitApproval` folds both axes into one gate.

---

## Issuance — the credential issuer's registration certificate

### How it reaches the wallet

The certificate is carried in the **signed issuer metadata**, as an `issuer_info` attestation with
`format = "registration_cert"` (ETSI TS 119 472-3 clause 4.2.3). Because it is part of the signed
metadata, it is available as soon as the issuer metadata is resolved — before authorization and before
any credential is requested.

Unlike `verifier_info` on the presentation path, `issuer_info` has no basis in the issuance protocol
itself: OpenID4VCI defines no equivalent metadata parameter, and no generic extension point for one.
It is added to the issuer metadata entirely by ETSI TS 119 472-3.

**Signed metadata is required.** The certificate is validated only when the wallet requires signed
issuer metadata (`configureIssuerTrust { requireSignedMetadata() }`); otherwise it is skipped.

**When the issuance stops.** With the policy `Enabled` and signed metadata required, the OpenID4VCI
library stops the issuance, with `IssueEvent.Failure`, when the issuer metadata does not carry exactly
one registration certificate in string form. Before the issuance, the offer and
`resolveIssuerRegistration(…)` report this as `Failed(CERTIFICATE_ABSENT)` or `Failed(MALFORMED)`, so
the application can inform the user. Any other evaluation outcome is reported without stopping the
issuance, and the application decides whether to proceed; for `ENTITLEMENT_MISSING`, see
[The entitlement check](#the-entitlement-check).

### Enabling and disabling

Handling is controlled by `IssuerRegistrationPolicy`, `Enabled` by default, together with the signed
metadata requirement:

```kotlin
val config = EudiWalletConfig()
    .configureIssuerTrust {
        // requireSignedMetadata() is the default inside this block, but the block itself
        // must be present for the registration certificate to be validated.
    }
    // Enabled by default; call this only to turn handling off:
    .configureIssuerRegistrationPolicy(IssuerRegistrationPolicy.Disabled)
```

- `IssuerRegistrationPolicy.Enabled` (default) — the certificate is validated and its outcome is
  surfaced to the user, including the absence of a certificate.
- `IssuerRegistrationPolicy.Disabled` — the certificate is neither validated nor surfaced.

### Trust anchors

Provider trust and status-list trust come from `configureIssuerRegistrationTrust`, else from the ETSI
Trusted Lists, the same lists as on the presentation path; see
[Trust for registration certificates](#trust-for-registration-certificates). Validation also needs
signed metadata, as described under *Enabling and disabling* above.

### Custom evaluation (advanced)

The default evaluator (`DefaultIssuerRegistrationEvaluator`) runs the same validity checks as the
presentation path — binding, expiry, status reference and revocation, with binding taken against the
certificate that signed the issuer metadata. It then checks the issuer's `entitlements` against what it
offers to issue, failing with `ENTITLEMENT_MISSING` when the provider role is not confirmed, and
finally compares the offered credential configurations against the registered `provides_attestations`
scope, reporting anything outside it as `overProvidedAttestations`.

Replacing the evaluator replaces the entitlement check too. A custom evaluator that does not implement
it does not satisfy ISSU_24a / ISSU_34a. An exception thrown by a custom evaluator during issuance stops
the issuance.

You can replace the evaluation stage via `configureIssuerRegistrationEvaluator`:

```kotlin
val config = EudiWalletConfig()
    .configureIssuerRegistrationEvaluator(myEvaluator)
```

As on the presentation path, the evaluator receives an **already authenticated** registration:

```kotlin
fun interface IssuerRegistrationEvaluator {
    suspend fun evaluate(
        registration: RegistrationCertificate,           // already parsed, signed and trusted
        accessCertificate: X509Certificate?,             // the issuer metadata signing certificate
        offeredAttestations: List<OfferedAttestation>,   // what the issuer offers to issue
    ): RegistrationCertificateResult                     // Verified(…, overProvidedAttestations) or Failed(reason, …)
}
```

### Reading the outcome

The outcome is available at two points, so it can be shown before the user commits to the issuance
and again while it runs.

**On the resolved offer** — `Offer.issuerRegistration`, for the offer screen:

```kotlin
val openId4VciManager = wallet.createOpenId4VciManager()
openId4VciManager.resolveDocumentOffer(offerUri) { result ->
    if (result is OfferResult.Success) {
        when (val registration = result.offer.issuerRegistration) {
            is RegistrationCertificateResult.Verified -> {
                // registration.registration — who the issuer is (name, country, …)
                // registration.overProvidedAttestations — attestations offered outside the registered scope
            }
            is RegistrationCertificateResult.Failed -> {
                // registration.reason — why validation failed, CERTIFICATE_ABSENT when the
                // issuer offered no registration certificate
            }
            // Not handled: the policy is Disabled, signed metadata is not required, or the
            // evaluation could not be performed.
            null -> Unit
        }
    }
}
```

**Before issuance, on demand** — `OpenId4VciManager.resolveIssuerRegistration(…)`, for flows that do
not begin from a resolved offer: issuing by configuration identifier, or before a re-issuance. It
resolves the issuer metadata and returns the verdict up front, so the application can decide whether to
proceed before any credential is requested:

```kotlin
// wallet-initiated issuance, by issuer URL and offered configuration identifiers:
val outcome = openId4VciManager.resolveIssuerRegistration(issuerUrl, credentialConfigurationIds)
// or, before re-issuing an existing document:
val outcome = openId4VciManager.resolveIssuerRegistration(documentId)

when (val registration = outcome.getOrNull()) {
    is RegistrationCertificateResult.Failed -> {
        // the issuer could not be verified — registration.reason says why
        // (CERTIFICATE_ABSENT, UNTRUSTED_PROVIDER, EXPIRED, REVOKED, MALFORMED, …); inform the user
        // and refuse if you choose to
        val reason = registration.reason
    }
    is RegistrationCertificateResult.Verified ->
        if (registration.overProvidedAttestations.isEmpty()) {
            // verified AND within the registered scope — nothing to warn about
        } else {
            // verified, but the issuer offers attestations beyond its registered scope (over-providing)
            // registration.overProvidedAttestations — the attestation types outside the registered scope
        }
    // null (a failed Result): the policy is Disabled, signed metadata is not required, or the check
    // could not be performed
    null -> Unit
}
```

### Validity and over-providing are two separate results

The same two-axis reading as on the presentation path applies, with `overProvidedAttestations` in
place of `overAskedClaims`: a `Verified` issuer registration can still be over-providing, and
`requiresExplicitApproval` folds both axes into one gate.

### The entitlement check

Before the scope check, the issuer's `entitlements` are checked against what it offers to issue. Each
offered attestation requires the provider role that issuing it calls for:

| Offered attestation | Required entitlement |
|---|---|
| A PID | `PID_Provider` |
| Anything else | any one of `QEAA_Provider`, `PUB_EAA_Provider`, `Non_Q_EAA_Provider` |

The non-PID case is a disjunction because ARF ISSU_34a requires only that the provider be registered
as a QEAA, PuB-EAA **or** EAA Provider. The entitlement is what the registrar asserted, whereas the
wallet's own classification of an attestation is local configuration — so requiring the two to agree
would refuse providers legitimately registered under a different one of the three.

Which offered attestations are PIDs is decided by the `pids` classification already configured for
trust-area routing — that of `configureIssuerTrust { classifications(...) }`, else that of
`configureEtsiTrust { classifications(...) }` — so this check and the trust evaluation cannot
disagree. An attestation identifying no type, and every attestation when no `pids`
classification is configured, counts as non-PID.

A shortfall is a validation failure, `Failed(ENTITLEMENT_MISSING, registration)` — not a finding on a
verified result. Like the other outcomes of the evaluation, it is reported without stopping the
issuance. ARF ISSU_24a / ISSU_34a require the wallet to warn the user and not to request the issuance
in this case, so on this outcome the application should not proceed with the issuance. The parsed
`registration` is still carried, so the provider can be named when the issuance is refused.

An absent or empty `entitlements` claim satisfies nothing: a certificate that does not confirm the
role cannot be treated as confirming it.