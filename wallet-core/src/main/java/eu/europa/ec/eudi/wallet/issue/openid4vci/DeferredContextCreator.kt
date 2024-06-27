/*
 *  Copyright (c) 2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
@file:JvmMultifileClass

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.jwk.JWK
import eu.europa.ec.eudi.openid4vci.*
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.URL
import java.time.Clock
import java.time.Duration
import java.time.Instant

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

internal data class DeferredContextCreator(
    val issuer: Issuer,
    val authorizedRequest: AuthorizedRequest,
) {
    fun create(credential: IssuedCredential.Deferred): DeferredIssuanceContext {
        return with(issuer) {
            authorizedRequest.deferredContext(credential)
        }
    }
}

internal fun DeferredIssuanceContext.toByteArray(): ByteArray =
    json.encodeToString(DeferredIssuanceStoredContextTO.from(this, null)).toByteArray()

internal fun ByteArray.toDeferredIssuanceContext(): DeferredIssuanceContext {
    return json.decodeFromString(DeferredIssuanceStoredContextTO.serializer(), String(this))
        .toDeferredIssuanceStoredContext(Clock.systemUTC(), null)
}
//
// Serialization
//

@Serializable
private data class RefreshTokenTO(
    @Required @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long? = null,
) {

    fun toRefreshToken(): RefreshToken {
        val exp = expiresIn?.let { Duration.ofMillis(it) }
        return RefreshToken(refreshToken, exp)
    }

    companion object {

        fun from(refreshToken: RefreshToken): RefreshTokenTO =
            RefreshTokenTO(refreshToken.refreshToken, refreshToken.expiresIn?.toMillis())
    }
}

@Serializable
private enum class AccessTokenTypeTO {
    DPoP, Bearer
}

@Serializable
private data class AccessTokenTO(
    @Required @SerialName("type") val type: AccessTokenTypeTO,
    @Required @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long? = null,
) {

    fun toAccessToken(): AccessToken {
        val exp = expiresIn?.let { Duration.ofMillis(it) }
        return when (type) {
            AccessTokenTypeTO.DPoP -> AccessToken.DPoP(accessToken, exp)
            AccessTokenTypeTO.Bearer -> AccessToken.Bearer(accessToken, exp)
        }
    }

    companion object {

        fun from(accessToken: AccessToken): AccessTokenTO {
            return AccessTokenTO(
                type = when (accessToken) {
                    is AccessToken.DPoP -> AccessTokenTypeTO.DPoP
                    is AccessToken.Bearer -> AccessTokenTypeTO.Bearer
                },
                accessToken = accessToken.accessToken,
                expiresIn = accessToken.expiresIn?.toMillis(),
            )
        }
    }
}

@Serializable
private data class DeferredIssuanceStoredContextTO(
    @Required @SerialName("client_id") val clientId: String,
    @Required @SerialName("deferred_endpoint") val deferredEndpoint: String,
    @Required @SerialName("token_endpoint") val tokenEndpoint: String,
    @SerialName("dpop_key_id") val dPoPSignerKid: String? = null,
    @SerialName("credential_response_encryption_spec") val responseEncryptionSpec: JsonObject? = null,
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("access_token") val accessToken: AccessTokenTO,
    @SerialName("refresh_token") val refreshToken: RefreshTokenTO? = null,
    @SerialName("authorization_timestamp") val authorizationTimestamp: Long,
) {

    fun toDeferredIssuanceStoredContext(
        clock: Clock,
        recreatePopSigner: ((String) -> PopSigner.Jwt)?,
    ): DeferredIssuanceContext {
        return DeferredIssuanceContext(
            config = DeferredIssuerConfig(
                clock = clock,
                clientId = clientId,
                deferredEndpoint = URL(deferredEndpoint),
                tokenEndpoint = URL(tokenEndpoint),
                dPoPSigner = dPoPSignerKid?.let { requireNotNull(recreatePopSigner).invoke(it) },
                responseEncryptionSpec = responseEncryptionSpec?.let { responseEncryption(it) },
            ),
            authorizedTransaction = AuthorizedTransaction(
                authorizedRequest = AuthorizedRequest.NoProofRequired(
                    accessToken = accessToken.toAccessToken(),
                    refreshToken = refreshToken?.toRefreshToken(),
                    credentialIdentifiers = emptyMap(),
                    timestamp = Instant.ofEpochSecond(authorizationTimestamp),
                ),
                transactionId = TransactionId(transactionId),
            ),

            )
    }

    companion object {
        fun from(
            dCtx: DeferredIssuanceContext,
            dPoPSignerKid: String?,
        ): DeferredIssuanceStoredContextTO {
            val authorizedTransaction = dCtx.authorizedTransaction
            return DeferredIssuanceStoredContextTO(
                clientId = dCtx.config.clientId,
                deferredEndpoint = dCtx.config.deferredEndpoint.toString(),
                tokenEndpoint = dCtx.config.tokenEndpoint.toString(),
                dPoPSignerKid = dPoPSignerKid,
                responseEncryptionSpec = dCtx.config.responseEncryptionSpec?.let { responseEncryptionSpecTO(it) },
                transactionId = authorizedTransaction.transactionId.value,
                accessToken = AccessTokenTO.from(authorizedTransaction.authorizedRequest.accessToken),
                refreshToken = authorizedTransaction.authorizedRequest.refreshToken?.let { RefreshTokenTO.from(it) },
                authorizationTimestamp = authorizedTransaction.authorizedRequest.timestamp.epochSecond,
            )
        }

        private fun responseEncryptionSpecTO(spec: IssuanceResponseEncryptionSpec): JsonObject {
            val jwkJson = Json.parseToJsonElement(spec.jwk.toJSONString())
            return buildJsonObject {
                put("jwk", jwkJson)
                put("algorithm", spec.algorithm.toString())
                put("encryption_method", spec.encryptionMethod.toString())
            }
        }

        private fun responseEncryption(specTO: JsonObject): IssuanceResponseEncryptionSpec =
            IssuanceResponseEncryptionSpec(
                jwk = run {
                    val element = specTO["jwk"]
                    require(element is JsonObject)
                    JWK.parse(element.toString())
                },
                algorithm = run {
                    val element = specTO["algorithm"]
                    require(element is JsonPrimitive)
                    JWEAlgorithm.parse(requireNotNull(element.contentOrNull))
                },
                encryptionMethod = run {
                    val element = specTO["encryption_method"]
                    require(element is JsonPrimitive)
                    EncryptionMethod.parse(requireNotNull(element.contentOrNull))
                },
            )
    }
}