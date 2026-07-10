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
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason

import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.util.X509CertUtils
import com.nimbusds.jwt.SignedJWT
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import org.multipaz.cbor.Cbor
import org.multipaz.cose.Cose
import org.multipaz.cose.toCoseLabel
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.javaX509Certificates

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
    private val certificateTrust: ReaderTrustStore,
    private val logger: Logger? = null,
) : WrprcAuthenticator {

    override suspend fun authenticate(serialized: ByteArray): WrprcAuthentication {
        val parsed = when (val result = parse(serialized)) {
            is ParseResult.Fail -> return invalid(result.reason, result.detail)
            is ParseResult.Ok -> result
        }

        val trusted = runCatching {
            certificateTrust.validateCertificationTrustPath(parsed.chain)
        }.getOrElse {
            logger?.e(TAG, "chain trust evaluation failed: ${it.message}", it)
            return invalid(RegistrationFailureReason.UNTRUSTED_PROVIDER, "chain trust evaluation failed")
        }
        if (!trusted) {
            return invalid(RegistrationFailureReason.UNTRUSTED_PROVIDER, "chain is not trusted")
        }

        logger?.d(TAG, "registration certificate authenticated")
        return WrprcAuthentication.Authentic(parsed.registration)
    }

    private sealed interface ParseResult {
        data class Ok(
            val chain: List<X509Certificate>,
            val registration: RegistrationCertificate,
        ) : ParseResult

        data class Fail(
            val reason: RegistrationFailureReason,
            val detail: String? = null,
        ) : ParseResult
    }

    /**
     * Parses and verifies the signature of the serialized registration certificate, detecting the
     * JWT or CWT form from its content, and returns the signer certificate chain together with the
     * described registration.
     */
    private suspend fun parse(serialized: ByteArray): ParseResult {
        val text = serialized.toString(Charsets.US_ASCII).trim()
        return if (COMPACT_JWS.matches(text)) parseJwt(text) else parseCwt(serialized)
    }

    private fun parseJwt(compact: String): ParseResult {
        val jwt = runCatching { SignedJWT.parse(compact) }
            .getOrElse { return ParseResult.Fail(RegistrationFailureReason.MALFORMED, it.message) }
        // The JWT typ header must be rc-wrp+jwt.
        if (jwt.header.type?.type != REG_CERT_TYPE_JWT) {
            return ParseResult.Fail(
                RegistrationFailureReason.MALFORMED,
                "unexpected typ '${jwt.header.type}', expected $REG_CERT_TYPE_JWT",
            )
        }
        val chain = jwt.header.x509CertChain
            ?.mapNotNull { X509CertUtils.parse(it.decode()) }
            .orEmpty()
        if (chain.isEmpty()) {
            return ParseResult.Fail(RegistrationFailureReason.SIGNATURE_INVALID, "no x5c certificate chain")
        }
        if (!verifySignature(jwt, chain.first())) {
            return ParseResult.Fail(RegistrationFailureReason.SIGNATURE_INVALID)
        }
        val registration = runCatching {
            json.decodeFromString<WrprcPayloadDto>(jwt.payload.toString()).toWrpRegistration()
        }.getOrElse { return ParseResult.Fail(RegistrationFailureReason.MALFORMED, it.message) }
        return ParseResult.Ok(chain, registration)
    }

    private suspend fun parseCwt(serialized: ByteArray): ParseResult {
        val coseSign1 = runCatching { Cbor.decode(serialized).asCoseSign1 }
            .getOrElse { return ParseResult.Fail(RegistrationFailureReason.MALFORMED, it.message) }
        // The CWT typ header must be rc-wrp+cwt.
        val typ = runCatching {
            (coseSign1.protectedHeaders[Cose.COSE_LABEL_TYP.toCoseLabel]
                ?: coseSign1.unprotectedHeaders[Cose.COSE_LABEL_TYP.toCoseLabel])?.asTstr
        }.getOrNull()
        if (typ != REG_CERT_TYPE_CWT) {
            return ParseResult.Fail(
                RegistrationFailureReason.MALFORMED,
                "unexpected typ '$typ', expected $REG_CERT_TYPE_CWT",
            )
        }
        val chain = runCatching {
            val x5chainItem = coseSign1.unprotectedHeaders[Cose.COSE_LABEL_X5CHAIN.toCoseLabel]
                ?: coseSign1.protectedHeaders[Cose.COSE_LABEL_X5CHAIN.toCoseLabel]
                ?: error("no x5chain")
            x5chainItem.asX509CertChain
        }.getOrElse { return ParseResult.Fail(RegistrationFailureReason.SIGNATURE_INVALID, "no x5chain") }
        val javaChain = chain.javaX509Certificates
        if (javaChain.isEmpty()) {
            return ParseResult.Fail(RegistrationFailureReason.SIGNATURE_INVALID, "empty x5chain")
        }

        val signatureValid = runCatching {
            val algorithmId = coseSign1.protectedHeaders[Cose.COSE_LABEL_ALG.toCoseLabel]?.asNumber?.toInt()
                ?: error("no algorithm")
            Cose.coseSign1Check(
                publicKey = chain.certificates.first().ecPublicKey,
                detachedData = null,
                signature = coseSign1,
                signatureAlgorithm = Algorithm.fromCoseAlgorithmIdentifier(algorithmId),
            )
            true
        }.getOrElse {
            logger?.d(TAG, "CWT signature check failed: ${it.message}")
            false
        }
        if (!signatureValid) return ParseResult.Fail(RegistrationFailureReason.SIGNATURE_INVALID)

        val registration = runCatching {
            val payload = coseSign1.payload ?: error("no payload")
            json.decodeFromString<WrprcPayloadDto>(CBORObject.DecodeFromBytes(payload).ToJSONString())
                .toWrpRegistration()
        }.getOrElse { return ParseResult.Fail(RegistrationFailureReason.MALFORMED, it.message) }
        return ParseResult.Ok(javaChain, registration)
    }

    private fun verifySignature(jwt: SignedJWT, signer: X509Certificate): Boolean = runCatching {
        val verifier: JWSVerifier = when (val key = signer.publicKey) {
            is ECPublicKey -> ECDSAVerifier(key)
            is RSAPublicKey -> RSASSAVerifier(key)
            else -> return false
        }
        jwt.verify(verifier)
    }.getOrDefault(false)

    private fun invalid(
        reason: RegistrationFailureReason,
        detail: String? = null,
    ): WrprcAuthentication.Invalid {
        logger?.d(TAG, "registration certificate not authentic: $reason${detail?.let { " ($it)" }.orEmpty()}")
        return WrprcAuthentication.Invalid(reason, detail)
    }

    private companion object {
        const val TAG = "RegistrationCertVerify"

        /** The token type header values for a WRPRC (ETSI TS 119 475 clause 5.2). */
        const val REG_CERT_TYPE_JWT = "rc-wrp+jwt"
        const val REG_CERT_TYPE_CWT = "rc-wrp+cwt"

        val COMPACT_JWS = Regex("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")
        val json = Json { ignoreUnknownKeys = true }
    }
}
