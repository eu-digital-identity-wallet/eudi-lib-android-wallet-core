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

import java.security.cert.X509Certificate

/**
 * X.509 Subject Alternative Name type constants per RFC 5280 §4.2.1.6.
 */
private const val SAN_TYPE_DNS_NAME = 2
private const val SAN_TYPE_URI = 6

/**
 * Extracts all Uniform Resource Identifier entries from the certificate's
 * Subject Alternative Name extension (type 6).
 *
 * @return SAN URI strings, or an empty list when the extension is absent
 */
internal fun X509Certificate.sanUris(): List<String> =
    sanEntries(SAN_TYPE_URI)

/**
 * Extracts all DNS Name entries from the certificate's
 * Subject Alternative Name extension (type 2).
 *
 * @return SAN DNS name strings, or an empty list when the extension is absent
 */
internal fun X509Certificate.sanDnsNames(): List<String> =
    sanEntries(SAN_TYPE_DNS_NAME)

private fun X509Certificate.sanEntries(type: Int): List<String> =
    buildList {
        subjectAlternativeNames
            ?.filter { entry -> !entry.isNullOrEmpty() && entry.size == 2 }
            ?.forEach { entry ->
                val altNameType = entry[0] as? Int
                if (altNameType == type) {
                    (entry[1] as? String)?.let { add(it) }
                }
            }
    }
