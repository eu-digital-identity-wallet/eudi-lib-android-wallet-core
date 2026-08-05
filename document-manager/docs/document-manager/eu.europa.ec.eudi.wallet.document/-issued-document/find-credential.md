//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[IssuedDocument](index.md)/[findCredential](find-credential.md)

# findCredential

[release]\
suspend fun [findCredential](find-credential.md)(now: [Instant](https://developer.android.com/reference/kotlin/java/time/Instant.html)? = null): SecureAreaBoundCredential?

Finds the most appropriate credential for the current time or a specified time.

This method selects a valid credential based on the following criteria:

1. 
   The credential must be valid at the specified time (now by default)
2. 
   Among valid credentials, the one with the lowest usage count is selected

This approach ensures optimal credential rotation when using the RotateUse policy and helps prevent unnecessary credential invalidation.

#### Return

The most appropriate credential, or null if no valid credential is found

#### Parameters

release

| | |
|---|---|
| now | Optional timestamp for which to find a valid credential. If null, the current time is used. |