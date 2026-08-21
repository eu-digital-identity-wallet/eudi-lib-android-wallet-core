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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * A multi-language string. Both `content` and `value` are accepted as the text field.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
internal data class MultiLangDto(
    val lang: String,
    val content: String? = null,
    val value: String? = null,
) {
    val text: String get() = content ?: value ?: ""
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
internal data class CredentialDto(
    val format: String,
    val meta: JsonObject? = null,
    val claim: List<ClaimDto> = emptyList(),
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
internal data class ClaimDto(
    val path: List<JsonPrimitive> = emptyList(),
    val values: List<JsonPrimitive>? = null,
)

internal fun MultiLangDto.toLocalizedText(): LocalizedText =
    LocalizedText(language = lang, value = text)

internal fun CredentialDto.toRegisteredCredential(): RegisteredCredential =
    RegisteredCredential(
        format = format,
        meta = meta.toCredentialMeta(),
        claims = claim.map { it.toRegisteredClaim() },
    )

internal fun CredentialDto.toProvidedAttestation(): ProvidedAttestation =
    ProvidedAttestation(
        format = format,
        meta = meta.toCredentialMeta(),
    )

private fun ClaimDto.toRegisteredClaim(): RegisteredClaim =
    RegisteredClaim(
        path = path.map { it.toClaimPathElement() },
        values = values?.map { it.content },
    )

/**
 * Reads one element of a registered claim path: `null` is the array wildcard, a non-negative integer
 * is an array index, and anything else is a claim name.
 */
private fun JsonPrimitive.toClaimPathElement(): ClaimPathElement = when {
    this is JsonNull -> ClaimPathElement.AllArrayElements
    !isString -> content.toIntOrNull()
        ?.takeIf { it >= 0 }
        ?.let { ClaimPathElement.ArrayElement(it) }
        ?: ClaimPathElement.Claim(content)

    else -> ClaimPathElement.Claim(content)
}

private fun JsonObject?.toCredentialMeta(): CredentialMeta? {
    if (this == null) return null
    return credentialMetaOrNull(
        doctypeValue = this["doctype_value"]?.jsonPrimitive?.contentOrNull,
        vctValues = this["vct_values"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull },
    )
}
