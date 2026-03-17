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
import eu.europa.ec.eudi.openid4vci.Grant
import eu.europa.ec.eudi.openid4vci.RefreshToken
import java.time.Instant

/**
 * Helper for credential re-issuance.
 *
 * This class provides utility functions to restore runtime objects from stored [IssuanceMetadata].
 * The actual re-issuance flow is handled by `DefaultOpenId4VciManager.reissueDocument()`.
 *
 * @see IssuanceMetadata
 */
internal class ReissuanceIssuer {

    /**
     * Reconstructs an [AuthorizedRequest] from stored [IssuanceMetadata].
     *
     * Following the pattern from Extensions.kt's `restore()` function for deferred issuance.
     * This recreates the runtime [AuthorizedRequest] object from serialized data,
     * including the refresh token which enables the openid4vci library to transparently
     * refresh the access token if it has expired.
     *
     * Note: credentialIdentifiers and DPoP nonces are initialized as null/empty,
     * as they will be updated during the re-issuance flow by the library.
     *
     * @param config The stored credential metadata
     * @return The reconstructed [AuthorizedRequest]
     */
    fun reconstructAuthorizedRequest(config: IssuanceMetadata): AuthorizedRequest {
        val accessToken = when (config.accessTokenType) {
            "DPoP" -> AccessToken.DPoP(config.accessToken, expiresIn = null)
            "Bearer" -> AccessToken.Bearer(config.accessToken, expiresIn = null)
            else -> error("Unknown access token type: ${config.accessTokenType}")
        }

        val refreshToken = config.refreshToken?.let { RefreshToken(it) }

        return AuthorizedRequest(
            accessToken = accessToken,
            refreshToken = refreshToken,
            credentialIdentifiers = emptyMap(),
            timestamp = Instant.ofEpochSecond(config.tokenTimestamp),
            authorizationServerDpopNonce = null,
            resourceServerDpopNonce = null,
            grant = Grant.AuthorizationCode,
        )
    }
}
