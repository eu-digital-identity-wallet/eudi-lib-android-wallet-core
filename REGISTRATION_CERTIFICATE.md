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
| Outcome read from | `ProcessedRequest.Success.wrpRegistration` | `Offer.issuerRegistration`, `IssueEvent.IssuerRegistrationChecked` |

> **Note:** Registration-certificate handling is **enabled by default** on both paths.

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

The scope check that follows is path-specific: over-asking on the presentation path, over-providing
on the issuance path.

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
| `CERTIFICATE_ABSENT` | No registration certificate was carried (presentation path; on the issuance path an absent certificate yields `null` instead) | Authentication |
| `MALFORMED` | The certificate could not be parsed | Authentication |
| `SIGNATURE_INVALID` | The signature could not be verified | Authentication |
| `UNTRUSTED_PROVIDER` | The signer chain is not trusted | Authentication |
| `NOT_BOUND_TO_REQUESTER` | The certificate is not bound to the entity's access certificate | Evaluation |
| `EXPIRED` | The certificate has expired | Evaluation |
| `STATUS_MISSING` | The certificate carries no status-list reference (mandatory) | Evaluation |
| `REVOKED` | The status list reports the certificate as revoked | Evaluation |
| `REVOCATION_STATUS_UNKNOWN` | The revocation status could not be determined | Evaluation |

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
  surfaced to the user.
- `WrpRegistrationPolicy.Disabled` — the certificate is neither validated nor surfaced.

### Trust anchors

Two trust stores are needed, both taken from the wallet's reader trust configuration
(`configureReaderTrustStore`):

- **Provider trust** — validates the certificate's signer chain (the registration-certificate
  provider's e-seal).
- **Status-list trust** — validates the status-list token used for the revocation check.

**Primary — ETSI Trusted Lists.** The intended production source. Enable ETSI reader trust
(`configureReaderTrustStore { }` together with `configureEtsiTrust`); both stores are then taken from
the trusted lists — provider trust from `WalletRelyingPartyRegistrationCertificate` and status-list
trust from `WalletRelyingPartyRegistrationCertificateStatus`.

```kotlin
val config = EudiWalletConfig()
    .configureEtsiTrust {
        loteLocations(SupportedLists(
            // … pidProviders / wrpacProviders …
            wrprcProviders = Uri("https://trustedlist.../WRPRCProviders.jwt"), // required for WRPRC validation
        ))
    }
    .configureReaderTrustStore { /* enable ETSI-based reader/registration trust (needs configureEtsiTrust) */ }
```

**Override — a static or custom store.** Alternatively, provide your own reader trust store
(`configureReaderTrustStore(store)` / `configureReaderTrustStore(certificates)`); it takes precedence
over the ETSI lists. Provider trust then uses that store, and status-list trust is unset — only the
status-list token's own signature is checked, without establishing that its signer is a trusted
status-list provider.

> **Note:** Binding needs the request to be reader-authenticated: the access certificate is taken from
> the proximity reader auth or the signed remote request. A request with no reader authentication has
> no requester identity, so binding fails (`NOT_BOUND_TO_REQUESTER`).

> **Note:** Provider trust is what activates the handling. With the policy `Enabled` but none of the
> three sources above configured, there is no trust to authenticate a certificate against, so nothing
> is validated and nothing is surfaced — `wrpRegistration` is `null` and no request is rejected. Set
> up reader trust to switch the handling on.

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
                    // Not handled: the policy is Disabled, or no reader trust is configured
                    // (see Trust anchors) — in which case nothing is validated or surfaced.
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

**Signed metadata is required.** Validation runs only when the wallet is configured to verify signed
issuer metadata; otherwise it is silently skipped and logged.

**An absent certificate is not a failure to the wallet.** Metadata that carries no registration
certificate yields `null` rather than `Failed(CERTIFICATE_ABSENT)`, so the application can tell "the
issuer offered none" apart from "one was offered and did not validate". Metadata that carries more than
one is reported as `Failed(MALFORMED)`. Neither is treated as a failure here — but note that the
OpenID4VCI library does reject the issuance itself in these cases, as described under
[Validation does not block](#validation-does-not-block).

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
  surfaced to the user.
- `IssuerRegistrationPolicy.Disabled` — the certificate is neither validated nor surfaced.

### Trust anchors

The certificate's signer chain and the status list used for the revocation check are validated against
the ETSI Trusted Lists, the same trust the presentation path uses. Without `configureEtsiTrust` there is
nothing to validate the certificate against, so validation is skipped and logged.

### Custom evaluation (advanced)

The default evaluator (`DefaultIssuerRegistrationEvaluator`) runs the same validity checks as the
presentation path — binding, expiry, status reference and revocation, with binding taken against the
certificate that signed the issuer metadata — and then compares the offered credential configurations
against the registered `provides_attestations` scope, reporting anything outside it as
`overProvidedAttestations`.

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
                // registration.reason — why validation failed
            }
            // Not handled: the policy is Disabled, signed issuer metadata is not configured,
            // or the issuer offered no registration certificate.
            null -> Unit
        }
    }
}
```

**During issuance** — `IssueEvent.IssuerRegistrationChecked`, delivered before `IssueEvent.Started`
whichever method started the issuance. Use it when the issuance does not begin from a resolved offer;
it arrives only when there is an outcome:

```kotlin
openId4VciManager.issueDocumentByConfigurationIdentifiers(issuerUrl, ids) { event ->
    when (event) {
        is IssueEvent.IssuerRegistrationChecked ->
            if (event.result.requiresExplicitApproval) {
                // warn the user before the issuance proceeds
            }
        // … the remaining issuance events
        else -> Unit
    }
}
```

### Validity and over-providing are two separate results

The same two-axis reading as on the presentation path applies, with `overProvidedAttestations` in
place of `overAskedClaims`: a `Verified` issuer registration can still be over-providing, and
`requiresExplicitApproval` folds both axes into one gate.
