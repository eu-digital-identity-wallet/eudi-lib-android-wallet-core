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

    // Transaction State
    val transactionId: String,
    val accessToken: String,
    val accessTokenType: String = "DPoP",
    val refreshToken: String? = null,

    // Encryption Specs
    val requestEncryptionKeyJwk: String? = null,
    val requestEncryptionMethod: String? = null,
    val responseEncryptionMethod: String? = null,

    // Metadata required for ReIssuance: needed to store IssuanceMetadata when deferred credential is issued
    val credentialConfigurationIdentifier: String? = null,
    val credentialEndpoint: String? = null,

    // Re-issuance linkage: if this deferred credential replaces an existing document
    val replacesDocumentId: String? = null
)