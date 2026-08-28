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
    val replacesDocumentId: String? = null,

    // Interacting party (TS10 §3.5): the credential issuer's registration identity, captured at
    // deferral so the transaction log can name it at resolution, when the certificate is gone.
    val interactingParty: StoredIssuerRegistration? = null,
)

/**
 * The log-relevant subset of a credential issuer's registration certificate, persisted with a deferred
 * document so its interacting-party fields (TS10 §3.5) survive until the credential resolves. The raw
 * fields are stored rather than the mapped log values, so one mapping serves both the synchronous and
 * the deferred issuance paths.
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
)