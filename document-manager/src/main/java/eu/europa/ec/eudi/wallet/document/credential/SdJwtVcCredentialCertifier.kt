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
import io.ktor.client.HttpClient
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.crypto.javaPublicKey
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

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

        val nbf = claims["nbf"]?.jsonPrimitive?.longOrNull?.let { Instant.fromEpochSeconds(it) }
        val iat = claims["iat"]?.jsonPrimitive?.longOrNull?.let { Instant.fromEpochSeconds(it) }
        val exp = claims["exp"]?.jsonPrimitive?.longOrNull?.let { Instant.fromEpochSeconds(it) }
        val validFrom = nbf ?: iat ?: Clock.System.now()
        val validUntil = exp ?: validFrom.plus(30.days)

        credential.certify(ByteString(data))
    }
}
