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

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An identifier in structured form: the scheme [type] as a URI and the bare [value]. Derived from a
 * raw [RegistrationIdentifier] via [structuredIdentifier].
 *
 * @property type the identifier scheme as a URI (for example [LEI], [EUID]).
 * @property value the bare identifier value.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class QualifiedIdentifier(
    val type: String,
    @SerialName("identifier") val value: String,
) {
    companion object {
        const val EORI = "http://data.europa.eu/eudi/id/EORI-No"
        const val LEI = "http://data.europa.eu/eudi/id/LEI"
        const val EUID = "http://data.europa.eu/eudi/id/EUID"
        const val VATIN = "http://data.europa.eu/eudi/id/VATIN"
        const val TIN = "http://data.europa.eu/eudi/id/TIN"
        const val EXCISE = "http://data.europa.eu/eudi/id/Excise"
    }
}

/**
 * The registration certificate's identifier in structured form: each raw identifier's scheme prefix
 * is decoded to a scheme URI (ETSI TS 119 475 Table 2) and paired with the bare value. Returns the
 * first identifier with a recognized scheme, or null if none is recognized.
 */
fun RegistrationCertificate.structuredIdentifier(): QualifiedIdentifier? =
    identifiers.firstNotNullOfOrNull { it.value.toQualifiedIdentifierOrNull() }

/**
 * Decodes an ETSI EN 319 412-1 semantic identifier: its first three characters map to a scheme URI
 * per ETSI TS 119 475 Table 2 (`VAT` maps to [QualifiedIdentifier.VATIN]). An unrecognized prefix
 * returns null.
 */
private fun String.toQualifiedIdentifierOrNull(): QualifiedIdentifier? {
    val type = when (take(3).uppercase()) {
        "LEI" -> QualifiedIdentifier.LEI
        "VAT" -> QualifiedIdentifier.VATIN
        "NTR" -> QualifiedIdentifier.EUID
        "EOR" -> QualifiedIdentifier.EORI
        "EXC" -> QualifiedIdentifier.EXCISE
        else -> return null
    }
    // The bare identifier is the part after the scheme prefix, e.g. `LEIXG-529900…` -> `529900…`.
    return QualifiedIdentifier(type = type, value = substringAfter('-', this))
}
