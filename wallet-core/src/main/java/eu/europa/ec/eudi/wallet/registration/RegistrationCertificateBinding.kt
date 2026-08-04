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

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import java.security.cert.X509Certificate

/**
 * Whether this registration certificate is bound to the relying party that presents it.
 *
 * The access certificate carries the presenter's identifier — the organization identifier for a
 * legal person or, in its absence, the serial number for a natural person (ETSI TS 119 475). When the
 * registration certificate names an intermediary, the identifier is matched against the intermediary;
 * otherwise against the relying party's own identifiers.
 *
 * @param accessCertificate the leaf access certificate that signed the request; when null the
 *   registration is treated as unbound.
 */
internal fun RegistrationCertificate.isBoundTo(accessCertificate: X509Certificate?): Boolean {
    val presenterId = accessCertificate?.subjectIdentifier() ?: return false
    return intermediary
        ?.let { it.identifier == presenterId }
        ?: identifiers.any { it.value == presenterId }
}

private fun X509Certificate.subjectIdentifier(): String? {
    val subject = runCatching { X500Name.getInstance(subjectX500Principal.encoded) }.getOrNull()
        ?: return null
    return subject.firstRdnValue(BCStyle.ORGANIZATION_IDENTIFIER)
        ?: subject.firstRdnValue(BCStyle.SERIALNUMBER)
}

private fun X500Name.firstRdnValue(type: ASN1ObjectIdentifier): String? = runCatching {
    getRDNs(type).firstOrNull()?.first?.value?.let { IETFUtils.valueToString(it) }
}.getOrNull()
