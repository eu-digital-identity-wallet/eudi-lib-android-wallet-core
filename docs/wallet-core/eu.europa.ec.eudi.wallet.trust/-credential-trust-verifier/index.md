//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../index.md)/[CredentialTrustVerifier](index.md)

# CredentialTrustVerifier

fun interface [CredentialTrustVerifier](index.md)

Per-format credential trust verifier. Implementations extract the certificate chain from the credential and evaluate trust using the ETSI library.

Built-in implementations:

- 
   MsoMdocCredentialTrustVerifier for MsoMdoc credentials (uses multipaz CBOR/COSE)
- 
   SdJwtVcCredentialTrustVerifier for SD-JWT VC credentials (uses eudi sd-jwt-vc library)

#### See also

| | |
|---|---|
| [IssuerTrustConfigBuilder.credentialTrustVerifier](../-issuer-trust-config-builder/credential-trust-verifier.md) | for registering custom verifiers |

## Functions

| Name | Summary |
|---|---|
| [verify](verify.md) | [androidJvm]<br>abstract suspend fun [verify](verify.md)(credentialValue: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), attestationIdentifier: AttestationIdentifier): CertificationChainValidation&lt;[TrustAnchor](https://developer.android.com/reference/kotlin/java/security/cert/TrustAnchor.html)&gt;?<br>Verify the issuer trust for a credential. |
