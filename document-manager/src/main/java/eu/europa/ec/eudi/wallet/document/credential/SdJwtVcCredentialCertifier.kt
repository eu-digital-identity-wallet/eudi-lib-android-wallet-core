/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.document.credential

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyConverter
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.wallet.document.internal.sdJwtVcString
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.crypto.javaPublicKey
import org.multipaz.sdjwt.SdJwt

/**
 * Certifies SD-JWT VC credentials by parsing the SD-JWT, verifying the device public key binding,
 * and extracting validity periods.
 *
 * **Important:** This certifier does **not** perform issuer trust verification (e.g., X.509
 * certificate path validation or issuer metadata resolution). Issuer trust verification is the
 * responsibility of the integrating layer which has access to trusted issuer
 * lists and certificate trust stores.
 *
 * This certifier enforces:
 * - The issuer signature: the issuer-signed JWT verifies with the public key of the first
 *   certificate of its `x5c` header
 * - Valid SD-JWT VC structure (parseable by the SD-JWT library)
 * - Presence of the `cnf` (confirmation) claim with a `jwk` key
 * - Device public key binding: the key in the `cnf` claim must match the credential's key
 * - Extraction of validity period from `nbf`/`iat` and `exp` claims
 */
class SdJwtVcCredentialCertifier : CredentialCertification {
    override suspend fun certifyCredential(
        credential: SecureAreaBoundCredential,
        issuedCredential: IssuerProvidedCredential,
    ) {
        val data = issuedCredential.data

        verifyIssuerSignature(data.sdJwtVcString)

        val sdJwt = DefaultSdJwtOps.unverifiedIssuanceFrom(data.sdJwtVcString).getOrElse {
            throw IllegalArgumentException("Invalid SD-JWT VC", it)
        }

        val (_, claims) = sdJwt.jwt

        val cnf = claims["cnf"]
            ?: throw IllegalArgumentException("SD-JWT VC is missing required 'cnf' claim for key binding")

        val jwk = JWK.parse(Json.Default.decodeFromString<JsonObject>(cnf.toString())["jwk"].toString())
        val sdjwtVcPk = KeyConverter.toJavaKeys(listOf(jwk)).first()
            ?: throw IllegalArgumentException("Invalid public key in SD-JWT VC 'cnf' claim")

        if (credential.secureArea.getKeyInfo(credential.alias).publicKey.javaPublicKey != sdjwtVcPk) {
            throw IllegalArgumentException("Public key in SD-JWT VC does not match the one in the request")
        }

        credential.certify(ByteString(data))
    }

    /**
     * Verifies the signature of the issuer-signed JWT with the public key of the first certificate
     * of its `x5c` header (OpenID4VC HAIP 1.0, section 6.1).
     *
     * This checks the integrity of the credential. Whether the issuer is trusted is decided by the
     * issuer trust evaluation of the wallet.
     */
    private suspend fun verifyIssuerSignature(sdJwtVc: String) {
        val sdJwt = SdJwt.fromCompactSerialization(sdJwtVc)
        val x5c = requireNotNull(sdJwt.x5c) { "No x5c in the header of the SD-JWT VC" }
        sdJwt.verify(x5c.certificates.first().ecPublicKey)
    }
}
