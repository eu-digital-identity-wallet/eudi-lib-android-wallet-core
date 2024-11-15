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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.android.identity.crypto.Algorithm
import com.android.identity.securearea.KeyUnlockData
import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.IssuanceRequestPayload
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument

internal class SubmitRequest(
    val config: OpenId4VciManager.Config,
    val issuer: Issuer,
    authorizedRequest: AuthorizedRequest,
    val algorithm: Algorithm = Algorithm.ES256,
) {
    var authorizedRequest: AuthorizedRequest = authorizedRequest
        private set


    suspend fun request(offeredDocuments: Map<UnsignedDocument, Offer.OfferedDocument>): Response {
        return Response(offeredDocuments.mapValues { (unsignedDocument, offeredDocument) ->
            try {
                Result.success(submitRequest(unsignedDocument, offeredDocument))
            } catch (e: Throwable) {
                Result.failure(e)
            }
        })
    }

    private suspend fun submitRequest(
        unsignedDocument: UnsignedDocument,
        offeredDocument: Offer.OfferedDocument,
        keyUnlockData: KeyUnlockData? = null,
    ): SubmissionOutcome {
        offeredDocument as DefaultOfferedDocument
        var proofSigner: JWSKeyPoPSigner? = null
        return try {
            val claimSet = null
            val payload = IssuanceRequestPayload.ConfigurationBased(
                offeredDocument.configurationIdentifier,
                claimSet
            )
            proofSigner = JWSKeyPoPSigner(
                document = unsignedDocument,
                algorithm = algorithm,
                keyUnlockData = keyUnlockData
            )
            val (updatedAuthorizedRequest, outcome) = with(issuer) {
                authorizedRequest.request(payload, listOf(proofSigner.popSigner))
            }.getOrThrow()

            this.authorizedRequest = updatedAuthorizedRequest
            outcome
        } catch (e: Throwable) {
            if (null !== proofSigner && null != proofSigner.keyLockedException) {
                throw UserAuthRequiredException(
                    signingAlgorithm = algorithm,
                    resume = { keyUnlockData ->
                        submitRequest(
                            unsignedDocument,
                            offeredDocument,
                            keyUnlockData
                        )
                    },
                    cause = e
                )
            } else throw e
        }
    }

    class Response(map: Map<UnsignedDocument, Result<SubmissionOutcome>>) :
        Map<UnsignedDocument, Result<SubmissionOutcome>> by map
}