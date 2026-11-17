/*
 * Copyright (c) 2024-2025 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.jwk.JWK
import eu.europa.ec.eudi.openid4vci.AccessToken
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.AuthorizedTransaction
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.DeferredIssuanceContext
import eu.europa.ec.eudi.openid4vci.DeferredIssuerConfig
import eu.europa.ec.eudi.openid4vci.EncryptionSpec
import eu.europa.ec.eudi.openid4vci.Grant
import eu.europa.ec.eudi.openid4vci.HttpsUrl
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.JwsAlgorithm
import eu.europa.ec.eudi.openid4vci.RefreshToken
import eu.europa.ec.eudi.openid4vci.SignOperation
import eu.europa.ec.eudi.openid4vci.Signer
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.openid4vci.TransactionId
import eu.europa.ec.eudi.wallet.internal.crypto.DataSealer
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.SecureAreaDpopSigner
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.DeferredTokens
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.StoredDeferredContext
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.StoredIssuerRegistration
import eu.europa.ec.eudi.wallet.issue.openid4vci.tokens.TokenRecord
import eu.europa.ec.eudi.wallet.issue.openid4vci.tokens.UndecodableTokenRecordException
import eu.europa.ec.eudi.wallet.issue.openid4vci.tokens.boundValues
import eu.europa.ec.eudi.wallet.issue.openid4vci.tokens.decodeTokenRecord
import eu.europa.ec.eudi.wallet.issue.openid4vci.tokens.tokenRecordJson
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.provider.WalletInstanceAttestationProvider
import eu.europa.ec.eudi.wallet.provider.WalletKeyManager
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.time.Clock
import java.time.Instant
import eu.europa.ec.eudi.openid4vci.DPoPConfig as VciDPoPConfig
import eu.europa.ec.eudi.openid4vci.ProvisionDPoPSigner as VciProvisionDPoPSigner
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Factory for creating [DeferredContext] instances from key aliases and deferred submission outcomes.
 *
 * @param keyAliases List of key aliases used in the deferred issuance process
 * @param outcome The outcome of a deferred submission containing transaction details
 * @param clientAttestationPopKeyId The client attestation PoP key ID, if any
 * @param dPoPKeyAlias The DPoP key alias, if any
 * @return A [DeferredContext] containing the issuance context and key aliases
 */
internal typealias DeferredContextFactory = (
    keyAliases: List<String>,
    outcome: SubmissionOutcome.Deferred,
    clientAttestationPopKeyId: String?,
    dPoPKeyAlias: String?,
) -> DeferredContext

/**
 * Creates a [DeferredContextFactory] from an [Issuer] and [AuthorizedRequest].
 *
 * @param issuer The credential issuer that will provide the deferred context
 * @param authorizedRequest The authorized request associated with the deferred issuance
 * @param dPoPKeyAlias The DPoP key alias to use as default, if any
 * @return A factory function that creates [DeferredContext] instances
 */
internal fun DeferredContextFactory(
    issuer: Issuer,
    authorizedRequest: AuthorizedRequest,
    dPoPKeyAlias: String?,
): DeferredContextFactory {
    return { keyAliases, outcome, clientAttestationPopKeyId, dpopAlias ->
        with(issuer) {
            DeferredContext(
                issuanceContext = authorizedRequest.deferredContext(outcome),
                keyAliases = keyAliases,
                clientAttestationPopKeyId = clientAttestationPopKeyId,
                dPoPKeyAlias = dpopAlias ?: dPoPKeyAlias,
            )
        }
    }
}

/**
 * A data class that holds the deferred issuance context along with key aliases.
 * Used for storing and retrieving deferred issuance data.
 *
 * @property issuanceContext The deferred issuance context containing information about the credential issuance process.
 * @property keyAliases List of key aliases used in the deferred issuance process.
 * @property clientAttestationPopKeyId The client attestation PoP key ID, if any.
 * @property dPoPKeyAlias The DPoP key alias used during issuance, if any.
 */
internal data class DeferredContext(
    val issuanceContext: DeferredIssuanceContext,
    val keyAliases: List<String>,
    val clientAttestationPopKeyId: String?,
    val dPoPKeyAlias: String?,
    val credentialConfigurationIdentifier: String? = null,
    val credentialEndpoint: String? = null,
    val replacesDocumentId: String? = null,
    val interactingParty: StoredIssuerRegistration? = null,
    val isUserTriggered: Boolean? = null
) {
    companion object
}

/**
 * Reads the stored context of the deferred document [documentId] and decrypts its transaction id and
 * tokens with [tokenSealer]. The tokens are bound to the fields of the context as they are stored,
 * except the [StoredDeferredContext.UNBOUND_FIELDS].
 *
 * @throws eu.europa.ec.eudi.wallet.internal.crypto.SealedDataUnreadableException when the tokens
 * can never be decrypted
 * @throws UndecodableTokenRecordException when the context cannot be decoded
 */
internal fun readStoredDeferredContext(
    bytes: ByteArray,
    documentId: String,
    tokenSealer: DataSealer,
): Pair<StoredDeferredContext, DeferredTokens> {
    val fields = decodeTokenRecord(documentId) { tokenRecordJson.parseToJsonElement(String(bytes, Charsets.UTF_8)).jsonObject }
    val dto = decodeTokenRecord(documentId) {
        tokenRecordJson.decodeFromJsonElement(StoredDeferredContext.serializer(), fields)
    }
    val tokens = dto.sealedTokens?.let { sealedTokens ->
        if (dto.transactionId != null || dto.accessToken != null || dto.refreshToken != null) {
            throw UndecodableTokenRecordException("The deferred context of document $documentId has both encrypted and plain tokens")
        }
        val boundFields = decodeTokenRecord(documentId) { boundValues(fields, StoredDeferredContext.UNBOUND_FIELDS) }
        val opened = tokenSealer.open(TokenRecord.DEFERRED_CONTEXT, documentId, boundFields, sealedTokens)
        decodeTokenRecord(documentId) { tokenRecordJson.decodeFromString<DeferredTokens>(opened.toString(Charsets.UTF_8)) }
    } ?: DeferredTokens( // A context written before the tokens were encrypted
        transactionId = dto.transactionId
            ?: throw UndecodableTokenRecordException("The deferred context of document $documentId has no transaction id"),
        accessToken = dto.accessToken
            ?: throw UndecodableTokenRecordException("The deferred context of document $documentId has no access token"),
        refreshToken = dto.refreshToken,
    )
    return dto to tokens
}

/**
 * Reads the context stored with the deferred document [documentId], decrypting its transaction id
 * and tokens with [tokenSealer].
 *
 * @throws eu.europa.ec.eudi.wallet.internal.crypto.SealedDataUnreadableException when
 * they can never be decrypted
 * @throws UndecodableTokenRecordException when the context cannot be decoded
 */
internal fun DeferredContext.Companion.fromBytes(
    bytes: ByteArray,
    documentId: String,
    tokenSealer: DataSealer,
    walletKeyManager: WalletKeyManager,
    walletInstanceAttestationProvider: WalletInstanceAttestationProvider?,
    dpopConfig: DPopConfig.Custom? = null,
    logger: Logger? = null,
): DeferredContext {
    val (dto, tokens) = readStoredDeferredContext(bytes, documentId, tokenSealer)

    // Recreate client authentication
    val clientAuthentication =
        if (dto.clientAttestationPopKeyId != null && walletInstanceAttestationProvider != null) {
            val key = runBlocking {
                walletKeyManager.getWalletAttestationKey(dto.clientAttestationPopKeyId)
            } ?: error("Key alias '${dto.clientAttestationPopKeyId}' not found in WalletKeyManager")

            with(key) {
                walletInstanceAttestationProvider.toClientAuthentication(dto.clientId)
            }.getOrThrow()
        } else {
            ClientAuthentication.None(dto.clientId)
        }

    // Recreate DPoP config from the SecureArea using the stored key alias.
    // DPoP keys are managed by SecureArea (not WalletKeyManager), so we use
    // SecureAreaDpopSigner.fromExistingKey() which looks up the key by alias.
    val vciDPoPConfig = if (dto.dPoPKeyAlias != null && dpopConfig != null) {
        val storedDPoPKeyAlias = dto.dPoPKeyAlias
        val storedDpopConfig = dpopConfig
        VciDPoPConfig(
            provisionDPoPSigner = object : VciProvisionDPoPSigner {
                override val popAlgorithm: JwsAlgorithm
                    get() {
                        val signer = runBlocking {
                            SecureAreaDpopSigner.fromExistingKey(storedDpopConfig, storedDPoPKeyAlias, logger)
                        }
                        return JwsAlgorithm(signer.keyInfo.algorithm.joseAlgorithmIdentifier!!)
                    }

                override suspend fun invoke(authorizationServer: HttpsUrl): Signer<JWK> {
                    return SecureAreaDpopSigner.fromExistingKey(storedDpopConfig, storedDPoPKeyAlias, logger)
                }
            }
        )
    } else null

    // Recreate encryption spec
    val reqEncSpec =
        if (dto.requestEncryptionKeyJwk != null && dto.requestEncryptionMethod != null) {
            EncryptionSpec(
                recipientKey = JWK.parse(dto.requestEncryptionKeyJwk),
                encryptionMethod = EncryptionMethod.parse(dto.requestEncryptionMethod)
            )
        } else null

    val respEncParams = dto.responseEncryptionMethod?.let {
        EncryptionMethod.parse(it) to null
    }

    // Build configuration
    val config = DeferredIssuerConfig(
        credentialIssuerId = CredentialIssuerId(dto.credentialIssuerId).getOrThrow(),
        clientAuthentication = clientAuthentication,
        deferredEndpoint = URI(dto.deferredEndpoint).toURL(),
        authorizationServerId = URI(dto.authorizationServerId).toURL(),
        challengeEndpoint = dto.challengeEndpoint?.let { URI(it).toURL() },
        tokenEndpoint = URI(dto.tokenEndpoint).toURL(),
        requestEncryptionSpec = reqEncSpec,
        responseEncryptionParams = respEncParams,
        dPoPConfig = vciDPoPConfig,
        clock = Clock.systemUTC(),
    )

    // Build transaction
    val accessToken = if (dto.accessTokenType == "DPoP") {
        AccessToken.DPoP(tokens.accessToken, null)
    } else {
        AccessToken.Bearer(tokens.accessToken, null)
    }

    val transaction = AuthorizedTransaction(
        authorizedRequest = AuthorizedRequest(
            accessToken = accessToken,
            refreshToken = tokens.refreshToken?.let { RefreshToken(it) },
            credentialIdentifiers = emptyMap(),
            timestamp = Instant.now(),
            grant = Grant.AuthorizationCode,
            authorizationServerDpopNonce = null,
            resourceServerDpopNonce = null,
        ),
        transactionId = TransactionId(tokens.transactionId)
    )

    return DeferredContext(
        issuanceContext = DeferredIssuanceContext(config, transaction),
        keyAliases = dto.popKeyAliases,
        clientAttestationPopKeyId = dto.clientAttestationPopKeyId,
        dPoPKeyAlias = dto.dPoPKeyAlias,
        credentialConfigurationIdentifier = dto.credentialConfigurationIdentifier,
        credentialEndpoint = dto.credentialEndpoint,
        replacesDocumentId = dto.replacesDocumentId,
        interactingParty = dto.interactingParty,
        isUserTriggered = dto.isUserTriggered
    )
}

/**
 * Whether [bytes] is a deferred context stored before its transaction id and tokens were encrypted.
 */
internal fun DeferredContext.Companion.isStoredInPlainText(bytes: ByteArray): Boolean =
    decodeTokenRecord("a deferred document") {
        tokenRecordJson.decodeFromString<StoredDeferredContext>(String(bytes, Charsets.UTF_8))
    }.sealedTokens == null

/**
 * Serializes this context to be stored with the deferred document [documentId]. Its transaction id
 * and tokens are encrypted with [tokenSealer].
 */
internal fun DeferredContext.toBytes(documentId: String, tokenSealer: DataSealer): ByteArray {
    val tx = issuanceContext.authorizedTransaction
    val cfg = issuanceContext.config
    val tokens = DeferredTokens(
        transactionId = tx.transactionId.value,
        accessToken = tx.authorizedRequest.accessToken.accessToken,
        refreshToken = tx.authorizedRequest.refreshToken?.refreshToken,
    )

    val dto = StoredDeferredContext(
        credentialIssuerId = cfg.credentialIssuerId.toString(),
        deferredEndpoint = cfg.deferredEndpoint.toString(),
        tokenEndpoint = cfg.tokenEndpoint.toString(),
        authorizationServerId = cfg.authorizationServerId.toString(),
        challengeEndpoint = cfg.challengeEndpoint?.toString(),
        clientId = cfg.clientAuthentication.id,
        popKeyAliases = keyAliases,
        dPoPKeyAlias = dPoPKeyAlias,
        clientAttestationPopKeyId = clientAttestationPopKeyId,
        clientAttestationJwt = null,
        accessTokenType = if (tx.authorizedRequest.accessToken is AccessToken.DPoP) "DPoP" else "Bearer",
        requestEncryptionKeyJwk = cfg.requestEncryptionSpec?.recipientKey?.toJSONString(),
        requestEncryptionMethod = cfg.requestEncryptionSpec?.encryptionMethod?.name,
        responseEncryptionMethod = cfg.responseEncryptionParams?.first?.name,
        credentialConfigurationIdentifier = credentialConfigurationIdentifier,
        credentialEndpoint = credentialEndpoint,
        replacesDocumentId = replacesDocumentId,
        interactingParty = interactingParty,
        isUserTriggered = isUserTriggered
    )

    val fields = tokenRecordJson.encodeToJsonElement(StoredDeferredContext.serializer(), dto).jsonObject
    val sealedTokens = tokenSealer.seal(
        TokenRecord.DEFERRED_CONTEXT,
        documentId,
        boundValues(fields, StoredDeferredContext.UNBOUND_FIELDS),
        tokenRecordJson.encodeToString(DeferredTokens.serializer(), tokens).toByteArray(Charsets.UTF_8)
    )
    val record = JsonObject(fields + ("sealedTokens" to JsonPrimitive(sealedTokens)))
    return tokenRecordJson.encodeToString(JsonObject.serializer(), record).toByteArray(Charsets.UTF_8)
}
