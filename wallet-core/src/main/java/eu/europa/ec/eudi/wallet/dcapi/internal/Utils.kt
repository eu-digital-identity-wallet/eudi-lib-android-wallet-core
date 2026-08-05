/*
 * Copyright (c) 2024-2025 European Commission
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

@file:JvmMultifileClass
package eu.europa.ec.eudi.wallet.dcapi.internal

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.scale
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.registry.provider.selectedCredentialSet
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol
import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSet
import org.multipaz.presentment.CredentialPresentmentSetOption
import org.multipaz.presentment.CredentialPresentmentSetOptionMember
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Locale
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject
import org.multipaz.util.toBase64

private const val SHA_256_ALGORITHM = "SHA-256"

/**
 * Utility functions for DCAPI.
 */

internal fun Context.getAppName(): String = this.applicationContext.applicationInfo.loadLabel(packageManager).toString()

internal fun Context.getLocale(): Locale = this.resources.configuration.locales[0]

internal fun Context.getMatcher(fileName: String): ByteArray {
    return this.assets.open(fileName).use { stream ->
        ByteArray(stream.available()).apply {
            stream.read(this)
        }
    }
}

internal fun Context.getDefaultPrivilegedUserAgents(): String {
    return this.assets.open("privilegedUserAgents.json").use { stream ->
        stream.readBytes().decodeToString()
    }
}

internal fun Bitmap.getIconBytes(): ByteArray {
    val scaledIcon = this.scale(48, 48)
    return ByteArrayOutputStream().use { stream ->
        scaledIcon.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.toByteArray()
    }
}

@OptIn(ExperimentalEncodingApi::class)
internal fun String.fromBase64(): ByteArray = kotlin.io.encoding.Base64.UrlSafe.withPadding(kotlin.io.encoding.Base64.PaddingOption.ABSENT_OPTIONAL).decode(this)

@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.toBase64():  String = kotlin.io.encoding.Base64.UrlSafe.encode(this).trimEnd('=')

/**
 * Generates a session transcript for the protocol `org-iso-mdoc`,
 * according to the ISO/IEC TS 18013-7:2025 Annex C.
 *
 * SessionTranscript = [
 *    null,
 *    null,
 *    [
 *       "dcapi",
 *       dcapiInfoHash
 *     ]
 * ]
 * dcapiInfo = [Base64EncryptionInfo, SerializedOrigin]
 * SerializedOrigin = tstr
 * dcapiInfoHash = bstr
 *
 * @param encryptionInfoBase64 Base64 encoded encryption information.
 * @param origin Serialized origin of the request.
 * @return ByteArray representing the session transcript.
 */

internal fun getDCAPIIsoMdocSessionTranscript(encryptionInfoBase64: String, origin: String) : ByteArray {
    val dcapiInfo =
        CBORObject.NewArray().apply {
            Add(encryptionInfoBase64)
            Add(origin)
        }.EncodeToBytes()
    val dcapiInfoHash = MessageDigest.getInstance(SHA_256_ALGORITHM).digest(dcapiInfo)
    val dcapiIsoMdocHandover = CBORObject.NewArray().apply {
        Add(DCAPI)
        Add(dcapiInfoHash)
    }
    val sessionTranscript = CBORObject.NewArray().apply {
        Add(CBORObject.Null)
        Add(CBORObject.Null)
        Add(dcapiIsoMdocHandover)
    }
    return sessionTranscript.EncodeToBytes()
}

/**
 * Calculates the origin for a native Android app,
 * in the form "android:apk-key-hash:<encoded SHA 256 fingerprint>"
 * https://developer.android.com/identity/digital-credentials/credential-holder/credential-holder#check-verifier-origin
 *
 * @param appSigningInfo the signing information for the native Android app
 * @return the origin as a string
 */
internal fun getAppOrigin(appSigningInfo: ByteArray): String {
    val digest = MessageDigest.getInstance(SHA_256_ALGORITHM)
    val appSigningInfoHash = digest.digest(appSigningInfo).toBase64()
    return "android:apk-key-hash:$appSigningInfoHash"
}

/**
 * Resolves the verifier origin for a DC API request, per
 * https://developer.android.com/identity/digital-credentials/credential-holder/credential-holder#check-verifier-origin
 *
 * A privileged caller (e.g. a trusted browser) may act on behalf of another verifier by
 * setting an origin; [CallingAppInfo.getOrigin] returns it only when the caller's package
 * and signature match [privilegedAllowlist]. Otherwise, the request is from a native app and
 * the origin is derived from the signing certificate as `android:apk-key-hash:<sha256>`.
 */
@OptIn(ExperimentalDigitalCredentialApi::class)
internal fun ProviderGetCredentialRequest.resolveOrigin(privilegedAllowlist: String): String =
    callingAppInfo.getOrigin(privilegedAllowlist)
        ?: getAppOrigin(callingAppInfo.signingInfoCompat.signingCertificateHistory[0].toByteArray())

/**
 * The raw request JSON of this DC API request (the platform-provided
 * [GetDigitalCredentialOption.requestJson]), or null when the request carries no such option.
 * Intended for debug logging.
 */
@OptIn(ExperimentalDigitalCredentialApi::class)
internal fun ProviderGetCredentialRequest.requestJsonOrNull(): String? =
    (credentialOptions.firstOrNull() as? GetDigitalCredentialOption)?.requestJson

/**
 * The document ids the OS credential picker selected for this request.
 *
 * The selection is reported on [selectedCredentialSet]. Each selected entry id has the form
 * `"<credentialSetId> <documentId>"`; removing the shared [credentialSetId] prefix yields the
 * registered document id. Returns every selected id, since a single selection may cover more than
 * one document, and an empty set when nothing was selected.
 */
@OptIn(ExperimentalDigitalCredentialApi::class)
internal fun ProviderGetCredentialRequest.selectedDocumentIds(): Set<String> {
    val set = selectedCredentialSet ?: return emptySet()
    val prefix = set.credentialSetId
    return set.credentials
        .map { it.credentialId.removePrefix(prefix).trim() }
        .filter { it.isNotEmpty() }
        .toSet()
}

/**
 * The protocol the OS credential picker selected for this request.
 *
 * It is encoded in [selectedCredentialSet].credentialSetId as `"<credentialSetId> <protocol>"`
 * (for example `"0 openid4vp-v1-signed"`). Returns null when nothing was selected.
 */
@OptIn(ExperimentalDigitalCredentialApi::class)
internal fun ProviderGetCredentialRequest.selectedProtocol(): String? =
    selectedCredentialSet?.credentialSetId?.split(" ")?.getOrNull(1)?.takeIf { it.isNotEmpty() }

/**
 * The `requests[]` entry the wallet will process: its [protocol] and its [index] in the
 * `requests[]` array, so the caller can read the matching `data` at `requests[index]`.
 */
internal data class ResolvedDcApiRequest(val protocol: String, val index: Int)

/**
 * Resolves which entry of the request's `requests[]` array the wallet will process, returned as a
 * [ResolvedDcApiRequest] (the chosen protocol and its index). A Digital Credential API request may
 * offer the same exchange under several alternative protocols; this picks the one to honour:
 *  1. The protocol the OS picker selected ([selectedProtocol]), when it is in [supportedProtocols].
 *  2. Otherwise, the first offered protocol that is in [supportedProtocols]. This covers the case
 *     where the picker selected a protocol the wallet does not support but a supported alternative
 *     is offered for the same request. The consent screen and the per-document filter remain the
 *     disclosure gate, so the wrong data is never released.
 *
 * @throws IllegalStateException when nothing was selected, the request has no
 *   `GetDigitalCredentialOption` or no `requests[]` array, or no offered protocol is in
 *   [supportedProtocols].
 */
@OptIn(ExperimentalDigitalCredentialApi::class)
internal fun ProviderGetCredentialRequest.resolveDcApiRequest(
    supportedProtocols: List<DCAPIProtocol>,
): ResolvedDcApiRequest {
    val chosen = selectedProtocol()
        ?: error("No protocol selected by the OS picker (selectedCredentialSet is missing/empty)")
    val option = credentialOptions.firstOrNull() as? GetDigitalCredentialOption
        ?: error("DC API request has no GetDigitalCredentialOption")
    val requests = JSONObject(option.requestJson).optJSONArray(REQUESTS)
        ?: error("DC API request has no requests[] array")
    val protocols = (0 until requests.length()).map { requests.getJSONObject(it).optString(PROTOCOL) }
    // Boundary: compare the on-the-wire protocol strings against the supported set's identifiers.
    val supportedIds = supportedProtocols.map { it.identifier }

    // 1. Selected protocol, if we support it and it is present in requests[].
    if (chosen in supportedIds) {
        val index = protocols.indexOf(chosen)
        if (index >= 0) return ResolvedDcApiRequest(chosen, index)
    }
    // 2. Fallback: first offered protocol we support (alternatives, per the DC API spec).
    protocols.indexOfFirst { it in supportedIds }
        .takeIf { it >= 0 }
        ?.let { return ResolvedDcApiRequest(protocols[it], it) }
    // 3. No supported alternatives.
    error("Selected protocol '$chosen' is not supported and requests[] offers no supported alternative: $protocols")
}

/**
 * Walks a [CredentialPresentmentData] tree and keeps only matches whose underlying
 * `Credential.document.identifier` is in [credentialIds]. Empty members / options / sets
 * are pruned, so the result contains exactly the requested document(s).
 */
internal fun CredentialPresentmentData.filterByCredentialIds(
    credentialIds: Set<String>,
): CredentialPresentmentData {
    val sets = credentialSets.mapNotNull { set ->
        val options = set.options.mapNotNull { option ->
            val members = option.members.map { member ->
                CredentialPresentmentSetOptionMember(
                    matches = member.matches.filter {
                        it.credential.document.identifier in credentialIds
                    }
                )
            }
            if (members.any { it.matches.isEmpty() }) null
            else CredentialPresentmentSetOption(members = members)
        }
        if (options.isEmpty()) null
        else CredentialPresentmentSet(optional = false, options = options)
    }
    return CredentialPresentmentData(sets)
}

/**
 * Wraps a DC API response payload as `{ "protocol": <protocol>, "data": <data> }` inside a
 * [DigitalCredential] result [Intent].
 */
@OptIn(ExperimentalDigitalCredentialApi::class)
internal fun createDcApiResponseIntent(protocol: String, data: JsonObject): Intent {
    val credentialJson = buildJsonObject {
        put(PROTOCOL, protocol)
        put(DATA, data)
    }
    val resultData = Intent()
    PendingIntentHandler.setGetCredentialResponse(
        resultData,
        GetCredentialResponse(DigitalCredential(credentialJson.toString()))
    )
    return resultData
}