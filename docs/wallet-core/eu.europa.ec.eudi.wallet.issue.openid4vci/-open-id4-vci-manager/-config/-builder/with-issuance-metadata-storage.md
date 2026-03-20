//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../../index.md)/[OpenId4VciManager](../../index.md)/[Config](../index.md)/[Builder](index.md)/[withIssuanceMetadataStorage](with-issuance-metadata-storage.md)

# withIssuanceMetadataStorage

[androidJvm]\
fun [withIssuanceMetadataStorage](with-issuance-metadata-storage.md)(storage: Storage?): &lt;Error class: unknown class&gt;

Sets the storage for issuance metadata.

When configured, the library will automatically store metadata after successful credential issuance, enabling credentials to be re-issued later without requiring the user to go through the full authorization flow again.

The metadata includes:

- 
   Access token and refresh token
- 
   Token endpoint and credential endpoint URLs
- 
   Key aliases used for proof-of-possession
- 
   DPoP key alias (if DPoP was used)
- 
   Client attestation information (if attestation-based auth was used)

If not set, a default org.multipaz.storage.android.AndroidStorage will be used with a separate database file for issuance metadata.

#### Return

This builder instance for method chaining

#### Parameters

androidJvm

| | |
|---|---|
| storage | The Storage implementation for issuance metadata, or null to use default |

#### See also

| |
|---|
| Storage |
| [IssuanceMetadata](../../../../eu.europa.ec.eudi.wallet.issue.openid4vci.reissue/-issuance-metadata/index.md) |
