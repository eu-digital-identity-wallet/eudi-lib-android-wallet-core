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
import eu.europa.ec.eudi.openid4vci.HasKeyAttestationRequirement
import eu.europa.ec.eudi.openid4vci.IssuanceRequestPayload
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.KeyAttestationJWT
import eu.europa.ec.eudi.openid4vci.KeyAttestationRequirement
import eu.europa.ec.eudi.openid4vci.ProofType
import eu.europa.ec.eudi.openid4vci.ProofsSpecification
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigner
import eu.europa.ec.eudi.wallet.provider.WalletAttestationsProvider
import org.multipaz.securearea.KeyUnlockData

internal class SubmitRequest(
    val config: OpenId4VciManager.Config,
    val walletAttestationsProvider: WalletAttestationsProvider?,
    val issuer: Issuer,
    authorizedRequest: AuthorizedRequest,
) {
    var authorizedRequest: AuthorizedRequest = authorizedRequest
        private set

    suspend fun request(offeredDocuments: Map<UnsignedDocument, Offer.OfferedDocument>): Response {
        return Response(offeredDocuments.mapValues { (unsignedDocument, offeredDocument) ->
            try {
                val (keyAliases, outcome) = submitRequest(unsignedDocument, offeredDocument)
                ResponseResult(
                    keyAliases = keyAliases,
                    outcome = Result.success(outcome)
                )
            } catch (e: Throwable) {
                ResponseResult(emptyList(), Result.failure(e))
            }
        })
    }

    private fun Offer.OfferedDocument.supportsAttestationProofType(): Boolean {
        return this.configuration.proofTypesSupported[ProofType.ATTESTATION] != null
    }

    private fun Offer.OfferedDocument.supportsJwtProofTypeWithAttestation(): Boolean {
        val meta = configuration.proofTypesSupported[ProofType.JWT]
        val requirement = (meta as? HasKeyAttestationRequirement)?.keyAttestationRequirement
        return requirement is KeyAttestationRequirement.Required

    }

    private fun Offer.OfferedDocument.supportsJwtProofTypeWithoutAttestation(): Boolean {
        val meta = configuration.proofTypesSupported[ProofType.JWT]
        val requirement = (meta as? HasKeyAttestationRequirement)?.keyAttestationRequirement
        return requirement == KeyAttestationRequirement.NotRequired
    }

    private suspend fun submitRequest(
        unsignedDocument: UnsignedDocument,
        offeredDocument: Offer.OfferedDocument,
        keyUnlockData: Map<KeyAlias, KeyUnlockData?>? = null,
    ): ResponseResult<SubmissionOutcome> {
        val payload = IssuanceRequestPayload.ConfigurationBased(
            offeredDocument.configurationIdentifier
        )
        val (updatedAuthorizedRequest, outcome) = when {
            walletAttestationsProvider != null && offeredDocument.supportsAttestationProofType() -> {
                val proofsSpecification = ProofsSpecification.AttestationProof { nonce ->
                    walletAttestationsProvider.getKeyAttestation(
                        unsignedDocument.getPoPSigners().toList().map { it.getKeyInfo() },
                        nonce
                    )
                        .map { KeyAttestationJWT(it) }
                        .getOrThrow()
                }
                authorizedRequest.requestWithAttestationProof(payload, proofsSpecification)
            }


            walletAttestationsProvider != null && offeredDocument.supportsJwtProofTypeWithAttestation() -> {
                throw NotImplementedError("JWT proof with attestation is not yet supported in wallet")
            }

            offeredDocument.supportsJwtProofTypeWithoutAttestation() -> {
                authorizedRequest.requestWithJwtProofWithoutAttestation(
                    payload,
                    unsignedDocument.getPoPSigners().toList(),
                    keyUnlockData,
                    unlockResume = { updatedKeyUnlockData ->
                        submitRequest(unsignedDocument, offeredDocument, updatedKeyUnlockData)
                    }
                )
            }

            else -> {
                error("No supported proof type found in the credential configuration")
            }
        }

        this.authorizedRequest = updatedAuthorizedRequest
        return ResponseResult(
            keyAliases = unsignedDocument.getPoPSigners().toList().map { it.keyAlias },
            outcome = outcome
        )
    }

    class Response(map: Map<UnsignedDocument, ResponseResult<Result<SubmissionOutcome>>>) :
        Map<UnsignedDocument, ResponseResult<Result<SubmissionOutcome>>> by map

    data class ResponseResult<T>(
        val keyAliases: List<String>,
        val outcome: T,
    )

    private suspend fun AuthorizedRequest.requestWithAttestationProof(
        payload: IssuanceRequestPayload,
        proofsSpecification: ProofsSpecification.AttestationProof,
    ): Pair<AuthorizedRequest, SubmissionOutcome> {
        return with(issuer) {
            request(payload, proofsSpecification)
        }.getOrThrow()
    }

    private suspend fun AuthorizedRequest.requestWithJwtProofWithoutAttestation(
        payload: IssuanceRequestPayload,
        signers: List<ProofOfPossessionSigner>,
        keyUnlockData: Map<String, KeyUnlockData?>?,
        unlockResume: suspend (Map<String, KeyUnlockData?>) -> SubmitRequest.ResponseResult<SubmissionOutcome>,
    ): Pair<AuthorizedRequest, SubmissionOutcome> {
        var proofSigner: BatchProofSigner? = null
        try {
            proofSigner =
                BatchProofSigner(signers, keyUnlockData)
            val proofsSpecification = ProofsSpecification.JwtProofs.NoKeyAttestation(proofSigner)
            return with(issuer) {
                request(payload, proofsSpecification)
            }.getOrThrow()
        } catch (e: Throwable) {

            val isUserAuthRequired = proofSigner?.keyLockedException != null
            if (isUserAuthRequired) {
                val keysAndSecureAreas = proofSigner.signers
                    .associate { it.keyAlias to it.secureArea }
                throw UserAuthRequiredException(
                    signingAlgorithm = proofSigner.algorithm,
                    keysAndSecureAreas = keysAndSecureAreas,
                    resume = { keyUnlockData -> unlockResume(keyUnlockData) },
                    cause = e
                )
            } else {
                throw e
            }
        }
    }

}