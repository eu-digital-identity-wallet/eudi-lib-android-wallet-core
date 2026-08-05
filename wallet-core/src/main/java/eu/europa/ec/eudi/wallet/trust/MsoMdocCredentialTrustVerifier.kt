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
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForAttestation
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import org.multipaz.cbor.Cbor
import org.multipaz.cose.Cose
import org.multipaz.cose.toCoseLabel
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.javaX509Certificates
import org.multipaz.mdoc.mso.StaticAuthDataParser
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.util.Base64

/**
 * [CredentialTrustVerifier] for MsoMdoc credentials.
 *
 * Extracts the X.509 certificate chain from the COSE_Sign1 unprotected `x5chain` header
 * (label 33), evaluates trust via [IsChainTrustedForAttestation], and verifies the COSE
 * signature using the leaf certificate's public key.
 *
 * @param isChainTrusted the ETSI trust source for validating certificate chains
 */
internal class MsoMdocCredentialTrustVerifier(
    private val isChainTrusted: IsChainTrustedForAttestation<List<X509Certificate>, TrustAnchor>,
    private val logger: Logger? = null,
) : CredentialTrustVerifier {

    override suspend fun verify(
        credentialValue: String,
        attestationIdentifier: AttestationIdentifier,
    ): CertificationChainValidation<TrustAnchor>? = runCatching {
        // Base64url-decode the credential string
        val credentialBytes = Base64.getUrlDecoder().decode(credentialValue)

        // Parse issuerAuth from StaticAuthData
        val issuerAuthBytes = StaticAuthDataParser(credentialBytes)
            .parse()
            .issuerAuth

        // Decode COSE_Sign1 from issuerAuth
        val coseSign1 = Cbor.decode(issuerAuthBytes).asCoseSign1

        // Extract x5chain from unprotected headers (COSE label 33)
        val x5chainDataItem = coseSign1.unprotectedHeaders[Cose.COSE_LABEL_X5CHAIN.toCoseLabel]
            ?: run { logger?.d(TAG, "No x5chain in COSE unprotected headers"); return@runCatching null }
        val x5chain = x5chainDataItem.asX509CertChain

        // Convert to List<X509Certificate> (JVM extension)
        val javaCerts = x5chain.javaX509Certificates
        logger?.d(TAG, "x5chain has ${javaCerts.size} certs, leaf=${javaCerts.firstOrNull()?.subjectX500Principal}")
        require(javaCerts.isNotEmpty()) { "x5chain must contain at least one certificate" }

        // Evaluate trust via the ETSI library
        val trustResult = isChainTrusted.issuance(javaCerts, attestationIdentifier)
            ?: run { logger?.d(TAG, "issuance() returned null"); return@runCatching null }

        // Verify COSE signature using the leaf certificate's public key
        val leafCert = x5chain.certificates.first()
        val algIdentifier = coseSign1.protectedHeaders[Cose.COSE_LABEL_ALG.toCoseLabel]
            ?.asNumber?.toInt()
            ?: run { logger?.d(TAG, "No algorithm in COSE protected headers"); return@runCatching null }
        val algorithm = Algorithm.fromCoseAlgorithmIdentifier(algIdentifier)

        Cose.coseSign1Check(
            publicKey = leafCert.ecPublicKey,
            detachedData = null,
            signature = coseSign1,
            signatureAlgorithm = algorithm,
        )

        trustResult
    }.onFailure { e ->
        logger?.e(TAG, "MsoMdoc credential trust verification failed", e)
    }.getOrNull()

    companion object {
        private const val TAG = "MdocIssuerTrust"
    }
}
