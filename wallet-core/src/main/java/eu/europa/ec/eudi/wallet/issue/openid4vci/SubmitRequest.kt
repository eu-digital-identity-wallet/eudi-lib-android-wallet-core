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
import eu.europa.ec.eudi.openid4vci.KeyAttestationJWT
import eu.europa.ec.eudi.openid4vci.ProofSpecification
import eu.europa.ec.eudi.openid4vci.ProofType
import eu.europa.ec.eudi.openid4vci.ProofTypeMeta
import eu.europa.ec.eudi.openid4vci.ProofTypesSupported
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigner
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.IssuanceProofProfile
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.IssuanceProofProfile.ProofType as IssuanceProofType
import eu.europa.ec.eudi.wallet.provider.WalletKeyAttestationProvider
import org.multipaz.securearea.KeyUnlockData

internal class SubmitRequest(
    val walletKeyAttestationProvider: WalletKeyAttestationProvider?,
    val issuer: Issuer,
    authorizedRequest: AuthorizedRequest,
    val issuanceProofProfile: IssuanceProofProfile,
) {
    var authorizedRequest: AuthorizedRequest = authorizedRequest
        private set

    suspend fun request(
        requestMap: Map<UnsignedDocument, Pair<Offer.OfferedDocument, IssuanceRequestPayload>>,
    ): Response {
        return Response(requestMap.mapValues { (unsignedDocument, entry) ->
            try {
                val (keyAliases, outcome) = submitRequest(unsignedDocument, entry.first, entry.second)
                ResponseResult(
                    keyAliases = keyAliases,
                    outcome = Result.success(outcome)
                )
            } catch (e: Throwable) {
                ResponseResult(emptyList(), Result.failure(e))
            }
        })
    }

    private suspend fun submitRequest(
        unsignedDocument: UnsignedDocument,
        offeredDocument: Offer.OfferedDocument,
        payload: IssuanceRequestPayload,
        keyUnlockData: Map<KeyAlias, KeyUnlockData?>? = null,
    ): ResponseResult<SubmissionOutcome> {
        val signers = unsignedDocument.getPoPSigners()
        val proofTypesSupported = offeredDocument.configuration.proofTypesSupported

        val (updatedAuthorizedRequest, outcome) = negotiateAndRequest(
            proofTypesSupported, payload, signers, keyUnlockData,
            unlockResume = { updatedKeyUnlockData ->
                submitRequest(unsignedDocument, offeredDocument, payload, updatedKeyUnlockData)
            }
        )

        this.authorizedRequest = updatedAuthorizedRequest
        return ResponseResult(
            keyAliases = signers.map { it.keyAlias },
            outcome = outcome
        )
    }

    private suspend fun negotiateAndRequest(
        proofTypesSupported: ProofTypesSupported,
        payload: IssuanceRequestPayload,
        signers: List<ProofOfPossessionSigner>,
        keyUnlockData: Map<KeyAlias, KeyUnlockData?>?,
        unlockResume: suspend (Map<String, KeyUnlockData?>) -> ResponseResult<SubmissionOutcome>,
    ): Pair<AuthorizedRequest, SubmissionOutcome> {
        if (proofTypesSupported == ProofTypesSupported.Empty) {
            return with(issuer) {
                authorizedRequest.request(payload, ProofSpecification.NoProof)
            }.getOrThrow()
        }

        val negotiated = issuanceProofProfile.preferenceOrder
            .firstOrNull { matchesIssuerCapabilities(it.proofType, proofTypesSupported) }
            ?: error(
                "No compatible proof type found between wallet profile " +
                    "($issuanceProofProfile) and issuer capabilities ($proofTypesSupported)"
            )

        return when (negotiated.proofType) {
            IssuanceProofType.ATTESTATION ->
                requestWithAttestationProof(payload, signers)

            IssuanceProofType.JWT_WITH_KEY_ATTESTATION ->
                authorizedRequest.requestWithJwtProofWithKeyAttestation(
                    payload, signers, keyUnlockData, unlockResume,
                )

            IssuanceProofType.JWT_WITHOUT_KEY_ATTESTATION ->
                authorizedRequest.requestWithJwtProofsWithoutKeyAttestation(
                    payload, signers, keyUnlockData, unlockResume,
                )

            IssuanceProofType.NO_PROOF ->
                with(issuer) {
                    authorizedRequest.request(payload, ProofSpecification.NoProof)
                }.getOrThrow()
        }
    }

    private fun matchesIssuerCapabilities(
        proofType: IssuanceProofType,
        proofTypesSupported: ProofTypesSupported,
    ): Boolean {
        val jwtMeta = proofTypesSupported[ProofType.JWT] as? ProofTypeMeta.Jwt
        return when (proofType) {
            IssuanceProofType.ATTESTATION ->
                proofTypesSupported[ProofType.ATTESTATION] != null && walletKeyAttestationProvider != null

            IssuanceProofType.JWT_WITH_KEY_ATTESTATION ->
                jwtMeta?.keyAttestationRequirement != null && walletKeyAttestationProvider != null

            IssuanceProofType.JWT_WITHOUT_KEY_ATTESTATION ->
                jwtMeta != null && jwtMeta.keyAttestationRequirement == null

            IssuanceProofType.NO_PROOF -> true
        }
    }

    private suspend fun requestWithAttestationProof(
        payload: IssuanceRequestPayload,
        signers: List<ProofOfPossessionSigner>,
    ): Pair<AuthorizedRequest, SubmissionOutcome> {
        val walletKeyAttestationProvider = checkNotNull(walletKeyAttestationProvider) {
            "WalletKeyAttestationProvider is required for attestation proof"
        }
        val proofsSpecification = ProofSpecification.AttestationProof { nonce, _ ->
            walletKeyAttestationProvider.getKeyAttestation(
                signers.map { it.getKeyInfo() },
                nonce
            )
                .map { KeyAttestationJWT(it) }
                .getOrThrow()
        }
        return authorizedRequest.requestWithAttestationProof(payload, proofsSpecification)
    }

    class Response(map: Map<UnsignedDocument, ResponseResult<Result<SubmissionOutcome>>>) :
        Map<UnsignedDocument, ResponseResult<Result<SubmissionOutcome>>> by map

    data class ResponseResult<T>(
        val keyAliases: List<String>,
        val outcome: T,
    )

    private suspend fun AuthorizedRequest.requestWithAttestationProof(
        payload: IssuanceRequestPayload,
        proofsSpecification: ProofSpecification.AttestationProof,
    ): Pair<AuthorizedRequest, SubmissionOutcome> {
        return with(issuer) {
            request(payload, proofsSpecification)
        }.getOrThrow()
    }

    private suspend fun AuthorizedRequest.requestWithJwtProofWithKeyAttestation(
        payload: IssuanceRequestPayload,
        signers: List<ProofOfPossessionSigner>,
        keyUnlockData: Map<String, KeyUnlockData?>?,
        unlockResume: suspend (Map<String, KeyUnlockData?>) -> ResponseResult<SubmissionOutcome>,
    ): Pair<AuthorizedRequest, SubmissionOutcome> {
        val walletKeyAttestationProvider = checkNotNull(walletKeyAttestationProvider) {
            "WalletKeyAttestationProvider is required for JWT proof with key attestation"
        }
        var proofSigner: KeyAttestationSigner? = null
        val proofsSpecification = ProofSpecification.JwtProofWithKeyAttestation(
            proofSignerProvider = { nonce, _ ->
                val factory = KeyAttestationSigner.Factory(
                    signers, walletKeyAttestationProvider, keyUnlockData
                )
                factory(nonce).getOrThrow().also { proofSigner = it }
            }
        )
        try {
            return with(issuer) { request(payload, proofsSpecification) }.getOrThrow()
        } catch (e: Throwable) {

            val isUserAuthRequired = proofSigner?.keyLockedException != null
            if (isUserAuthRequired) {
                val keysAndSecureAreas = mapOf(
                    proofSigner.signer.let { it.keyAlias to it.secureArea }
                )
                throw UserAuthRequiredException(
                    signingAlgorithm = proofSigner.signer.getKeyInfo().algorithm,
                    keysAndSecureAreas = keysAndSecureAreas,
                    resume = { keyUnlockData ->
                        unlockResume(keyUnlockData)
                    },
                    cause = e
                )
            } else {
                throw e
            }
        }

    }

    private suspend fun AuthorizedRequest.requestWithJwtProofsWithoutKeyAttestation(
        payload: IssuanceRequestPayload,
        signers: List<ProofOfPossessionSigner>,
        keyUnlockData: Map<String, KeyUnlockData?>?,
        unlockResume: suspend (Map<String, KeyUnlockData?>) -> ResponseResult<SubmissionOutcome>,
    ): Pair<AuthorizedRequest, SubmissionOutcome> {
        val firstSigner = signers[0]
        val javaAlgorithm = requireNotNull(firstSigner.getKeyInfo().algorithm.javaAlgorithm) {
            "No JCA algorithm name for ${firstSigner.getKeyInfo().algorithm.name}"
        }

        val batchSigner = JwtProofBatchSigner(javaAlgorithm, signers, keyUnlockData)
        val proofSpec = ProofSpecification.JwtProofsWithoutKeyAttestation(batchSigner)

        try {
            return with(issuer) { request(payload, proofSpec) }.getOrThrow()
        } catch (e: Throwable) {
            val isUserAuthRequired = batchSigner.keyLockedException != null
            if (isUserAuthRequired) {
                val keysAndSecureAreas = signers.associate { it.keyAlias to it.secureArea }
                throw UserAuthRequiredException(
                    signingAlgorithm = firstSigner.getKeyInfo().algorithm,
                    keysAndSecureAreas = keysAndSecureAreas,
                    resume = { updatedKeyUnlockData ->
                        unlockResume(updatedKeyUnlockData)
                    },
                    cause = e
                )
            } else {
                throw e
            }
        }
    }

}
