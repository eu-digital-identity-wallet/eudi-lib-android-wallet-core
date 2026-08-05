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
package eu.europa.ec.eudi.wallet.trust

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import eu.europa.ec.eudi.openid4vci.Credential
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.logging.Logger
import java.security.cert.TrustAnchor

/**
 * Shared logic for evaluating issuer trust during credential issuance.
 *
 * Used by both ProcessResponse and ProcessDeferredOutcome to verify the issuer's
 * certificate chain against the configured trust lists.
 *
 * @param issuerTrustConfig the trust configuration, or null if trust verification is not configured
 * @param document the document being issued
 * @param credential the credential received from the issuer
 * @param logger optional logger for debug messages
 * @return the trust evaluation result, or null if trust verification is not configured or not applicable
 * @throws IssuerNotTrustedException if the issuer is not trusted and the policy is [TrustPolicy.Action.ENFORCE]
 */
internal suspend fun evaluateIssuerTrust(
    issuerTrustConfig: IssuerTrustConfig?,
    document: Document,
    credential: Credential,
    logger: Logger?,
): CertificationChainValidation<TrustAnchor>? {
    if (issuerTrustConfig == null) {
        logger?.d(TAG, "EXIT 1: issuerTrustConfig is null")
        return null
    }

    require(credential is Credential.Str) { "Credential must be a string" }

    // 1. Derive AttestationIdentifier from document format
    val attestationIdentifier = when (val fmt = document.format) {
        is MsoMdocFormat -> AttestationIdentifier.MDoc(fmt.docType)
        is SdJwtVcFormat -> AttestationIdentifier.SDJwtVc(fmt.vct)
        else -> {
            logger?.d(TAG, "EXIT 2: Unknown document format ${document.format}")
            logger?.d(TAG, "Unknown document format, skipping trust verification")
            return null
        }
    }

    logger?.d(TAG, "attestationIdentifier=$attestationIdentifier")

    // 2. Look up verifier by format
    val verifier = issuerTrustConfig.credentialTrustVerifiers[document.format::class]
    if (verifier == null) {
        logger?.d(TAG, "EXIT 3: No verifier for ${document.format::class}, available: ${issuerTrustConfig.credentialTrustVerifiers.keys}")
        logger?.d(TAG, "No CredentialTrustVerifier for format, skipping trust verification")
        return null
    }

    // 3. Verify trust
    logger?.d(TAG, "Calling verifier.verify()...")
    val result = verifier.verify(credential.value, attestationIdentifier)
    if (result == null) {
        logger?.d(TAG, "EXIT 4: verifier.verify() returned null")
        logger?.d(TAG, "No certificate chain found, skipping trust verification")
        return null
    }

    logger?.d(TAG, "Trust result: $result")

    // 4. Resolve policy
    val verificationContext = issuerTrustConfig.classifications
        ?.classify(attestationIdentifier)
        ?.fold(
            ifPid = VerificationContext.PID,
            ifPubEaa = VerificationContext.PubEAA,
            ifQEaa = VerificationContext.QEAA,
            ifEaa = { useCase -> VerificationContext.EAA(useCase) },
        )

    val action = issuerTrustConfig.trustPolicy.resolve(attestationIdentifier, verificationContext)

    // 5. Apply policy
    if (action == TrustPolicy.Action.ENFORCE && result is CertificationChainValidation.NotTrusted) {
        throw IssuerNotTrustedException(result.cause)
    }

    return result
}

private const val TAG = "EvaluateTrust"
