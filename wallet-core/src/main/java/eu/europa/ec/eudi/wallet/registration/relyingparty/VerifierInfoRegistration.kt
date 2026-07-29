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

import eu.europa.ec.eudi.openid4vp.VerifierInfo
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.Base64

/**
 * The `format` value of the OpenID4VP `verifier_info` element that carries the registration
 * certificate (ETSI TS 119 472-2 clause 6.3.2.2).
 */
internal const val FORMAT_REGISTRATION_CERT = "registration_cert"

private val COMPACT_JWS = Regex("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")

/**
 * Extracts the serialized registration certificate from an OpenID4VP `verifier_info` value.
 *
 */
internal fun VerifierInfo?.extractRegistrationCertificate(): ByteArray? {
    val attestation =
        this?.attestations.orEmpty().singleOrNull { it.format.value == FORMAT_REGISTRATION_CERT }
            ?: return null
    if (attestation.credentialIds != null) return null
    val content = (attestation.data.value as? JsonPrimitive)?.contentOrNull ?: return null
    return decodeSerializedRegistrationCertificate(content)
}

internal fun decodeSerializedRegistrationCertificate(content: String): ByteArray? =
    if (COMPACT_JWS.matches(content)) {
        content.toByteArray(Charsets.US_ASCII)
    } else {
        runCatching { Base64.getUrlDecoder().decode(content) }.getOrNull()
    }