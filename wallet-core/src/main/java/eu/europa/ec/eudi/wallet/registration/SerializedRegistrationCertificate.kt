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

import eu.europa.ec.eudi.wallet.logging.Logger
import java.util.Base64

private val COMPACT_JWS = Regex("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")

/**
 * Decodes the serialized registration certificate carried by the `verifier_info` and `issuer_info`
 * parameters. The value is base64url encoded; the compact JWS form is also accepted. Returns null
 * when the value is neither.
 */
internal fun decodeSerializedRegistrationCertificate(content: String): ByteArray? =
    if (COMPACT_JWS.matches(content)) {
        content.toByteArray(Charsets.US_ASCII)
    } else {
        runCatching { Base64.getUrlDecoder().decode(content) }.getOrNull()
    }

/**
 * Decodes a serialized registration certificate and verifies its signature.
 */
internal suspend fun parseRegistrationCertificate(
    serialized: ByteArray,
    logger: Logger? = null,
): RegistrationCertificateParseResult {
    val text = serialized.toString(Charsets.US_ASCII).trim()
    return if (COMPACT_JWS.matches(text)) {
        parseRegistrationCertificateJwt(text)
    } else {
        parseRegistrationCertificateCwt(serialized, logger)
    }
}
