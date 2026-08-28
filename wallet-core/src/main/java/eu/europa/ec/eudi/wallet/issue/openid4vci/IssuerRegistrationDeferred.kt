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
 * The log-relevant subset of the credential issuer's registration, or null when this outcome carries no
 * parsed certificate. Both a verified and a failed-but-parsed result contribute their registration, so a
 * deferred credential is named after whatever registration was seen at issuance.
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
    )
}

/**
 * The stored subset as a [RegistrationCertificate] holding only the interacting-party fields, so the
 * transaction log maps a resolved deferred credential the same way as a synchronous one. The fields not
 * captured at deferral keep their defaults.
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
    )

/**
 * The credential issuer's registration captured with this deferred document at issuance, or null when
 * none was captured or the stored data cannot be read. The certificate itself is gone by the time the
 * credential resolves, so this is how the transaction log names the issuer of a resolved deferred
 * credential (TS10 §3.5). Reading never throws, since it only feeds logging.
 */
internal fun DeferredDocument.storedIssuerRegistration(): RegistrationCertificate? =
    runCatching {
        Json.decodeFromString<StoredDeferredContext>(String(relatedData, Charsets.UTF_8))
            .interactingParty
            ?.toRegistrationCertificate()
    }.getOrNull()
