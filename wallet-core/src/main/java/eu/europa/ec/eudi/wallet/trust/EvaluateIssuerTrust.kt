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
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import java.security.cert.TrustAnchor

/**
 * Shared logic for evaluating issuer trust during credential issuance.
 *
 * Used by both ProcessResponse and ProcessDeferredOutcome to verify the issuer's
 * certificate chain against the configured trust lists.
 *
 * When the trust policy is [TrustPolicy.Action.ENFORCE], this function **fails closed**:
 * any condition that prevents trust evaluation (unknown format, missing verifier,
 * unclassified attestation type, missing certificate chain) is treated as a trust
 * failure and throws [IssuerNotTrustedException]. When the policy is
 * [TrustPolicy.Action.INFORM], those conditions return `null` (informational only).
 *
 * @param issuerTrustConfig the trust configuration, or null if trust verification is not configured
 * @param document the document being issued
 * @param credential the credential received from the issuer
 * @param logger optional logger for debug messages
 * @return the trust evaluation result, or null if trust verification is not configured
 *   or the policy is [TrustPolicy.Action.INFORM] and evaluation could not be performed
 * @throws IssuerNotTrustedException if the issuer is not trusted and the policy is [TrustPolicy.Action.ENFORCE]
 */
internal suspend fun evaluateIssuerTrust(
    issuerTrustConfig: IssuerTrustConfig?,
    document: Document,
    credential: Credential,
    logger: Logger?,
): CertificationChainValidation<TrustAnchor>? {
    if (issuerTrustConfig == null) {
        logger?.d(TAG, "issuerTrustConfig is null, trust verification not configured")
        return null
    }

    require(credential is Credential.Str) { "Credential must be a string" }

    // 1. Derive AttestationIdentifier from document format
    val attestationIdentifier = when (val fmt = document.format) {
        is MsoMdocFormat -> AttestationIdentifier.MDoc(fmt.docType)
        is SdJwtVcFormat -> AttestationIdentifier.SDJwtVc(fmt.vct)
        else -> null
    }

    // 2. Resolve policy early — needed before any exit point so ENFORCE can fail closed.
    //    When attestationIdentifier is null (unknown format), verificationContext is also
    //    null and the policy falls through to its default action.
    val verificationContext = attestationIdentifier?.let {
        issuerTrustConfig.classifications
            ?.classify(it)
            ?.fold(
                ifPid = VerificationContext.PID,
                ifPubEaa = VerificationContext.PubEAA,
                ifQEaa = VerificationContext.QEAA,
                ifEaa = { useCase -> VerificationContext.EAA(useCase) },
            )
    }
    val action = issuerTrustConfig.trustPolicy.resolve(attestationIdentifier, verificationContext)

    // 3. Check attestation identifier was derived
    if (attestationIdentifier == null) {
        return throwIfEnforced(
            action,
            "Unknown document format ${document.format}, cannot evaluate trust",
            logger,
        )
    }

    logger?.d(TAG, "attestationIdentifier=$attestationIdentifier, verificationContext=$verificationContext, action=$action")

    // 4. Look up verifier by format
    val verifier = issuerTrustConfig.credentialTrustVerifiers[document.format::class]
    if (verifier == null) {
        logger?.d(TAG, "No verifier for ${document.format::class}, available: ${issuerTrustConfig.credentialTrustVerifiers.keys}")
        return throwIfEnforced(
            action,
            "No CredentialTrustVerifier for format ${document.format::class.simpleName}",
            logger,
        )
    }

    // 5. Verify trust
    logger?.d(TAG, "Calling verifier.verify()...")
    val result = verifier.verify(credential.value, attestationIdentifier)
    if (result == null) {
        logger?.d(TAG, "verifier.verify() returned null")
        return throwIfEnforced(
            action,
            "No trust evaluation for attestation=$attestationIdentifier (verificationContext=$verificationContext)",
            logger,
        )
    }

    logger?.d(TAG, "Trust result: $result")

    // 6. Apply policy on concrete result
    if (action == TrustPolicy.Action.ENFORCE && result is CertificationChainValidation.NotTrusted) {
        throw IssuerNotTrustedException("Issuer certificate chain is not trusted", result.cause)
    }

    return result
}

/**
 * When [action] is [TrustPolicy.Action.ENFORCE], throws [IssuerNotTrustedException].
 * Otherwise returns `null` (the trust failure is informational only).
 */
private fun throwIfEnforced(
    action: TrustPolicy.Action?,
    message: String,
    logger: Logger?,
): Nothing? {
    if (action == TrustPolicy.Action.ENFORCE) {
        logger?.e(TAG, "ENFORCE: $message")
        throw IssuerNotTrustedException(message)
    }
    logger?.d(TAG, "INFORM: $message")
    return null
}

private const val TAG = "EvaluateTrust"
