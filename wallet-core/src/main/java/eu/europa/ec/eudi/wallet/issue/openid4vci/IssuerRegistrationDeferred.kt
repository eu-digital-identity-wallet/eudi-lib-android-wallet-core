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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.StoredDeferredContext
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.StoredIssuerRegistration
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationIdentifier
import kotlinx.serialization.json.Json

/**
 * The subset of the credential issuer's registration the transaction log needs, or null when this
 * outcome carries no parsed certificate. A failed-but-parsed result still contributes its registration.
 */
internal fun RegistrationCertificateResult?.toStoredIssuerRegistration(): StoredIssuerRegistration? {
    val registration = when (this) {
        is RegistrationCertificateResult.Verified -> registration
        is RegistrationCertificateResult.Failed -> registration
        null -> null
    } ?: return null
    return StoredIssuerRegistration(
        name = registration.name,
        legalName = registration.legalName,
        givenName = registration.givenName,
        familyName = registration.familyName,
        identifiers = registration.identifiers.map { it.value },
        entitlements = registration.entitlements,
        country = registration.country,
        infoUri = registration.infoUri,
        supportUri = registration.supportUri,
    )
}

/**
 * The stored subset as a [RegistrationCertificate], so a resolved deferred credential is mapped the
 * same way as a synchronous one. Fields that were not captured keep their defaults.
 */
internal fun StoredIssuerRegistration.toRegistrationCertificate(): RegistrationCertificate =
    RegistrationCertificate(
        identifiers = identifiers.map { RegistrationIdentifier(it) },
        name = name,
        legalName = legalName,
        givenName = givenName,
        familyName = familyName,
        country = country,
        entitlements = entitlements,
        infoUri = infoUri,
        supportUri = supportUri,
    )

/**
 * The credential issuer's registration captured with this deferred document at issuance, or null when
 * none was captured. The certificate itself is gone by the time the credential resolves (TS10 §3.5).
 */
internal fun DeferredDocument.storedIssuerRegistration(): RegistrationCertificate? =
    storedContext()?.interactingParty?.toRegistrationCertificate()

/**
 * Whether the User started the issuance this deferred credential came from (TS10 §3.5
 * `isUserTriggered`), or null when it was not captured.
 */
internal fun DeferredDocument.storedIsUserTriggered(): Boolean? = storedContext()?.isUserTriggered

/**
 * The issuance context stored with this deferred document, or null when it cannot be read. Reading
 * never throws, since it only feeds logging.
 */
private fun DeferredDocument.storedContext(): StoredDeferredContext? = runCatching {
    Json.decodeFromString<StoredDeferredContext>(String(relatedData, Charsets.UTF_8))
}.getOrNull()
