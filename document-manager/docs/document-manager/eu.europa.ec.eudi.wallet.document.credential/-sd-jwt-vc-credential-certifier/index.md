//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document.credential](../index.md)/[SdJwtVcCredentialCertifier](index.md)

# SdJwtVcCredentialCertifier

[release]\
class [SdJwtVcCredentialCertifier](index.md) : [CredentialCertification](../-credential-certification/index.md)

Certifies SD-JWT VC credentials by parsing the SD-JWT, verifying the device public key binding, and extracting validity periods.

**Important:** This certifier does **not** perform issuer trust verification (e.g., X.509 certificate path validation or issuer metadata resolution). Issuer trust verification is the responsibility of the integrating layer which has access to trusted issuer lists and certificate trust stores.

This certifier enforces:

- 
   Valid SD-JWT VC structure (parseable by the SD-JWT library)
- 
   Presence of the `cnf` (confirmation) claim with a `jwk` key
- 
   Device public key binding: the key in the `cnf` claim must match the credential's key
- 
   Extraction of validity period from `nbf`/`iat` and `exp` claims

## Constructors

| | |
|---|---|
| [SdJwtVcCredentialCertifier](-sd-jwt-vc-credential-certifier.md) | [release]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [certifyCredential](certify-credential.md) | [release]<br>open suspend override fun [certifyCredential](certify-credential.md)(credential: SecureAreaBoundCredential, issuedCredential: [IssuerProvidedCredential](../-issuer-provided-credential/index.md)) |