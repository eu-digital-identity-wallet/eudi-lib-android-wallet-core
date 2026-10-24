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

package eu.europa.ec.eudi.wallet.registration

import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifierPredicate
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.registration.issuer.DefaultIssuerRegistrationEvaluator
import eu.europa.ec.eudi.wallet.registration.issuer.IssuerRegistrationEvaluator
import eu.europa.ec.eudi.wallet.registration.relyingparty.DefaultWrpRegistrationEvaluator
import eu.europa.ec.eudi.wallet.registration.relyingparty.WrpRegistrationEvaluator
import io.ktor.client.HttpClient
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

/**
 * The trust for the signer of a registration certificate and for the signer of the status list that
 * carries its revocation status; either is null when no trust is available for it.
 */
internal data class RegistrationTrust(
    val certificateTrust: CertificateTrust?,
    val statusTrust: CertificateTrust?,
) {

    /**
     * The evaluator of a relying party's registration: [custom] when configured, else the built-in
     * evaluator, which checks the revocation status against [statusTrust].
     */
    fun wrpRegistrationEvaluator(
        custom: WrpRegistrationEvaluator?,
        logger: Logger? = null,
        httpClientFactory: (() -> HttpClient)? = null,
    ): WrpRegistrationEvaluator =
        custom ?: DefaultWrpRegistrationEvaluator(
            statusTrust = statusTrust,
            logger = logger,
            httpClientFactory = httpClientFactory,
        )

    /**
     * The evaluator of a credential issuer's registration: [custom] when configured, else the built-in
     * evaluator, which checks the revocation status against [statusTrust] and classifies the offered
     * attestations with [isPid].
     */
    fun issuerRegistrationEvaluator(
        custom: IssuerRegistrationEvaluator?,
        isPid: AttestationIdentifierPredicate,
        logger: Logger? = null,
        httpClientFactory: (() -> HttpClient)? = null,
    ): IssuerRegistrationEvaluator =
        custom ?: DefaultIssuerRegistrationEvaluator(
            isPid = isPid,
            statusTrust = statusTrust,
            logger = logger,
            httpClientFactory = httpClientFactory
        )

    companion object {

        /**
         * The registration trust of a relying party: the certificates configured with
         * [EudiWalletConfig.configureWrpRegistrationTrust], else the ETSI trusted lists. The reader
         * authentication trust is not used, so an access certificate authority cannot vouch for a
         * registration certificate.
         */
        fun forRelyingParty(
            config: EudiWalletConfig,
            etsiSource: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>?,
            logger: Logger? = null,
        ): RegistrationTrust = RegistrationTrust(
            certificateTrust = registrationCertificateTrust(
                trustedCertificates = config.wrpRegistrationTrustedCertificates,
                revocationPolicy = config.wrpRegistrationRevocationPolicy,
                etsiSource = etsiSource,
                logger = logger
            ),
            statusTrust = registrationStatusTrust(
                statusListTrustedCertificates = config.wrpRegistrationStatusListTrustedCertificates,
                revocationPolicy = config.wrpRegistrationRevocationPolicy,
                etsiSource = etsiSource,
                logger = logger
            ),
        )

        /**
         * The registration trust of a credential issuer: the certificates configured with
         * [EudiWalletConfig.configureIssuerRegistrationTrust], else the ETSI trusted lists.
         */
        fun forIssuer(
            config: EudiWalletConfig,
            etsiSource: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>?,
            logger: Logger? = null,
        ): RegistrationTrust = RegistrationTrust(
            certificateTrust = registrationCertificateTrust(
                trustedCertificates = config.issuerRegistrationTrustedCertificates,
                revocationPolicy = config.issuerRegistrationRevocationPolicy,
                etsiSource = etsiSource,
                logger = logger
            ),
            statusTrust = registrationStatusTrust(
                statusListTrustedCertificates = config.issuerRegistrationStatusListTrustedCertificates,
                revocationPolicy = config.issuerRegistrationRevocationPolicy,
                etsiSource = etsiSource,
                logger = logger
            ),
        )
    }
}
