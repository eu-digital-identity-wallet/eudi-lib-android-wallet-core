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

import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.IssuanceRequestPayload
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import kotlinx.coroutines.runBlocking
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.KeyUnlockData

internal class SubmitRequest(
    val config: OpenId4VciManager.Config,
    val issuer: Issuer,
    authorizedRequest: AuthorizedRequest,
    val algorithm: Algorithm = Algorithm.ESP256
) {
    var authorizedRequest: AuthorizedRequest = authorizedRequest
        private set

    suspend fun request(offeredDocuments: Map<UnsignedDocument, Offer.OfferedDocument>): Response {
        return Response(offeredDocuments.mapValues { (unsignedDocument, offeredDocument) ->
            try {
                val (keyAliases, outcome) = submitRequest(unsignedDocument, offeredDocument)
                keyAliases to Result.success(outcome)
            } catch (e: Throwable) {
                emptyList<String>() to Result.failure(e)
            }
        })
    }

    private suspend fun submitRequest(
        unsignedDocument: UnsignedDocument,
        offeredDocument: Offer.OfferedDocument,
        keyUnlockData: Map<KeyAlias, KeyUnlockData?>? = null,
    ): Pair<List<String>, SubmissionOutcome> {

        var proofSigners: List<JWSKeyPoPSigner>? = null
        return try {
            val payload = IssuanceRequestPayload.ConfigurationBased(
                offeredDocument.configurationIdentifier
            )

            proofSigners =
                unsignedDocument.getPoPSigners().map { s ->
                    JWSKeyPoPSigner(s, keyUnlockData?.get(s.keyAlias))
                }
            val (updatedAuthorizedRequest, outcome) = with(issuer) {
                authorizedRequest.request(payload, proofSigners.map { it.popSigner })
            }.getOrThrow()

            this.authorizedRequest = updatedAuthorizedRequest
            proofSigners.map { it.proofSigner.keyAlias } to outcome
        } catch (e: Throwable) {

            val isUserAuthRequired = proofSigners?.any { it.keyLockedException != null } == true
            if (isUserAuthRequired) {
                val keysAndSecureAreas = proofSigners
                    .map { it.proofSigner }
                    .associate { it.keyAlias to it.secureArea }
                throw UserAuthRequiredException(
                    signingAlgorithm = algorithm,
                    keysAndSecureAreas = keysAndSecureAreas,
                    resume = { keyUnlockData ->
                        runBlocking {
                            submitRequest(
                                unsignedDocument,
                                offeredDocument,
                                keyUnlockData
                            )
                        }
                    },
                    cause = e
                )
            } else {
                throw e
            }
        }
    }

    class Response(map: Map<UnsignedDocument, Pair<List<String>, Result<SubmissionOutcome>>>) :
        Map<UnsignedDocument, Pair<List<String>, Result<SubmissionOutcome>>> by map
}