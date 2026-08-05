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
package eu.europa.ec.eudi.wallet.statium

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.etsi1196x2.consultation.CertificationChainValidation
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import eu.europa.ec.eudi.statium.VerifyStatusListTokenCwtSignature
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.trust.StatusListTrustConfig
import eu.europa.ec.eudi.wallet.trust.TrustPolicy
import org.multipaz.cbor.Cbor
import org.multipaz.cose.Cose
import org.multipaz.cose.toCoseLabel
import org.multipaz.crypto.javaX509Certificates
import java.security.cert.X509Certificate
import kotlin.time.Instant

/**
 * Wraps a [VerifyStatusListTokenCwtSignature] to add ETSI trust evaluation of
 * the signer's certificate chain after cryptographic signature verification.
 *
 * Extracts the x5chain from COSE_Sign1 unprotected headers (label 33) and evaluates
 * trust using the configured ETSI trust source.
 *
 * @param delegate the underlying CWT signature verifier
 * @param trustConfig the ETSI trust configuration for status list tokens
 * @param attestationIdentifier the attestation identifier derived from the document format
 */
internal class TrustEvaluatingCwtSignatureVerifier(
    private val delegate: VerifyStatusListTokenCwtSignature,
    private val trustConfig: StatusListTrustConfig,
    private val attestationIdentifier: AttestationIdentifier,
    private val logger: Logger? = null,
) : VerifyStatusListTokenCwtSignature {

    override suspend fun invoke(
        statusListToken: ByteArray,
        at: Instant,
    ): Result<Unit> = runCatching {
        // Delegate cryptographic signature verification
        logger?.d(TAG, "CWT: delegating signature verification...")
        delegate(statusListToken, at).getOrThrow()
        logger?.d(TAG, "CWT: signature verification passed")

        // Extract x5chain from COSE_Sign1 unprotected headers (label 33)
        val certs = extractX5cFromCwt(statusListToken)
        logger?.d(TAG, "CWT: x5chain has ${certs.size} certs, leaf=${certs.firstOrNull()?.subjectX500Principal}")

        // Evaluate trust via ETSI (revocation context for status list tokens)
        val trustResult = trustConfig.isChainTrustedForAttestation
            .revocation(certs, attestationIdentifier)
        logger?.d(TAG, "CWT: trustResult=$trustResult for attestation=$attestationIdentifier")

        // Resolve verification context from classifications
        val verificationContext = trustConfig.classifications
            ?.classify(attestationIdentifier)
            ?.fold(
                ifPid = VerificationContext.PIDStatus,
                ifPubEaa = VerificationContext.PubEAAStatus,
                ifQEaa = VerificationContext.QEAAStatus,
                ifEaa = { useCase -> VerificationContext.EAAStatus(useCase) },
            )
        logger?.d(TAG, "CWT: verificationContext=$verificationContext")

        // Apply policy
        val action = trustConfig.trustPolicy.resolve(attestationIdentifier, verificationContext)
        logger?.d(TAG, "CWT: policy action=$action")
        if (action == TrustPolicy.Action.ENFORCE) {
            when {
                trustResult == null -> {
                    logger?.e(TAG, "CWT: ENFORCE + null trustResult → throwing")
                    throw StatusListNotTrustedException(
                        "No trust anchors available for attestation=$attestationIdentifier " +
                            "(verificationContext=$verificationContext)"
                    )
                }
                trustResult is CertificationChainValidation.NotTrusted -> {
                    logger?.e(TAG, "CWT: ENFORCE + NotTrusted → throwing", trustResult.cause)
                    throw StatusListNotTrustedException(
                        "Status list token signer certificate chain is not trusted",
                        trustResult.cause,
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "StatusListTrust"
        private fun extractX5cFromCwt(statusListToken: ByteArray): List<X509Certificate> {
            val coseSign1 = Cbor.decode(statusListToken).asCoseSign1

            val x5chainDataItem = coseSign1.unprotectedHeaders[Cose.COSE_LABEL_X5CHAIN.toCoseLabel]
                ?: throw IllegalStateException("Missing x5chain in COSE unprotected headers")
            val x5chain = x5chainDataItem.asX509CertChain

            val javaCerts = x5chain.javaX509Certificates
            require(javaCerts.isNotEmpty()) { "x5chain must contain at least one certificate" }

            return javaCerts
        }
    }
}
