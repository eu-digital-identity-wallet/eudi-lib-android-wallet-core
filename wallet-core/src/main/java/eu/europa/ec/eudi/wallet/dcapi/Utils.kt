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
package eu.europa.ec.eudi.wallet.dcapi

import android.content.Context
import android.graphics.Bitmap
import com.upokecenter.cbor.CBORObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Locale
import kotlin.io.encoding.ExperimentalEncodingApi
import androidx.core.graphics.scale

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
