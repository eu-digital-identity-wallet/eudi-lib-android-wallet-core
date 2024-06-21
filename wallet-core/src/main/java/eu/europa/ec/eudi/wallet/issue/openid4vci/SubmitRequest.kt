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

import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.IssuanceRequestPayload
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.SubmittedRequest
import eu.europa.ec.eudi.wallet.document.UnsignedDocument

internal class SubmitRequest(
    val config: OpenId4VciManager.Config,
    val issuer: Issuer,
    var authorizedRequest: AuthorizedRequest,
) {

    val proofTypes: List<OpenId4VciManager.Config.ProofType>
        get() = config.proofTypes

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
        offeredDocument: Offer.OfferedDocument
    ): SubmittedRequest {
        var proofSigner: ProofSigner? = null
        val claimSet = null
        val payload = IssuanceRequestPayload.ConfigurationBased(offeredDocument.configurationIdentifier, claimSet)
        return try {
            with(issuer) {
                when (val rq = authorizedRequest) {
                    is AuthorizedRequest.NoProofRequired -> rq.requestSingle(payload)
                        .getOrThrow()
                        .retryIfProofRequired { submitRequest(unsignedDocument, offeredDocument) }

                    is AuthorizedRequest.ProofRequired -> {
                        proofSigner = ProofSigner(
                            unsignedDocument,
                            offeredDocument.configuration,
                            proofTypes
                        ).getOrThrow()
                        rq.requestSingle(payload, proofSigner!!.popSigner)
                            .getOrThrow()
                            .also { it.updateCNonce() }
                    }
                }
            }
        } catch (e: Throwable) {
            when (val userAuthStatus = proofSigner?.userAuthStatus) {
                is ProofSigner.UserAuthStatus.Required -> throw UserAuthRequiredException(
                    cryptoObject = userAuthStatus.cryptoObject,
                    resume = { status ->
                        if (status) {
                            submitRequest(unsignedDocument, offeredDocument)
                        } else {
                            throw IllegalStateException("User authentication failed")
                        }
                    }
                )

                else -> throw e
            }
        }
    }

    suspend fun SubmittedRequest.updateCNonce() {
        authorizedRequest = authorizedRequest.withUpdatedCNonce(this)
    }

    suspend fun AuthorizedRequest.withUpdatedCNonce(submittedRequest: SubmittedRequest): AuthorizedRequest {
        return when {
            this is AuthorizedRequest.NoProofRequired &&
                    submittedRequest is SubmittedRequest.InvalidProof -> with(issuer) {
                handleInvalidProof(submittedRequest.cNonce)
            }

            this is AuthorizedRequest.ProofRequired &&
                    submittedRequest is SubmittedRequest.Success -> {
                submittedRequest.cNonce?.let {
                    copy(cNonce = it)
                } ?: this
            }

            this is AuthorizedRequest.ProofRequired &&
                    submittedRequest is SubmittedRequest.InvalidProof -> {
                copy(cNonce = submittedRequest.cNonce)
            }

            else -> this
        }
    }

    suspend fun SubmittedRequest.retryIfProofRequired(block: suspend () -> SubmittedRequest): SubmittedRequest {
        return when {
            this is SubmittedRequest.InvalidProof && authorizedRequest is AuthorizedRequest.NoProofRequired -> {
                updateCNonce()
                block()
            }

            else -> this
        }
    }

    class Response(map: Map<UnsignedDocument, Result<SubmittedRequest>>) :
        Map<UnsignedDocument, Result<SubmittedRequest>> by map
}