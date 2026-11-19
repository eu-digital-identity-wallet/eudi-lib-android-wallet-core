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

package eu.europa.ec.eudi.wallet.issue.openid4vci.tokens

import android.annotation.SuppressLint
import eu.europa.ec.eudi.openid4vci.AccessToken
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.wallet.internal.crypto.DataSealer
import eu.europa.ec.eudi.wallet.internal.crypto.SealedDataUnreadableException
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.IssuanceMetadata
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.registration.QualifiedIdentifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.storage.NoRecordStorageException
import org.multipaz.storage.Storage
import org.multipaz.storage.StorageTable

/**
 * Reads and writes the [IssuanceMetadata] records of the issued documents in [storage].
 *
 * The access and refresh tokens of a record are stored encrypted with [sealer], in the field
 * `sealedTokens`, bound to the other fields of the record, except the ones kept for the transaction
 * log. The other fields are stored as they are, so that they can be read without the key.
 *
 * Records written before the tokens were encrypted have no `sealedTokens` field. [encryptLegacyRecords]
 * encrypts them; [get], [insert] and [storeRefreshedTokens] call it first.
 */
internal class IssuanceMetadataStore(
    private val storage: Storage,
    private val sealer: DataSealer,
    private val logger: Logger? = null,
) {

    @Volatile
    private var legacyRecordsEncrypted = false

    /**
     * Returns the record of [documentId] with its tokens, or null when there is none.
     *
     * @throws eu.europa.ec.eudi.wallet.internal.crypto.SealedDataUnreadableException when the tokens can never be decrypted
     * @throws UndecodableTokenRecordException when the record cannot be decoded
     */
    suspend fun get(documentId: String): IssuanceMetadata? {
        encryptLegacyRecords()
        return table().get(documentId)?.toIssuanceMetadata(documentId)
    }

    /** Returns the [IssuanceMetadataWithoutTokens] of the record of [documentId], or null when there is none. */
    suspend fun getWithoutTokens(documentId: String): IssuanceMetadataWithoutTokens? =
        storage.getIssuanceMetadataWithoutTokens(documentId)

    /** Stores [metadata] as the record of [documentId]. */
    suspend fun insert(documentId: String, metadata: IssuanceMetadata) {
        encryptLegacyRecords()
        table().insert(key = documentId, data = metadata.toRecord(documentId))
    }

    /**
     * Encrypts the tokens of [metadata] without storing them. It is called before a refresh, so that
     * a refresh whose tokens could not be stored is not sent.
     */
    fun checkTokensCanBeStored(documentId: String, metadata: IssuanceMetadata) {
        metadata.toRecord(documentId)
    }

    /**
     * Stores the tokens of [refreshedRequest] in every record still holding [oldRefreshToken], the
     * refresh token that was sent to obtain them.
     *
     * Documents issued under one authorization each keep a copy of its refresh token, so the token
     * also identifies the authorization. When a refresh rotates it, the old one must be discarded
     * (RFC 6749 §6) for all of these documents, not only the one being re-issued.
     *
     * Each record is updated on its own, so one that fails does not keep the others on the old
     * token. A record deleted with its document since the table was read is skipped, and so is a
     * record that cannot be decoded or whose tokens can never be decrypted. A record that cannot be
     * read for a reason that may be temporary is reported, since it may hold the old refresh token.
     *
     * @param recordReadBeforeRefresh the id and the record of the document whose tokens were refreshed,
     * as read before the refresh. This record is updated without being decrypted again, so a decryption
     * failure cannot keep it on the old refresh token.
     * @param onUpdateFailure called for each record that could not be read or updated, other than
     * deleted ones and ones that can never be read
     * @return the ids of the documents whose records were updated
     */
    suspend fun storeRefreshedTokens(
        oldRefreshToken: String,
        refreshedRequest: AuthorizedRequest,
        recordReadBeforeRefresh: Pair<String, IssuanceMetadata>? = null,
        onUpdateFailure: (documentId: String, error: Throwable) -> Unit = { _, _ -> },
    ): List<String> {
        encryptLegacyRecords()
        return recordUpdateLock.withLock {
            val table = table()
            table.enumerateWithData().mapNotNull { (documentId, bytes) ->
                val metadata = recordReadBeforeRefresh?.takeIf { it.first == documentId }?.second ?: try {
                    bytes.toIssuanceMetadata(documentId)
                } catch (e: SealedDataUnreadableException) {
                    null
                } catch (e: UndecodableTokenRecordException) {
                    null
                } catch (e: Exception) {
                    onUpdateFailure(documentId, e)
                    null
                }
                if (metadata?.refreshToken != oldRefreshToken) return@mapNotNull null
                val updated = metadata.copy(
                    accessToken = refreshedRequest.accessToken.accessToken,
                    accessTokenType = when (refreshedRequest.accessToken) {
                        is AccessToken.DPoP -> "DPoP"
                        is AccessToken.Bearer -> "Bearer"
                    },
                    // The server may answer a refresh without a new refresh token; the old one then stays valid
                    refreshToken = refreshedRequest.refreshToken?.refreshToken ?: oldRefreshToken,
                    tokenTimestamp = refreshedRequest.timestamp.epochSecond,
                )
                runCatching { table.update(key = documentId, data = updated.toRecord(documentId)) }
                    .onFailure { error -> if (error !is NoRecordStorageException) onUpdateFailure(documentId, error) }
                    .map { documentId }
                    .getOrNull()
            }
        }
    }

    /**
     * Encrypts the tokens of the records written before the tokens were encrypted. A record that
     * cannot be encrypted stays readable as it is, and the pass runs again at the next call until it
     * completes without a failure. A record that cannot be decoded is left as it is and does not make
     * the pass run again. Failures are logged, not thrown.
     */
    suspend fun encryptLegacyRecords() {
        if (legacyRecordsEncrypted) return
        recordUpdateLock.withLock {
            if (legacyRecordsEncrypted) return
            val (table, records) = try {
                table().let { it to it.enumerateWithData() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger?.e(TAG, "Failed to read the issuance metadata to encrypt their tokens", e)
                return
            }
            var failed = false
            records.forEach { (documentId, bytes) ->
                val metadata = runCatching {
                    val fields = bytes.toJsonObject(documentId)
                    if (SEALED_TOKENS_FIELD in fields) return@forEach
                    decodeTokenRecord(documentId) {
                        tokenRecordJson.decodeFromJsonElement(IssuanceMetadata.serializer(), fields)
                    }
                }.onFailure { error ->
                    logger?.e(TAG, "The issuance metadata of document $documentId cannot be decoded and are not encrypted", error)
                }.getOrNull() ?: return@forEach
                runCatching {
                    table.update(key = documentId, data = metadata.toRecord(documentId))
                }.onFailure { error ->
                    if (error !is NoRecordStorageException) {
                        failed = true
                        logger?.e(TAG, "Failed to encrypt the stored tokens of document $documentId", error)
                    }
                }
            }
            legacyRecordsEncrypted = !failed
        }
    }

    private suspend fun table(): StorageTable = storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)

    private fun IssuanceMetadata.toRecord(documentId: String): ByteString {
        val fields = tokenRecordJson.encodeToJsonElement(IssuanceMetadata.serializer(), this).jsonObject
        val tokens = JsonObject(fields.filterKeys { it in TOKEN_FIELDS })
        val sealedTokens = sealer.seal(
            TokenRecord.ISSUANCE_METADATA,
            documentId,
            boundValues(fields, UNBOUND_FIELDS),
            tokenRecordJson.encodeToString(JsonObject.serializer(), tokens).toByteArray(Charsets.UTF_8)
        )
        val record = JsonObject(
            fields.filterKeys { it !in TOKEN_FIELDS } + (SEALED_TOKENS_FIELD to JsonPrimitive(sealedTokens))
        )
        return ByteString(tokenRecordJson.encodeToString(JsonObject.serializer(), record).toByteArray(Charsets.UTF_8))
    }

    private fun ByteString.toIssuanceMetadata(documentId: String): IssuanceMetadata {
        val fields = toJsonObject(documentId)
        val sealedTokens = fields[SEALED_TOKENS_FIELD]
            // A record written before the tokens were encrypted
            ?: return decodeTokenRecord(documentId) {
                tokenRecordJson.decodeFromJsonElement(IssuanceMetadata.serializer(), fields)
            }
        if (fields.keys.any { it in TOKEN_FIELDS }) {
            throw UndecodableTokenRecordException("The record of document $documentId has both encrypted and plain tokens")
        }
        val (boundFields, sealed) = decodeTokenRecord(documentId) {
            boundValues(fields, UNBOUND_FIELDS) to sealedTokens.jsonPrimitive.content
        }
        val tokens = sealer.open(TokenRecord.ISSUANCE_METADATA, documentId, boundFields, sealed)
            .toString(Charsets.UTF_8)
            .let { decodeTokenRecord(documentId) { tokenRecordJson.parseToJsonElement(it).jsonObject } }
        if (!tokens.keys.all { it in TOKEN_FIELDS }) {
            throw UndecodableTokenRecordException("The encrypted tokens of document $documentId have unexpected fields")
        }
        return decodeTokenRecord(documentId) {
            tokenRecordJson.decodeFromJsonElement(
                IssuanceMetadata.serializer(),
                JsonObject(fields - SEALED_TOKENS_FIELD + tokens)
            )
        }
    }

    companion object {
        private const val SEALED_TOKENS_FIELD = "sealedTokens"

        /** The fields of a record that are stored encrypted. */
        val TOKEN_FIELDS = setOf("accessToken", "refreshToken")

        /**
         * The fields to which the encrypted tokens are not bound: the tokens, and the ones kept for the
         * transaction log. The tokens are bound to every other field of a record.
         */
        val UNBOUND_FIELDS = TOKEN_FIELDS + SEALED_TOKENS_FIELD + setOf("issuerIdentifier", "issuerName")

        /**
         * Serializes the updates of existing records, so that encrypting a record written before
         * the tokens were encrypted never overwrites tokens refreshed meanwhile.
         */
        private val recordUpdateLock = Mutex()
    }
}

/**
 * The fields of an [IssuanceMetadata] record that are needed without its tokens.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
internal data class IssuanceMetadataWithoutTokens(
    val credentialIssuerId: String,
    val credentialConfigurationIdentifier: String,
    val dPoPKeyAlias: String? = null,
    val clientAttestationPopKeyId: String? = null,
    val authorizationServerId: String? = null,
    val issuerIdentifier: QualifiedIdentifier? = null,
    val issuerName: String? = null,
)

/**
 * Reads the [IssuanceMetadataWithoutTokens] of the record of [documentId], or null when there is
 * none. It does not need the token encryption key.
 */
internal suspend fun Storage.getIssuanceMetadataWithoutTokens(documentId: String): IssuanceMetadataWithoutTokens? =
    getTable(IssuanceMetadata.STORAGE_TABLE_SPEC).get(documentId)?.let { bytes ->
        decodeTokenRecord(documentId) {
            lenientJson.decodeFromString(IssuanceMetadataWithoutTokens.serializer(), bytes.toByteArray().toString(Charsets.UTF_8))
        }
    }

private val lenientJson = Json(tokenRecordJson) { ignoreUnknownKeys = true }

private fun ByteString.toJsonObject(documentId: String): JsonObject = decodeTokenRecord(documentId) {
    tokenRecordJson.parseToJsonElement(toByteArray().toString(Charsets.UTF_8)).jsonObject
}
