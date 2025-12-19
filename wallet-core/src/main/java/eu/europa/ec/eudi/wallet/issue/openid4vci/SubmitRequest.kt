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
import eu.europa.ec.eudi.openid4vci.KeyAttestationRequirement
import eu.europa.ec.eudi.openid4vci.ProofTypeMeta
import eu.europa.ec.eudi.openid4vci.ProofsSpecification
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigner
import eu.europa.ec.eudi.wallet.provider.WalletAttestationsProvider
import kotlinx.coroutines.runBlocking
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

    private suspend fun submitRequest(
        unsignedDocument: UnsignedDocument,
        offeredDocument: Offer.OfferedDocument,
        keyUnlockData: Map<KeyAlias, KeyUnlockData?>? = null,
    ): ResponseResult<SubmissionOutcome> {
        val payload =
            IssuanceRequestPayload.ConfigurationBased(offeredDocument.configurationIdentifier)
        val signers = unsignedDocument.getPoPSigners().toList()

        val (updatedAuthorizedRequest, outcome) = when (config.clientAuthenticationType) {
            is OpenId4VciManager.ClientAuthenticationType.None -> {
                requestWithNoAuthentication(
                    payload,
                    signers,
                    unsignedDocument,
                    offeredDocument,
                    keyUnlockData
                )
            }

            is OpenId4VciManager.ClientAuthenticationType.AttestationBased -> {
                requestWithAttestationBasedAuth(
                    payload,
                    signers,
                    unsignedDocument,
                    offeredDocument,
                    keyUnlockData
                )
            }
        }

        this.authorizedRequest = updatedAuthorizedRequest
        return ResponseResult(
            keyAliases = signers.map { it.keyAlias },
            outcome = outcome
        )
    }

    private suspend fun requestWithNoAuthentication(
        payload: IssuanceRequestPayload,
        signers: List<ProofOfPossessionSigner>,
        unsignedDocument: UnsignedDocument,
        offeredDocument: Offer.OfferedDocument,
        keyUnlockData: Map<KeyAlias, KeyUnlockData?>?,
    ): Pair<AuthorizedRequest, SubmissionOutcome> {
        val canBeIssuedWithoutAttestation = offeredDocument.configuration.proofTypesSupported.values
            .filterIsInstance<ProofTypeMeta.Jwt>()
            .any { it.keyAttestationRequirement is KeyAttestationRequirement.NotRequired }

        check(canBeIssuedWithoutAttestation) {
            "Offered document requires attestation proof, but client authentication type is None"
        }

        return authorizedRequest.requestWithJwtProofWithoutAttestation(
            payload, signers, keyUnlockData,
            unlockResume = { updatedKeyUnlockData ->
                submitRequest(unsignedDocument, offeredDocument, updatedKeyUnlockData)
            }
        )
    }

    private suspend fun requestWithAttestationBasedAuth(
        payload: IssuanceRequestPayload,
        signers: List<ProofOfPossessionSigner>,
        unsignedDocument: UnsignedDocument,
        offeredDocument: Offer.OfferedDocument,
        keyUnlockData: Map<KeyAlias, KeyUnlockData?>?,
    ): Pair<AuthorizedRequest, SubmissionOutcome> {

        val proofType =
            offeredDocument.configuration.proofTypesSupported.values.filterIsInstance<ProofTypeMeta.Attestation>()
                .firstOrNull()
        if (proofType != null) {
            return requestWithAttestationProof(payload, signers)
        }

        val jwtProofTypes =
            offeredDocument.configuration.proofTypesSupported.values.filterIsInstance<ProofTypeMeta.Jwt>()

        val jwtWithAttest =
            jwtProofTypes.firstOrNull() { it.keyAttestationRequirement is KeyAttestationRequirement.Required }
        if (jwtWithAttest != null) {
            return authorizedRequest.requestWithJwtProofWithAttestation(
                payload, signers, keyUnlockData,
                unlockResume = { updatedKeyUnlockData ->
                    submitRequest(unsignedDocument, offeredDocument, updatedKeyUnlockData)
                }
            )
        }

        val jwtWithoutAttest =
            jwtProofTypes.firstOrNull { it.keyAttestationRequirement is KeyAttestationRequirement.NotRequired }
        if (jwtWithoutAttest != null) {
            return authorizedRequest.requestWithJwtProofWithoutAttestation(
                payload, signers, keyUnlockData,
                unlockResume = { updatedKeyUnlockData ->
                    submitRequest(unsignedDocument, offeredDocument, updatedKeyUnlockData)
                }
            )
        }

        throw IllegalStateException("No supported proof type found in the credential configuration")
    }

    private suspend fun requestWithAttestationProof(
        payload: IssuanceRequestPayload,
        signers: List<ProofOfPossessionSigner>,
    ): Pair<AuthorizedRequest, SubmissionOutcome> {
        val walletAttestationsProvider = checkNotNull(walletAttestationsProvider) {
            "WalletAttestationsProvider is required for attestation based client authentication"
        }
        val proofsSpecification = ProofsSpecification.AttestationProof { nonce ->
            walletAttestationsProvider.getKeyAttestation(
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
        proofsSpecification: ProofsSpecification.AttestationProof,
    ): Pair<AuthorizedRequest, SubmissionOutcome> {
        return with(issuer) {
            request(payload, proofsSpecification)
        }.getOrThrow()
    }

    private suspend fun AuthorizedRequest.requestWithJwtProofWithAttestation(
        payload: IssuanceRequestPayload,
        signers: List<ProofOfPossessionSigner>,
        keyUnlockData: Map<String, KeyUnlockData?>?,
        unlockResume: suspend (Map<String, KeyUnlockData?>) -> ResponseResult<SubmissionOutcome>,
    ): Pair<AuthorizedRequest, SubmissionOutcome> {
        val walletAttestationsProvider = checkNotNull(walletAttestationsProvider) {
            "WalletAttestationsProvider is required for attestation based client authentication"
        }
        var proofSigner: KeyAttestationSigner? = null
        val keyIndex = 0
        val proofsSpecification = ProofsSpecification.JwtProofs.WithKeyAttestation(
            proofSignerProvider = { nonce ->
                val factory = KeyAttestationSigner.Factory(
                    signers, keyIndex, walletAttestationsProvider, keyUnlockData
                )
                factory(nonce).getOrThrow().also { proofSigner = it }
            },
            keyIndex = keyIndex,
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
                        runBlocking {
                            unlockResume(keyUnlockData)
                        }
                    },
                    cause = e
                )
            } else {
                throw e
            }
        }

    }

    private suspend fun AuthorizedRequest.requestWithJwtProofWithoutAttestation(
        payload: IssuanceRequestPayload,
        signers: List<ProofOfPossessionSigner>,
        keyUnlockData: Map<String, KeyUnlockData?>?,
        unlockResume: suspend (Map<String, KeyUnlockData?>) -> ResponseResult<SubmissionOutcome>,
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
                    resume = { keyUnlockData -> runBlocking {
                        unlockResume(keyUnlockData)
                    } },
                    cause = e
                )
            } else {
                throw e
            }
        }
    }

}