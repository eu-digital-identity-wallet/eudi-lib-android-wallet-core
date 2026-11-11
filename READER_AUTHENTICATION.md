# Reader Authentication

When a relying party signs its presentation request, the **EUDI Wallet Core** library verifies the
signature, validates the reader's certificate chain against the configured trust sources, and
applies the configured `ReaderAuthPolicy`. This page describes what the application receives and
when a request is rejected. For the configuration itself, see
[Reader Authentication Policy](README.md#reader-authentication-policy).

## What the application receives

A processed request (`RequestProcessor.ProcessedRequest.Success`) carries two values about the
reader:

| Value | Meaning |
|---|---|
| `trustMetadata` | Not `null` only when the request is **trust-verified**. `displayName` is the reader's name: for ISO/IEC 18013-5, the common name of the first trusted reader certificate; for OpenID4VP, the verifier's legal name. |
| `requester.requesterIdentities` | The reader identities, each with its certificate chain. Empty when the request carries no reader signature. |
| `wrpRegistration` | The result of the registration certificate validation, when a registration validator is configured. See [Registration certificate](#registration-certificate). |

## Policies

| Policy | The request is rejected when |
|---|---|
| `DoNotEnforce` | never |
| `EnforceIfPresent` (default) | a signer's certificate is not trusted |
| `AlwaysRequire` | the request is not trust-verified |

`DoNotEnforce` evaluates trust only when a trust source is configured; without one, `trustMetadata`
is always `null`.

When the request is rejected, no document is shared:

- ISO/IEC 18013-5 requests (proximity and DC API `org-iso-mdoc`): the response is a DeviceResponse
  with status 10 (General Error) and no documents.
- OpenID4VP requests: generating the response fails with `ReaderAuthPolicyException`.

A request that `EnforceIfPresent` does not reject is shared after the user's consent, also when it
is not trust-verified. In that case `trustMetadata` is `null`.

## ISO/IEC 18013-5 requests

ISO/IEC 18013-5 has two ways to sign a request:

- **`readerAuth`** on a DocRequest: one signature that covers only that DocRequest.
- **`readerAuthAll`** (second edition of ISO/IEC 18013-5): one or more signatures, each covering
  the whole request.

### Rules

1. **Every signature must verify**, of both kinds. When a signature does not verify, processing of
   the request fails with `SignatureVerificationException` under every policy, including
   `DoNotEnforce`: the application receives `RequestProcessor.ProcessedRequest.Failure`. The wallet
   does not fall back to the other signatures of the request.
2. **With `readerAuthAll`**, only the `readerAuthAll` signatures decide trust. The request is
   trust-verified when at least one of them is trusted, since each one covers the whole request.
   Several `readerAuthAll` signatures are expected to be identities of the same reader, for example
   one per trust framework. The `readerAuth` of each DocRequest is then only checked to verify.
   `requesterIdentities` contains one identity for each `readerAuthAll` signature.
3. **Without `readerAuthAll`**, the request is trust-verified only when **every** DocRequest is
   signed and **every** signer is trusted. Reader authentication fails when a signer is not
   trusted, wherever its DocRequest is in the request. `requesterIdentities` contains one identity
   for each distinct signer, in the order of the DocRequests.

### Examples

A and B are trusted readers, X is a reader whose certificate is not trusted.

| Request | `trustMetadata` | `requesterIdentities` | `EnforceIfPresent` | `AlwaysRequire` |
|---|---|---|---|---|
| A signs every DocRequest | A | A | shared | shared |
| A signs one DocRequest, B signs another | A | A, B | shared | shared |
| A signs one DocRequest, another is unsigned | `null` | A | shared | rejected |
| A signs one DocRequest, X signs another, in any order | `null` | A, X | rejected | rejected |
| No DocRequest is signed | `null` | none | shared | rejected |
| A signature does not verify | - | - | processing fails | processing fails |
| `readerAuthAll` by A, with or without `readerAuth` on DocRequests | A | A | shared | shared |
| `readerAuthAll` by X only | `null` | X | rejected | rejected |

With `DoNotEnforce`, a request whose signatures verify is shared after the user's consent.

`trustMetadata.displayName` names one reader. When different trusted readers sign different
DocRequests, it is the first of them, and `requesterIdentities` lists every signer.

### Registration certificate

The registration certificate is carried in the `requestInfo` of each DocRequest (ETSI TS 119 472-2
clause 5.3.2), and it must be bound to the access certificate of the relying party that signed the
request. The wallet checks it against the access certificate of a trusted reader that signs the
whole request:

- with `readerAuthAll`, the first trusted `readerAuthAll` signer;
- without `readerAuthAll`, the reader that signs every DocRequest, when it is trusted.

Otherwise the registration certificate is reported as not bound to the requester
(`NOT_BOUND_TO_REQUESTER`): when a DocRequest is not signed, when different readers sign the
DocRequests, when no signer is trusted, and always with `DoNotEnforce` without a trust source, which
evaluates no trust.

## OpenID4VP requests

An OpenID4VP request object carries one signature. The OpenID4VP library rejects a request object
whose signature does not verify, or whose `x5c` certificate chain is not trusted, when it resolves
the request: the application receives an error instead of a request to consent to. A request that
is resolved with an `x5c` certificate chain is trust-verified, and `requesterIdentities` contains
that one identity.
An unsigned request has no identity. Multi-signed requests (JWS JSON Serialization,
`openid4vp-v1-multisigned`) are not supported.
