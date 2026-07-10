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
import eu.europa.ec.eudi.wallet.registration.Intermediary
import eu.europa.ec.eudi.wallet.registration.SupervisoryAuthority
import eu.europa.ec.eudi.wallet.registration.RegistrationIdentifier
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate

import android.annotation.SuppressLint
import eu.europa.ec.eudi.statium.StatusIndex
import eu.europa.ec.eudi.statium.StatusReference
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * The payload claims of a Wallet-Relying Party Registration Certificate in JWT form (ETSI TS 119 475
 * clause 5.2.4). The claims map directly onto [RegistrationCertificate].
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
internal data class WrprcPayloadDto(
    val sub: String? = null,
    val name: String? = null,
    @SerialName("sub_ln") val legalName: String? = null,
    @SerialName("sub_gn") val givenName: String? = null,
    @SerialName("sub_fn") val familyName: String? = null,
    val country: String? = null,
    @SerialName("srv_description") val serviceDescription: List<MultiLangDto> = emptyList(),
    val entitlements: List<String> = emptyList(),
    @SerialName("registry_uri") val registryUri: String? = null,
    @SerialName("privacy_policy") val privacyPolicy: String? = null,
    @SerialName("info_uri") val infoUri: String? = null,
    @SerialName("support_uri") val supportUri: String? = null,
    @SerialName("supervisory_authority") val supervisoryAuthority: SupervisoryAuthorityDto? = null,
    @SerialName("policy_id") val policyIds: List<String> = emptyList(),
    @SerialName("certificate_policy") val certificatePolicy: String? = null,
    val iat: Long? = null,
    val exp: Long? = null,
    val status: StatusDto? = null,
    @SerialName("intended_use_id") val intendedUseId: String? = null,
    val purpose: List<MultiLangDto> = emptyList(),
    val credentials: List<CredentialDto> = emptyList(),
    @SerialName("provides_attestations") val providedAttestations: List<CredentialDto> = emptyList(),
    val intermediary: IntermediaryDto? = null,
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
internal data class SupervisoryAuthorityDto(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val uri: String? = null,
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
internal data class StatusDto(
    @SerialName("status_list") val statusList: StatusListDto? = null,
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
internal data class StatusListDto(
    val idx: Int,
    val uri: String,
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
internal data class IntermediaryDto(
    val sub: String? = null,
    // The intermediary common name is carried in the `sname` claim, not `name`.
    @SerialName("sname") val name: String? = null,
)

/**
 * Maps the verified registration certificate payload to the domain [RegistrationCertificate].
 */
internal fun WrprcPayloadDto.toWrpRegistration(): RegistrationCertificate =
    RegistrationCertificate(
        identifiers = listOfNotNull(sub?.let { RegistrationIdentifier(value = it) }),
        name = name,
        legalName = legalName,
        givenName = givenName,
        familyName = familyName,
        country = country,
        serviceDescription = serviceDescription.map { it.toLocalizedText() },
        entitlements = entitlements,
        registryUri = registryUri,
        privacyPolicyUri = privacyPolicy,
        infoUri = infoUri,
        supportUri = supportUri,
        supervisoryAuthority = supervisoryAuthority?.let {
            SupervisoryAuthority(name = it.name, email = it.email, phone = it.phone, uri = it.uri)
        },
        policyIds = policyIds,
        certificatePolicyUri = certificatePolicy,
        issuedAt = iat?.let { Instant.fromEpochSeconds(it) },
        expiresAt = exp?.let { Instant.fromEpochSeconds(it) },
        status = status?.statusList?.let { StatusReference(uri = it.uri, index = StatusIndex(it.idx)) },
        intendedUseId = intendedUseId,
        purpose = purpose.map { it.toLocalizedText() },
        requestedCredentials = credentials.map { it.toRegisteredCredential() },
        providedAttestations = providedAttestations.map { it.toProvidedAttestation() },
        intermediary = intermediary?.sub?.let { Intermediary(identifier = it, name = intermediary.name) },
    )
