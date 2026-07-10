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

import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.util.X509CertUtils
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.statium.VerifyStatusListTokenJwtSignature
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import kotlin.time.Instant

/**
 * Verifies the signature of the status list token that carries a registration certificate's
 * revocation status. The signature is checked against the certificate in the token's `x5c` header.
 * When [statusTrust] is set, that certificate chain is additionally validated against it; when null,
 * only the signature is verified.
 */
internal class WrprcStatusTokenVerifier(
    private val statusTrust: ReaderTrustStore? = null,
) : VerifyStatusListTokenJwtSignature {

    override suspend fun invoke(statusListToken: String, at: Instant): Result<Unit> = runCatching {
        val jwt = SignedJWT.parse(statusListToken)
        val chain = jwt.header?.x509CertChain
            ?.mapNotNull { X509CertUtils.parse(it.decode()) }
            .orEmpty()
        check(chain.isNotEmpty()) { "status list token has no x5c certificate chain" }

        val verifier: JWSVerifier = when (val key = chain.first().publicKey) {
            is ECPublicKey -> ECDSAVerifier(key)
            is RSAPublicKey -> RSASSAVerifier(key)
            else -> error("unsupported status list token key type")
        }
        check(jwt.verify(verifier)) { "status list token signature is invalid" }

        if (statusTrust != null) {
            check(statusTrust.validateCertificationTrustPath(chain)) {
                "status list token certificate chain is not trusted"
            }
        }
    }
}
