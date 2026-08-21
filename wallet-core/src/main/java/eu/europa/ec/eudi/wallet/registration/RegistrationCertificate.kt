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

import eu.europa.ec.eudi.statium.StatusReference
import kotlin.time.Instant

/**
 * Registration information about a relying party, as described by a Wallet-Relying Party
 * Registration Certificate (WRPRC). Follows ETSI TS 119 475 clause 5.2.4.
 *
 * @property identifiers the relying party's identifiers, each with an optional type
 * @property name the user-friendly name of the relying party (WRPRC `name`)
 * @property legalName the registered legal name of a legal person (WRPRC `sub_ln`)
 * @property givenName the given name of a natural person (WRPRC `sub_gn`)
 * @property familyName the family name of a natural person (WRPRC `sub_fn`)
 * @property country the country code of the relying party (WRPRC `country`)
 * @property serviceDescription localized descriptions of the service (WRPRC `srv_description`)
 * @property entitlements the entitlement identifiers the relying party is registered for (WRPRC `entitlements`)
 * @property registryUri the URI of the Registrar API holding this registration (WRPRC `registry_uri`)
 * @property privacyPolicyUri the privacy policy URI for the intended use (WRPRC `privacy_policy`)
 * @property infoUri the URI of additional information about the relying party (WRPRC `info_uri`)
 * @property supportUri the support and data-deletion contact URI (WRPRC `support_uri`)
 * @property supervisoryAuthority the competent data protection supervisory authority (WRPRC `supervisory_authority`)
 * @property policyIds the object identifiers of the applicable certificate policies (WRPRC `policy_id`)
 * @property certificatePolicyUri the URI of the certificate policy (WRPRC `certificate_policy`)
 * @property issuedAt the time the certificate was issued (WRPRC `iat`)
 * @property expiresAt the time the certificate expires (WRPRC `exp`)
 * @property status the status list reference for revocation (WRPRC `status`)
 * @property intendedUseId the registrar-provided identifier of the intended use (WRPRC `intended_use_id`)
 * @property purpose localized descriptions of the registered purposes (WRPRC `purpose`)
 * @property requestedCredentials the attestations and claims the relying party may request (WRPRC `credentials`)
 * @property providedAttestations the attestations the relying party may issue (WRPRC `provides_attestations`)
 * @property intermediary the intermediary presenting the request on behalf of the relying party (WRPRC `intermediary`)
 */
data class RegistrationCertificate(
    val identifiers: List<RegistrationIdentifier> = emptyList(),
    val name: String? = null,
    val legalName: String? = null,
    val givenName: String? = null,
    val familyName: String? = null,
    val country: String? = null,
    val serviceDescription: List<LocalizedText> = emptyList(),
    val entitlements: List<String> = emptyList(),
    val registryUri: String? = null,
    val privacyPolicyUri: String? = null,
    val infoUri: String? = null,
    val supportUri: String? = null,
    val supervisoryAuthority: SupervisoryAuthority? = null,
    val policyIds: List<String> = emptyList(),
    val certificatePolicyUri: String? = null,
    val issuedAt: Instant? = null,
    val expiresAt: Instant? = null,
    val status: StatusReference? = null,
    val intendedUseId: String? = null,
    val purpose: List<LocalizedText> = emptyList(),
    val requestedCredentials: List<RegisteredCredential> = emptyList(),
    val providedAttestations: List<ProvidedAttestation> = emptyList(),
    val intermediary: Intermediary? = null
)

/**
 * An identifier of the relying party as recorded in a national register. Corresponds to the
 * WRPRC `Identifier` structure.
 *
 * @property type the identifier type as a URI (for example LEI, EUID or VATIN), when qualified
 * @property value the identifier value
 */
data class RegistrationIdentifier(
    val type: String? = null,
    val value: String
)

/**
 * A piece of text in a specific language. Corresponds to a WRPRC multi-language string.
 *
 * @property language the language tag
 * @property value the text in the given language
 */
data class LocalizedText(
    val language: String,
    val value: String
)

/**
 * The competent supervisory authority for data protection registered for the relying party.
 * Corresponds to the WRPRC `supervisory_authority` claim.
 *
 * @property name the name of the supervisory authority
 * @property email the contact email address of the supervisory authority
 * @property phone the contact phone number of the supervisory authority
 * @property uri a contact URI of the supervisory authority
 */
data class SupervisoryAuthority(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val uri: String? = null
)

/**
 * An attestation type the relying party is registered to request, together with the claims it may
 * request from it. Corresponds to an entry of the WRPRC `credentials` claim.
 *
 * @property format the attestation format, for example `dc+sd-jwt` or `mso_mdoc`
 * @property meta the properties that identify the attestation type
 * @property claims the claims the relying party is registered to request; an empty list means all claims
 */
data class RegisteredCredential(
    val format: String,
    val meta: CredentialMeta? = null,
    val claims: List<RegisteredClaim> = emptyList()
)

/**
 * A claim the relying party is registered to request, identified by its path.
 * Corresponds to an entry of the WRPRC `claim` array.
 *
 * @property path the path to the claim, following the OpenID4VP DCQL claims path syntax
 * @property values the expected values of the claim, when registered
 */
data class RegisteredClaim(
    val path: List<ClaimPathElement>,
    val values: List<String>? = null
)

/**
 * An attestation type the relying party is registered to issue. Corresponds to an entry of the
 * WRPRC `provides_attestations` claim.
 *
 * @property format the attestation format, for example `dc+sd-jwt` or `mso_mdoc`
 * @property meta the properties that identify the attestation type
 */
data class ProvidedAttestation(
    val format: String,
    val meta: CredentialMeta? = null
)

/**
 * Properties that identify an attestation type within a [RegisteredCredential] or a
 * [ProvidedAttestation]. Corresponds to the WRPRC `meta` object.
 *
 * @property vctValues the accepted verifiable credential types, for the `dc+sd-jwt` format
 * @property doctypeValue the document type, for the `mso_mdoc` format
 */
data class CredentialMeta(
    val vctValues: List<String>? = null,
    val doctypeValue: String? = null
)

/**
 * The [CredentialMeta] for the given properties, or null when neither identifies an attestation type.
 */
internal fun credentialMetaOrNull(
    doctypeValue: String? = null,
    vctValues: List<String>? = null,
): CredentialMeta? =
    if (doctypeValue == null && vctValues.isNullOrEmpty()) {
        null
    } else {
        CredentialMeta(vctValues = vctValues, doctypeValue = doctypeValue)
    }

/**
 * An intermediary presenting a request on behalf of the relying party. Corresponds to the WRPRC
 * `intermediary` claim.
 *
 * @property identifier the intermediary's unique identifier (WRPRC `intermediary.sub`)
 * @property name the user-friendly name of the intermediary (WRPRC `intermediary.name`)
 */
data class Intermediary(
    val identifier: String,
    val name: String? = null
)