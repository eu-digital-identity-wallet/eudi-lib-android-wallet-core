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

import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.serialization.json.Json
import org.multipaz.cbor.Cbor
import org.multipaz.cose.Cose
import org.multipaz.cose.toCoseLabel
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.javaX509Certificates

private const val TAG = "RegistrationCertCwt"

/** The token type header value of a registration certificate in CWT form (ETSI TS 119 475 clause 5.2). */
internal const val REGISTRATION_CERT_TYPE_CWT = "rc-wrp+cwt"

private val json = Json { ignoreUnknownKeys = true }

/**
 * Decodes the CWT form of a registration certificate and verifies its signature: the token type
 * header, the certificate chain of the `x5chain` header, the signature against the chain's leaf, and
 * the payload claims. Trust in the returned chain is established by the caller.
 */
internal suspend fun parseRegistrationCertificateCwt(
    serialized: ByteArray,
    logger: Logger? = null,
): RegistrationCertificateParseResult {
    val coseSign1 = runCatching { Cbor.decode(serialized).asCoseSign1 }
        .getOrElse {
            return RegistrationCertificateParseResult.Invalid(
                RegistrationFailureReason.MALFORMED,
                it.message,
            )
        }
    val typ = runCatching {
        (coseSign1.protectedHeaders[Cose.COSE_LABEL_TYP.toCoseLabel]
            ?: coseSign1.unprotectedHeaders[Cose.COSE_LABEL_TYP.toCoseLabel])?.asTstr
    }.getOrNull()
    if (typ != REGISTRATION_CERT_TYPE_CWT) {
        return RegistrationCertificateParseResult.Invalid(
            RegistrationFailureReason.MALFORMED,
            "unexpected typ '$typ', expected $REGISTRATION_CERT_TYPE_CWT",
        )
    }
    val chain = runCatching {
        val x5chainItem = coseSign1.unprotectedHeaders[Cose.COSE_LABEL_X5CHAIN.toCoseLabel]
            ?: coseSign1.protectedHeaders[Cose.COSE_LABEL_X5CHAIN.toCoseLabel]
            ?: error("no x5chain")
        x5chainItem.asX509CertChain
    }.getOrElse {
        return RegistrationCertificateParseResult.Invalid(
            RegistrationFailureReason.SIGNATURE_INVALID,
            "no x5chain",
        )
    }
    val javaChain = chain.javaX509Certificates
    if (javaChain.isEmpty()) {
        return RegistrationCertificateParseResult.Invalid(
            RegistrationFailureReason.SIGNATURE_INVALID,
            "empty x5chain",
        )
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
    if (!signatureValid) {
        return RegistrationCertificateParseResult.Invalid(RegistrationFailureReason.SIGNATURE_INVALID)
    }

    val registration = runCatching {
        val payload = coseSign1.payload ?: error("no payload")
        json.decodeFromString<RegistrationCertificateDto>(
            CBORObject.DecodeFromBytes(payload).ToJSONString(),
        ).toRegistrationCertificate()
    }.getOrElse {
        return RegistrationCertificateParseResult.Invalid(
            RegistrationFailureReason.MALFORMED,
            it.message,
        )
    }
    return RegistrationCertificateParseResult.Parsed(javaChain, registration)
}
