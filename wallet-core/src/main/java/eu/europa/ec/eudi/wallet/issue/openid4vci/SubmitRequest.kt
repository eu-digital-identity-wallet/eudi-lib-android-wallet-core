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
import eu.europa.ec.eudi.openid4vci.ProofsSpecification
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import kotlinx.coroutines.runBlocking
import org.multipaz.securearea.KeyUnlockData

internal class SubmitRequest(
    val config: OpenId4VciManager.Config,
    val issuer: Issuer,
    authorizedRequest: AuthorizedRequest,
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

        lateinit var proofSigner: BatchProofSigner
        return try {
            val payload = IssuanceRequestPayload.ConfigurationBased(
                offeredDocument.configurationIdentifier
            )
            proofSigner = BatchProofSigner(unsignedDocument.getPoPSigners().toList(), keyUnlockData)
            val proofSpec = ProofsSpecification.JwtProofs.NoKeyAttestation(
                proofsSigner = proofSigner
            )
            val (updatedAuthorizedRequest, outcome) = with(issuer) {
                authorizedRequest.request(payload, proofSpec)
            }.getOrThrow()

            this.authorizedRequest = updatedAuthorizedRequest
            proofSigner.signers.map { it.keyAlias } to outcome
        } catch (e: Throwable) {

            val isUserAuthRequired = proofSigner.keyLockedException != null
            if (isUserAuthRequired) {
                val keysAndSecureAreas = proofSigner.signers
                    .associate { it.keyAlias to it.secureArea }
                throw UserAuthRequiredException(
                    signingAlgorithm = proofSigner.algorithm,
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