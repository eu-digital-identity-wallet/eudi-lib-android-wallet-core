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

package eu.europa.ec.eudi.wallet.registration.issuer

import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.IssuerInfo
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.registration.CertificateTrust
import eu.europa.ec.eudi.wallet.registration.CredentialMeta
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateParseResult
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import eu.europa.ec.eudi.wallet.registration.decodeSerializedRegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.parseRegistrationCertificate
import kotlinx.serialization.json.JsonPrimitive
import java.security.cert.X509Certificate

/**
 * Resolves and evaluates the credential issuer's registration certificate carried in the signed
 * issuer metadata (`issuer_info`, ETSI TS 119 472-3).
 *
 * The certificate is authenticated (signature and signer-chain trust) and then handed to [evaluator],
 * which checks it against the offered credential configurations.
 *
 * @property certificateTrust trust for the registration certificate signer chain
 * @property evaluator the evaluator applied to the authenticated registration
 * @property logger optional logger
 */
internal class IssuerRegistrationResolver(
    private val certificateTrust: CertificateTrust,
    private val evaluator: IssuerRegistrationEvaluator,
    private val logger: Logger? = null,
) {

    /**
     * Resolves the registration certificate from [metadata] and evaluates it against the offered
     * configurations identified by [offeredConfigurationIds].
     *
     * @return the evaluated registration, a failed result when the certificate is present but could
     *   not be validated, or null when no registration certificate is offered
     */
    suspend fun resolve(
        metadata: CredentialIssuerMetadata,
        offeredConfigurationIds: List<CredentialConfigurationIdentifier>,
    ): RegistrationCertificateResult? {
        val registrationCerts = metadata.issuerInfo?.attestations
            ?.filter { it.format == IssuerInfo.Attestation.Format.REGISTRATION_CERT }
            .orEmpty()
        if (registrationCerts.isEmpty()) return null
        if (registrationCerts.size > 1) {
            logger?.d(TAG, "multiple registration certificates offered; treated as malformed")
            return RegistrationCertificateResult.Failed(RegistrationFailureReason.MALFORMED)
        }
        val data = (registrationCerts.first().data.value as? JsonPrimitive)
            ?.takeIf { it.isString }?.content
            ?: return RegistrationCertificateResult.Failed(RegistrationFailureReason.MALFORMED)
        val serialized = decodeSerializedRegistrationCertificate(data)
            ?: return RegistrationCertificateResult.Failed(RegistrationFailureReason.MALFORMED)

        val offeredAttestations = metadata.credentialConfigurationsSupported
            .filterKeys { it in offeredConfigurationIds }
            .values.mapNotNull { it.toOfferedAttestation() }
        return evaluate(metadata.metadataSigningCertificate, serialized, offeredAttestations)
    }

    /**
     * Authenticates the serialized registration certificate and, on success, evaluates the described
     * registration against the [offeredAttestations] and its binding to [accessCertificate].
     */
    internal suspend fun evaluate(
        accessCertificate: X509Certificate?,
        serialized: ByteArray,
        offeredAttestations: List<OfferedAttestation>,
    ): RegistrationCertificateResult = when (val authentication = authenticate(serialized)) {
        is RegistrationCertificateParseResult.Parsed -> evaluator.evaluate(
            registration = authentication.registration,
            accessCertificate = accessCertificate,
            offeredAttestations = offeredAttestations,
        )

        is RegistrationCertificateParseResult.Invalid ->
            RegistrationCertificateResult.Failed(authentication.reason)
    }

    /**
     * Decodes the registration certificate, verifies its signature and establishes the trust of its
     * signer chain. A certificate that fails to authenticate does not carry the described
     * registration, whose content is not established as authentic and is withheld from the user.
     */
    private suspend fun authenticate(serialized: ByteArray): RegistrationCertificateParseResult {
        val parsed = when (val result = parseRegistrationCertificate(serialized, logger)) {
            is RegistrationCertificateParseResult.Invalid -> return result
            is RegistrationCertificateParseResult.Parsed -> result
        }
        val trusted = runCatching { certificateTrust.isTrusted(parsed.chain) }
            .getOrElse {
                logger?.d(TAG, "signer chain trust evaluation failed: ${it.message}")
                false
            }
        if (!trusted) {
            logger?.d(
                TAG,
                "signer chain is not trusted: leaf=${parsed.chain.firstOrNull()?.subjectX500Principal}"
            )
            return RegistrationCertificateParseResult.Invalid(RegistrationFailureReason.UNTRUSTED_PROVIDER)
        }
        logger?.d(TAG, "issuer registration certificate authenticated")
        return parsed
    }

    private companion object {
        const val TAG = "IssuerRegistrationResolve"

    }
}

private const val MSO_MDOC_FORMAT = "mso_mdoc"
private const val SD_JWT_VC_FORMAT = "dc+sd-jwt"

/**
 * Projects an offered [CredentialConfiguration] onto the format and type used to check it against the
 * issuer's registered `provides_attestations`. Formats other than mdoc and SD-JWT VC are not evaluated.
 */
internal fun CredentialConfiguration.toOfferedAttestation(): OfferedAttestation? = when (this) {
    is MsoMdocCredential -> OfferedAttestation(
        format = MSO_MDOC_FORMAT,
        meta = CredentialMeta(doctypeValue = docType),
    )

    is SdJwtVcCredential -> OfferedAttestation(
        format = SD_JWT_VC_FORMAT,
        meta = CredentialMeta(vctValues = listOf(type)),
    )

    else -> null
}
