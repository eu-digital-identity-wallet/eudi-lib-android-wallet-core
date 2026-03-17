/*
 * Copyright (c) 2026 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.europa.ec.eudi.wallet.issue.openid4vci.reissue

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.multipaz.storage.StorageTableSpec

/**
 * Configuration data required for credential re-issuance.
 *
 * This data class stores all necessary information to re-issue a previously issued credential
 * without requiring the user to go through the full authorization flow again. It uses
 * the stored refresh token to obtain a new access token and issues a fresh credential.
 *
 * Note: Encryption configuration (requestEncryptionSpec, responseEncryptionParams) is NOT stored
 * because re-issuance creates a fresh Issuer via IssuerCreator, which automatically configures
 * encryption support from the issuer's current metadata.
 *
 * The data is persisted after successful credential issuance and can be used later to refresh
 * the credential (e.g., when it expires or needs to be updated).
 *
 * @property credentialIssuerId The credential issuer identifier (URL)
 * @property credentialConfigurationIdentifier The specific credential configuration ID that was issued
 * @property credentialEndpoint The issuer's credential endpoint URL
 * @property tokenEndpoint The token endpoint URL for refreshing access tokens
 * @property authorizationServerId The authorization server identifier (URL)
 * @property challengeEndpoint Optional challenge endpoint for attestation-based client authentication
 *
 * @property clientId The OAuth client ID
 * @property clientAttestationJwt Optional JWT for attestation-based client authentication (WIA)
 * @property clientAttestationPopKeyId Optional key ID for client attestation PoP signing
 *
 * @property popKeyAliases List of PoP (Proof-of-Possession) key aliases used in the original issuance
 * @property dPoPKeyAlias Optional DPoP key alias if DPoP was used in the original issuance
 *
 * @property accessToken The OAuth access token (may be expired)
 * @property accessTokenType "Bearer" or "DPoP"
 * @property refreshToken Optional refresh token to obtain new access tokens
 * @property tokenTimestamp Epoch seconds when the token was issued
 * @property grantType "authorization_code" or "pre-authorized_code"
 */
@Serializable
data class IssuanceMetadata(
    // Issuer identifiers
    val credentialIssuerId: String,
    val credentialConfigurationIdentifier: String,

    // Endpoints
    val credentialEndpoint: String,
    val tokenEndpoint: String,
    val authorizationServerId: String,
    val challengeEndpoint: String? = null,

    // Client authentication
    val clientId: String,
    val clientAttestationJwt: String? = null,
    val clientAttestationPopKeyId: String? = null,

    // Keys (SAME as original issuance)
    val popKeyAliases: List<String>,
    val dPoPKeyAlias: String? = null,

    // OAuth tokens
    val accessToken: String,
    val accessTokenType: String, // "Bearer" or "DPoP"
    val refreshToken: String? = null,
    val tokenTimestamp: Long, // epochSeconds
    val grantType: String, // "authorization_code" or "pre-authorized_code"
) {
    companion object {
        /**
         * Shared [StorageTableSpec] for the issuance metadata table.
         *
         * Multipaz [org.multipaz.storage.Storage] requires a single [StorageTableSpec] instance
         * per table name. This constant must be used by all callers that access the
         * `"issuance_metadata"` table.
         */
        internal val STORAGE_TABLE_SPEC = StorageTableSpec(
            name = "issuance_metadata",
            supportPartitions = false,
            supportExpiration = false
        )

        /**
         * Deserializes an [IssuanceMetadata] from a [ByteArray].
         *
         * This method reconstructs an [IssuanceMetadata] from its serialized JSON form.
         * The ByteArray should have been created using [IssuanceMetadata.toByteArray].
         *
         * @param bytes The serialized ByteArray
         * @return The deserialized [IssuanceMetadata]
         * @throws Exception if the bytes are invalid or corrupted
         */
        fun fromByteArray(bytes: ByteArray): IssuanceMetadata {
            return Json.decodeFromString<IssuanceMetadata>(bytes.toString(Charsets.UTF_8))
        }
    }

    /**
     * Serializes this [IssuanceMetadata] to a [ByteArray] for storage.
     *
     * This method converts the issuance metadata into a JSON-encoded ByteArray
     * that can be stored in a multipaz Storage table. Use [fromByteArray] to deserialize.
     *
     * @return The serialized ByteArray
     */
    fun toByteArray(): ByteArray {
        return Json.encodeToString(this).toByteArray(Charsets.UTF_8)
    }
}
