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

import eu.europa.ec.eudi.statium.VerifyStatusListTokenCwtSignature
import org.multipaz.cbor.Cbor
import org.multipaz.cose.Cose
import org.multipaz.cose.toCoseLabel
import org.multipaz.crypto.Algorithm
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import kotlin.time.Instant

/**
 * Verifies the signature of a CWT (CBOR Web Token) status list token using the x5chain header.
 *
 * Parses the COSE_Sign1 structure, extracts the x5chain from unprotected headers (label 33),
 * and verifies the signature using the leaf certificate's public key.
 */
class VerifyStatusListTokenSignatureCwtX5c : VerifyStatusListTokenCwtSignature {

    override suspend fun invoke(
        statusListToken: ByteArray,
        at: Instant,
    ): Result<Unit> = runCatching {
        // Decode COSE_Sign1 from CWT bytes
        val coseSign1 = Cbor.decode(statusListToken).asCoseSign1

        // Extract x5chain from unprotected headers (COSE label 33)
        val x5chainDataItem = coseSign1.unprotectedHeaders[Cose.COSE_LABEL_X5CHAIN.toCoseLabel]
            ?: throw IllegalStateException("Missing x5chain in COSE unprotected headers")
        val x5chain = x5chainDataItem.asX509CertChain

        // Get the leaf certificate
        val leafCert = x5chain.certificates.firstOrNull()
            ?: throw IllegalStateException("x5chain must contain at least one certificate")

        // Determine algorithm from protected headers
        val algIdentifier = coseSign1.protectedHeaders[Cose.COSE_LABEL_ALG.toCoseLabel]
            ?.asNumber?.toInt()
            ?: throw IllegalStateException("Missing algorithm in COSE protected headers")
        val algorithm = Algorithm.fromCoseAlgorithmIdentifier(algIdentifier)

        // Verify COSE signature using the leaf certificate's public key
        Cose.coseSign1Check(
            publicKey = leafCert.ecPublicKey,
            detachedData = null,
            signature = coseSign1,
            signatureAlgorithm = algorithm,
        )
    }
}

/**
 * Companion object extension for [VerifyStatusListTokenCwtSignature] to provide an x5c implementation.
 */
val VerifyStatusListTokenCwtSignature.Companion.x5c: VerifyStatusListTokenCwtSignature
    get() = VerifyStatusListTokenSignatureCwtX5c()
