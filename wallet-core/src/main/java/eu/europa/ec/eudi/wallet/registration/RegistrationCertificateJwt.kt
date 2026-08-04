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

import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.util.X509CertUtils
import com.nimbusds.jwt.SignedJWT
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey

/** The token type header value of a registration certificate in JWT form (ETSI TS 119 475 clause 5.2). */
internal const val REGISTRATION_CERT_TYPE_JWT = "rc-wrp+jwt"

private val json = Json { ignoreUnknownKeys = true }

/**
 * The outcome of decoding a registration certificate and verifying its signature.
 */
internal sealed interface RegistrationCertificateParseResult {

    /**
     * The certificate was decoded and its signature verified against the leaf of the certificate
     * chain it carries. Trust in that [chain] is established by the caller.
     */
    data class Parsed(
        val chain: List<X509Certificate>,
        val registration: RegistrationCertificate,
    ) : RegistrationCertificateParseResult

    /** The certificate could not be decoded or its signature did not verify. */
    data class Invalid(
        val reason: RegistrationFailureReason,
        val detail: String? = null,
    ) : RegistrationCertificateParseResult
}

/**
 * Decodes the JWT form of a registration certificate and verifies its signature: the token type
 * header, the certificate chain of the `x5c` header, the signature against the chain's leaf, and the
 * payload claims. Trust in the returned chain is established by the caller.
 */
internal fun parseRegistrationCertificateJwt(compact: String): RegistrationCertificateParseResult {
    val jwt = runCatching { SignedJWT.parse(compact) }
        .getOrElse {
            return RegistrationCertificateParseResult.Invalid(
                RegistrationFailureReason.MALFORMED,
                it.message,
            )
        }
    if (jwt.header.type?.type != REGISTRATION_CERT_TYPE_JWT) {
        return RegistrationCertificateParseResult.Invalid(
            RegistrationFailureReason.MALFORMED,
            "unexpected typ '${jwt.header.type}', expected $REGISTRATION_CERT_TYPE_JWT",
        )
    }
    val chain = jwt.header.x509CertChain
        ?.mapNotNull { X509CertUtils.parse(it.decode()) }
        .orEmpty()
    if (chain.isEmpty()) {
        return RegistrationCertificateParseResult.Invalid(
            RegistrationFailureReason.SIGNATURE_INVALID,
            "no x5c certificate chain",
        )
    }
    if (!jwt.verifiesWith(chain.first())) {
        return RegistrationCertificateParseResult.Invalid(RegistrationFailureReason.SIGNATURE_INVALID)
    }
    val registration = runCatching {
        json.decodeFromString<RegistrationCertificateDto>(jwt.payload.toString()).toRegistrationCertificate()
    }.getOrElse {
        return RegistrationCertificateParseResult.Invalid(
            RegistrationFailureReason.MALFORMED,
            it.message,
        )
    }
    return RegistrationCertificateParseResult.Parsed(chain, registration)
}

/**
 * Whether the signature of this token verifies with the public key of [signer]. Signers holding a key
 * of an unsupported type do not verify.
 */
private fun SignedJWT.verifiesWith(signer: X509Certificate): Boolean = runCatching {
    val verifier: JWSVerifier = when (val key = signer.publicKey) {
        is ECPublicKey -> ECDSAVerifier(key)
        is RSAPublicKey -> RSASSAVerifier(key)
        else -> return false
    }
    verify(verifier)
}.getOrDefault(false)
