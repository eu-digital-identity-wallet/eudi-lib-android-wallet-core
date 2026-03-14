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
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.openid4vci.AccessToken
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.AuthorizedTransaction
import eu.europa.ec.eudi.openid4vci.ClientAttestationJWT
import eu.europa.ec.eudi.openid4vci.ClientAttestationPoPJWTSpec
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.DeferredIssuanceContext
import eu.europa.ec.eudi.openid4vci.DeferredIssuerConfig
import eu.europa.ec.eudi.openid4vci.EncryptionSpec
import eu.europa.ec.eudi.openid4vci.Grant
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.RefreshToken
import eu.europa.ec.eudi.openid4vci.SignOperation
import eu.europa.ec.eudi.openid4vci.Signer
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.openid4vci.TransactionId
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.SecureAreaDpopSigner
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.StoredDeferredContext
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.provider.WalletKeyManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.time.Clock
import java.time.Instant

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
) {
    companion object
}

internal fun DeferredContext.Companion.fromBytes(
    bytes: ByteArray,
    walletKeyManager: WalletKeyManager,
    dpopConfig: DPopConfig.Custom? = null,
    logger: Logger? = null,
): DeferredContext {
    val dto = Json.decodeFromString<StoredDeferredContext>(String(bytes, Charsets.UTF_8))

    // Recreate client authentication
    val clientAuthentication =
        if (dto.clientAttestationJwt != null && dto.clientAttestationPopKeyId != null) {
            val jwt = runCatching {
                ClientAttestationJWT(SignedJWT.parse(dto.clientAttestationJwt))
            }.getOrNull() ?: error("Invalid client attestation JWT in stored context")

            val popSigner = createSignerAdapter(walletKeyManager, dto.clientAttestationPopKeyId)
            val popSpec = ClientAttestationPoPJWTSpec(signer = popSigner)
            ClientAuthentication.AttestationBased(jwt, popSpec)
        } else {
            ClientAuthentication.None(dto.clientId)
        }

    // Recreate DPoP signer from the SecureArea using the stored key alias.
    // DPoP keys are managed by SecureArea (not WalletKeyManager), so we use
    // SecureAreaDpopSigner.fromExistingKey() which looks up the key by alias.
    val dPoPSigner = if (dto.dPoPKeyAlias != null && dpopConfig != null) {
        runBlocking {
            runCatching {
                SecureAreaDpopSigner.fromExistingKey(dpopConfig, dto.dPoPKeyAlias, logger)
            }.getOrNull()
        }
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
        dPoPSigner = dPoPSigner,
        clock = Clock.systemUTC()
    )

    // Build transaction
    val accessToken = if (dto.accessTokenType == "DPoP") {
        AccessToken.DPoP(dto.accessToken, null)
    } else {
        AccessToken.Bearer(dto.accessToken, null)
    }

    val transaction = AuthorizedTransaction(
        authorizedRequest = AuthorizedRequest(
            accessToken = accessToken,
            refreshToken = dto.refreshToken?.let { RefreshToken(it) },
            credentialIdentifiers = emptyMap(),
            timestamp = Instant.now(),
            grant = Grant.AuthorizationCode,
            authorizationServerDpopNonce = null,
            resourceServerDpopNonce = null,
        ),
        transactionId = TransactionId(dto.transactionId)
    )

    return DeferredContext(
        issuanceContext = DeferredIssuanceContext(config, transaction),
        keyAliases = dto.popKeyAliases,
        clientAttestationPopKeyId = dto.clientAttestationPopKeyId,
        dPoPKeyAlias = dto.dPoPKeyAlias,
        credentialConfigurationIdentifier = dto.credentialConfigurationIdentifier,
        credentialEndpoint = dto.credentialEndpoint,
        replacesDocumentId = dto.replacesDocumentId,
    )
}

internal fun DeferredContext.toBytes(): ByteArray {
    val tx = issuanceContext.authorizedTransaction
    val cfg = issuanceContext.config

    val (clientId, attestationJwt) = when (val auth = cfg.clientAuthentication) {
        is ClientAuthentication.None -> auth.id to null
        is ClientAuthentication.AttestationBased -> auth.id to auth.attestationJWT.jwt.serialize()
        else -> auth.id to null
    }

    val dto = StoredDeferredContext(
        credentialIssuerId = cfg.credentialIssuerId.toString(),
        deferredEndpoint = cfg.deferredEndpoint.toString(),
        tokenEndpoint = cfg.tokenEndpoint.toString(),
        authorizationServerId = cfg.authorizationServerId.toString(),
        challengeEndpoint = cfg.challengeEndpoint?.toString(),
        clientId = clientId,
        popKeyAliases = keyAliases,
        dPoPKeyAlias = dPoPKeyAlias,
        clientAttestationPopKeyId = clientAttestationPopKeyId,
        clientAttestationJwt = attestationJwt,
        transactionId = tx.transactionId.value,
        accessToken = tx.authorizedRequest.accessToken.accessToken,
        accessTokenType = if (tx.authorizedRequest.accessToken is AccessToken.DPoP) "DPoP" else "Bearer",
        refreshToken = tx.authorizedRequest.refreshToken?.refreshToken,
        requestEncryptionKeyJwk = cfg.requestEncryptionSpec?.recipientKey?.toJSONString(),
        requestEncryptionMethod = cfg.requestEncryptionSpec?.encryptionMethod?.name,
        responseEncryptionMethod = cfg.responseEncryptionParams?.first?.name,
        credentialConfigurationIdentifier = credentialConfigurationIdentifier,
        credentialEndpoint = credentialEndpoint,
        replacesDocumentId = replacesDocumentId,
    )

    return Json.encodeToString(dto).toByteArray(Charsets.UTF_8)
}

private fun createSignerAdapter(walletKeyManager: WalletKeyManager, keyAlias: String): Signer<JWK> {
    val key = runBlocking {
        walletKeyManager.getWalletAttestationKey(keyAlias)
    } ?: error("Key alias '$keyAlias' not found in WalletKeyManager")

    return object : Signer<JWK> {
        override val javaAlgorithm: String = key.keyInfo.algorithm.javaAlgorithm
            ?: error("Key algorithm is required for alias $keyAlias")

        override suspend fun acquire(): SignOperation<JWK> {
            return SignOperation(
                function = { inputData -> key.signFunction(inputData) },
                publicMaterial = JWK.parse(key.keyInfo.publicKey.toJwk().toString())
            )
        }

        override suspend fun release(signOperation: SignOperation<JWK>?) {
            // No-op for Android Keystore
        }
    }
}
