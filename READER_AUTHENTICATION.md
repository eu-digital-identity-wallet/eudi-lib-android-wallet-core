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
| `requester.requesterIdentities` | One identity for each reader signature, with its certificate chain. Empty when the request carries no reader signature, or when a signature does not verify. |

## Policies

| Policy | The request is rejected when |
|---|---|
| `DoNotEnforce` | never |
| `EnforceIfPresent` (default) | a reader signature does not verify, or a signer's certificate is not trusted |
| `AlwaysRequire` | the request is not trust-verified |

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

1. **Every signature must verify**, of both kinds. When a signature does not verify, reader
   authentication fails and no identity is reported. The wallet does not fall back to the other
   signatures of the request.
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
| A signature does not verify | `null` | none | rejected | rejected |
| `readerAuthAll` by A, with or without `readerAuth` on DocRequests | A | A | shared | shared |
| `readerAuthAll` by X only | `null` | X | rejected | rejected |

With `DoNotEnforce`, every request is shared after the user's consent.

### Which reader signed which document

`trustMetadata.displayName` names one reader. When different trusted readers sign different
DocRequests, it is the first of them. To show which reader signed which document, the application
can read the signature of the DocRequest that each document answers:

```kotlin
val docRequest = (match.source as? CredentialMatchSourceIso18013)?.docRequest
val signerChain: X509CertChain? = docRequest?.readerAuth?.let { readerAuth ->
    (readerAuth.protectedHeaders[Cose.COSE_LABEL_X5CHAIN.toCoseLabel]
        ?: readerAuth.unprotectedHeaders[Cose.COSE_LABEL_X5CHAIN.toCoseLabel])?.asX509CertChain
}
// signerChain is null when the DocRequest is not signed. Otherwise it is one of the
// certificate chains in requesterIdentities.
```

With `readerAuthAll`, every signature covers every document, so there is no signer per document.

## OpenID4VP requests

An OpenID4VP request object carries one signature. The request is trust-verified when that
signature verifies and its certificate is trusted, and `requesterIdentities` contains that one
identity. An unsigned request has no identity. Multi-signed requests (JWS JSON Serialization,
`openid4vp-v1-multisigned`) are not supported.
