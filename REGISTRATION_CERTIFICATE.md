# Relying Party Registration Certificate

The **EUDI Wallet Core** library validates the **Wallet-Relying Party Registration Certificate
(WRPRC)** that a relying party may present with a data-sharing request, and surfaces the outcome to
the application so the user can be informed before sharing.

A WRPRC is a signed data object (a JWT per [ETSI TS 119 475](https://www.etsi.org/standards), or a
CWT/COSE) issued by a registration-certificate **provider**. It describes the relying party's
registered identity, intended use, and the attestations and claims it is registered to request. It
is distinct from the **access certificate (WRPAC)** — the X.509 e-seal that authenticates the request
itself — and is bound to it.

Handling the certificate lets the wallet meet two obligations of
[ETSI TS 119 472-2 clause 4.4](https://www.etsi.org/standards) and
Commission Implementing Regulation (EU) 2025/848 Article 8(2):

- **WRP-VALIDATION-02** — warn the user when the certificate cannot be validated.
- **WRP-OVERASKING-02** — warn the user when the request asks for attributes outside the registered
  scope.

> **Note:** Registration-certificate handling is **enabled by default**.

## What the wallet does with it

When a request carries a WRPRC, the wallet authenticates it (parse, signature, signer-chain trust),
evaluates it against the request (binding to the requester, expiry, revocation, over-asking), and
returns the result on the processed request. The application decides how to present that result; the
wallet does not silently share data on the user's behalf.

The certificate is carried differently per channel, but both are handed to the same validation, so
the checks and the returned result are the same regardless of channel — save for one
library-imposed exception on the OpenID4VP `x509_hash` path, described under [Behaviour](#behaviour):

| Channel | Carrier |
|---|---|
| Proximity (ISO/IEC 18013-5) and DC API (ISO mdoc) | `euWrprc` byte string in each `ItemsRequest` `requestInfo` (ETSI TS 119 472-2 §5.3.2) |
| Remote (OpenID4VP) and DC API (OpenID4VP) | `verifier_info` element with `format = "registration_cert"` (ETSI TS 119 472-2 §6.3.2.2) |

## Configuration

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
over the ETSI lists. Provider trust then uses that store, and status-list trust is unset — the
status-list token is verified against its own embedded `x5c` chain.

> **Note:** Binding to the requester needs the request to be reader-authenticated: the access
> certificate is taken from the proximity reader auth or the signed remote request. A request with no
> reader authentication has no requester identity, so binding fails (`NOT_BOUND_TO_REQUESTER`).
> Revocation is checked against the certificate's status list; a check that cannot be completed is
> treated as a failure, so a certificate is never presented as validated without a confirmed status.

### Custom evaluation (advanced)

Validation runs in two stages:

1. **Authentication** — parses the certificate, verifies its signature, and checks that the signer
   chain is trusted. This stage is always performed by the wallet core and cannot be replaced.
2. **Evaluation** — takes the authenticated registration and checks it against the request.

The default evaluator (`DefaultWrpRegistrationEvaluator`) performs, in order:

1. **Binding** — the certificate is bound to the access certificate that signed the request (the
   organization identifier for a legal person, or the serial number for a natural person; matched
   against the intermediary when the certificate names one).
2. **Expiry** — the certificate has not expired.
3. **Status reference present** — a status-list reference is included (mandatory).
4. **Revocation** — the status list is checked; a check that cannot be completed is treated as a
   failure.
5. **Over-asking** — the requested attributes are compared against the registered scope.

Each check derives from:

| Check | Failure reason | Source |
|---|---|---|
| Binding | `NOT_BOUND_TO_REQUESTER` | ETSI TS 119 475 §4.5 (WRPRC–WRPAC linked through the WRP identifier), §5.1.1 GEN-5.1.1-03/-04 (linkage relies solely on the matching identifier; where the certificate carries several, at least one must correspond to the access certificate), GEN-5.2.4-02 (the `sub` claim matches the WRP's registered identifier), §5.1.2 / §5.1.4 (identifier mapping) |
| Expiry | `EXPIRED` | WRP-VALIDATION-02; ETSI TS 119 475 §5.2.4 (`exp`) |
| Status reference present | `STATUS_MISSING` | ETSI TS 119 475 §6.2.6.2, REV-6.2.6.2-03 (status list mandatory) |
| Revocation | `REVOKED` / `REVOCATION_STATUS_UNKNOWN` | WRP-VALIDATION-02; ETSI TS 119 475 §6.2.6.2 |
| Over-asking | `overAskedClaims` | WRP-OVERASKING-01/02; CIR (EU) 2025/848 Article 8(2); ETSI TS 119 475 §5.2.4 (`credentials`, GEN-5.2.4-06) |

`WRP-VALIDATION-*` and `WRP-OVERASKING-*` are placed into ETSI TS 119 472-2 clause 4.4 by the
Implementing Act amending CIR (EU) 2024/2982; the overarching duty to validate before presenting is
`WRP-VALIDATION-01`. Binding is not one of the failure causes enumerated in `WRP-VALIDATION-02`; it
follows from the WRPRC–WRPAC relationship of ETSI TS 119 475 §4.5 (with the identifier-matching
mechanism specified in §5.1.1), which `WRP-VALIDATION-01` requires validating.

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
    ): WrpRegistrationResult                                     // Verified(…, overAskedClaims) or Failed(reason, …)
}
```

## Reading the outcome

The result is exposed uniformly on the processed request via `Success.wrpRegistration`, typed as
`WrpRegistrationInfo` and produced as a `WrpRegistrationResult`:

```kotlin
eudiWallet.addTransferEventListener { event ->
    if (event is TransferEvent.RequestReceived) {
        when (val processed = event.processedRequest) {
            is RequestProcessor.ProcessedRequest.Failure -> {
                // The request could not be processed.
                val error = processed.error
            }

            is RequestProcessor.ProcessedRequest.Success -> {
                when (val result = processed.wrpRegistration as? WrpRegistrationResult) {
                    is WrpRegistrationResult.Verified -> {
                        // result.registration — who the relying party is (name, country, purpose, …)
                        // result.overAskedClaims — attributes requested outside the registered scope
                    }
                    is WrpRegistrationResult.Failed -> {
                        // result.reason — why validation failed (authentication or evaluation)
                        // result.registration — the parsed registration, when read before the failure
                    }
                    null -> { /* no certificate handled (policy Disabled, or no WRPRC in the request) */ }
                }

                // Convenience gate: true when the user must explicitly approve before sharing
                // (evaluation failed, or the request is over-asking).
                val needsApproval = (processed.wrpRegistration as? WrpRegistrationResult)
                    ?.requiresExplicitApproval == true
            }
        }
    }
}
```

`WrpRegistrationResult`:

- **`Verified(registration, overAskedClaims)`** — the certificate was authenticated and validated.
  `overAskedClaims` lists any requested attributes outside the registered scope (empty when the
  request stays within scope).
- **`Failed(reason, registration?)`** — validation did not pass; `reason` is a
  `RegistrationFailureReason`. `registration` carries the parsed content when it was read before the
  failing check (for example an expired or revoked certificate), so the user can still be shown who
  the request is from.
- **`requiresExplicitApproval`** — `true` when the result is `Failed` or when there are over-asked
  claims. Per WRP-VALIDATION-02 / WRP-OVERASKING-02 the user must actively approve; silence or a
  pre-ticked box does not suffice.

### Validity and over-asking are two separate results

A request raises two independent questions, and you should check **both**:

1. **Is the certificate valid?** — `Verified` vs `Failed`.
2. **Does the request stay within the registered scope?** — `overAskedClaims`.

A `Verified` result does **not** mean the request is within scope: a fully valid certificate can
still be **over-asking**. Testing only for `Verified` is therefore not enough — inspect
`overAskedClaims` on it as well.

```kotlin
when (val result = success.wrpRegistration as? WrpRegistrationResult) {
    is WrpRegistrationResult.Verified ->
        if (result.overAskedClaims.isEmpty()) {
            // valid AND within scope — nothing to warn about
        } else {
            // valid, but the request is over-asking — warn and require approval
        }
    is WrpRegistrationResult.Failed ->
        // not valid — warn (result.reason) and require approval
        Unit
    null -> Unit
}
```

Over-asking is reported only on a `Verified` result: when validation fails the declared scope cannot
be relied on, so no over-asked claims are computed on a `Failed` result. When you only need a single
yes/no for the consent UI, `requiresExplicitApproval` folds both axes into one gate.

## Behaviour

With `Enabled`, the outcome falls into two groups:

- **Within scope and fully valid** → verified, no warning.
- **A check does not pass, or the request is over-asking** → surfaced as a `WrpRegistrationResult.Failed`
  (or a `Verified` carrying `overAskedClaims`) on `Success.wrpRegistration` for the user to approve.
  This covers every failure reason — the evaluation checks (not bound to the requester, expired, missing status reference, 
  revoked, revocation status unknown, over-asking) and the authentication checks (absent, malformed, 
  invalid signature, untrusted provider) alike.

### Failure reasons

| Reason | Meaning | Stage |
|---|---|---|
| `CERTIFICATE_ABSENT` | The request carried no registration certificate | Authentication |
| `MALFORMED` | The certificate could not be parsed | Authentication |
| `SIGNATURE_INVALID` | The signature could not be verified | Authentication |
| `UNTRUSTED_PROVIDER` | The signer chain is not trusted | Authentication |
| `NOT_BOUND_TO_REQUESTER` | The certificate is not bound to the access certificate that signed the request | Evaluation |
| `EXPIRED` | The certificate has expired | Evaluation |
| `STATUS_MISSING` | The certificate carries no status-list reference (mandatory) | Evaluation |
| `REVOKED` | The status list reports the certificate as revoked | Evaluation |
| `REVOCATION_STATUS_UNKNOWN` | The revocation status could not be determined | Evaluation |