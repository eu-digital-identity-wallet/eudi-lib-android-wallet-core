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
package eu.europa.ec.eudi.wallet.registration.relyingparty

import eu.europa.ec.eudi.wallet.registration.CertificateTrust
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateParseResult
import eu.europa.ec.eudi.wallet.registration.parseRegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason

import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger

/**
 * The outcome of authenticating a Wallet-Relying Party Registration Certificate (WRPRC).
 */
internal sealed interface WrprcAuthentication {

    /** The certificate is authentic and describes the given [registration]. */
    data class Authentic(val registration: RegistrationCertificate) : WrprcAuthentication

    /** The certificate could not be authenticated, for the given [reason] and optional [detail]. */
    data class Invalid(
        val reason: RegistrationFailureReason,
        val detail: String? = null,
    ) : WrprcAuthentication
}

/**
 * Authenticates a serialized WRPRC presented by a relying party and, on success, returns the
 * [RegistrationCertificate] it describes.
 */
internal interface WrprcAuthenticator {

    /**
     * Authenticates the [serialized] registration certificate: decodes it, verifies its signature and
     * establishes the trust of its signer chain. The checks concerning the described registration are
     * performed separately by a [WrpRegistrationEvaluator].
     */
    suspend fun authenticate(serialized: ByteArray): WrprcAuthentication
}

/**
 * Default [WrprcAuthenticator] for both forms of a WRPRC, detected from the content: the JWT form
 * (JAdES) and the CWT form (COSE). Follows ETSI TS 119 475 clause 5.2.
 *
 * Authentication covers the token signature and the trust of the signer's certificate chain against
 * [certificateTrust]. The remaining checks are left to the evaluation stage.
 *
 * @property certificateTrust trust store for the registration certificate signer chain
 */
internal class DefaultWrprcAuthenticator(
    private val certificateTrust: CertificateTrust,
    private val logger: Logger? = null,
) : WrprcAuthenticator {

    override suspend fun authenticate(serialized: ByteArray): WrprcAuthentication {
        val parsed = when (val result = parseRegistrationCertificate(serialized, logger)) {
            is RegistrationCertificateParseResult.Invalid -> return invalid(result.reason, result.detail)
            is RegistrationCertificateParseResult.Parsed -> result
        }

        val trusted = runCatching {
            certificateTrust.isTrusted(parsed.chain)
        }.getOrElse {
            logger?.e(TAG, "chain trust evaluation failed: ${it.message}", it)
            return invalid(RegistrationFailureReason.UNTRUSTED_PROVIDER, "chain trust evaluation failed")
        }
        if (!trusted) {
            return invalid(
                RegistrationFailureReason.UNTRUSTED_PROVIDER,
                "chain is not trusted, leaf=${parsed.chain.firstOrNull()?.subjectX500Principal}"
            )
        }

        logger?.d(TAG, "registration certificate authenticated")
        return WrprcAuthentication.Authentic(parsed.registration)
    }

    private fun invalid(
        reason: RegistrationFailureReason,
        detail: String? = null,
    ): WrprcAuthentication.Invalid {
        logger?.d(TAG, "registration certificate not authentic: $reason${detail?.let { " ($it)" }.orEmpty()}")
        return WrprcAuthentication.Invalid(reason, detail)
    }

    private companion object {
        const val TAG = "RegistrationCertVerify"
    }
}
