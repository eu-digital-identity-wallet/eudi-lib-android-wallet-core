//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../index.md)/[CredentialTrustVerifier](index.md)/[verify](verify.md)

# verify

[androidJvm]\
abstract suspend fun [verify](verify.md)(credentialValue: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), attestationIdentifier: AttestationIdentifier): CertificationChainValidation&lt;[TrustAnchor](https://developer.android.com/reference/kotlin/java/security/cert/TrustAnchor.html)&gt;?

Verify the issuer trust for a credential.

#### Return

the trust evaluation result, or `null` if the certificate chain could not be extracted or the verification context is not configured

#### Parameters

androidJvm

| | |
|---|---|
| credentialValue | the raw credential string (base64url-encoded CBOR for MsoMdoc, or SD-JWT string for SD-JWT VC) |
| attestationIdentifier | the attestation identifier derived from the document format |
