package eu.europa.ec.eudi.wallet.issue.openid4vci.reissue

import kotlinx.serialization.Serializable

@Serializable
internal data class StoredDeferredContext(
    // Configuration URIs
    val credentialIssuerId: String,
    val deferredEndpoint: String,
    val tokenEndpoint: String,
    val authorizationServerId: String,
    val challengeEndpoint: String? = null,


    val clientId: String,
    // Ordered list of keys used for the credentials
    val popKeyAliases: List<String>,
    val dPoPKeyAlias: String? = null,
    val clientAttestationPopKeyId: String? = null,
    val clientAttestationJwt: String? = null,

    // Transaction State. The transaction id and the tokens are stored encrypted in sealedTokens
    // (a DeferredTokens); contexts written before they were encrypted have them in plain text.
    val transactionId: String? = null,
    val accessToken: String? = null,
    val accessTokenType: String = "DPoP",
    val refreshToken: String? = null,
    val sealedTokens: String? = null,

    // Encryption Specs
    val requestEncryptionKeyJwk: String? = null,
    val requestEncryptionMethod: String? = null,
    val responseEncryptionMethod: String? = null,

    // Metadata required for ReIssuance: needed to store IssuanceMetadata when deferred credential is issued
    val credentialConfigurationIdentifier: String? = null,
    val credentialEndpoint: String? = null,

    // Re-issuance linkage: if this deferred credential replaces an existing document
    val replacesDocumentId: String? = null,

    // Kept at deferral so the transaction log still has them when the credential resolves (TS10 §3.5).
    val interactingParty: StoredIssuerRegistration? = null,
    val isUserTriggered: Boolean? = null,
) {
    companion object {
        /**
         * The fields to which the encrypted tokens are not bound: the transaction id and the tokens, in
         * plain text or encrypted, and the fields kept for the transaction log. The tokens are bound to
         * every other field of a context.
         */
        val UNBOUND_FIELDS = setOf(
            "transactionId", "accessToken", "refreshToken", "sealedTokens", "interactingParty", "isUserTriggered",
        )
    }
}

/**
 * The fields of a [StoredDeferredContext] that are stored encrypted.
 */
@Serializable
internal data class DeferredTokens(
    val transactionId: String,
    val accessToken: String,
    val refreshToken: String? = null,
)

/**
 * The subset of a credential issuer's registration certificate the transaction log needs. The raw
 * fields are stored rather than the mapped values, so one mapping serves both issuance paths.
 */
@Serializable
internal data class StoredIssuerRegistration(
    val name: String? = null,
    val legalName: String? = null,
    val givenName: String? = null,
    val familyName: String? = null,
    val identifiers: List<String> = emptyList(),
    val entitlements: List<String> = emptyList(),
    val country: String? = null,
    val infoUri: String? = null,
    val supportUri: String? = null
)