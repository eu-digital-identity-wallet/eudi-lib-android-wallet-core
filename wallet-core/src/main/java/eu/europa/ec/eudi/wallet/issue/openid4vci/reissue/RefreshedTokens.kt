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

import eu.europa.ec.eudi.openid4vci.AccessToken
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.io.bytestring.ByteString
import org.multipaz.storage.Storage

/**
 * Serializes loading, refreshing and storing the tokens of a re-issuance, so two re-issuances
 * never send the same refresh token. It is process-wide because a manager may be created per issuer.
 */
internal val tokenRefreshLock = Mutex()

/**
 * Stores the tokens of [refreshedRequest] in every [IssuanceMetadata] record still holding
 * [oldRefreshToken], the refresh token that was sent to obtain them.
 *
 * Documents issued under one authorization each keep a copy of its refresh token, so the token
 * also identifies the authorization. When a refresh rotates it, the old one must be discarded
 * (RFC 6749 §6) for all of these documents, not only the one being re-issued.
 *
 * @return the ids of the documents whose records were updated
 */
internal suspend fun Storage.storeRefreshedTokens(
    oldRefreshToken: String,
    refreshedRequest: AuthorizedRequest,
): List<String> {
    val table = getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
    return table.enumerateWithData().mapNotNull { (documentId, bytes) ->
        val metadata = runCatching { IssuanceMetadata.fromByteArray(bytes.toByteArray()) }.getOrNull()
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
        table.update(key = documentId, data = ByteString(updated.toByteArray()))
        documentId
    }
}
