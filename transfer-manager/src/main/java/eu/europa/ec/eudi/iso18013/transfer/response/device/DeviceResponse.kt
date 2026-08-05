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

package eu.europa.ec.eudi.iso18013.transfer.response.device

import eu.europa.ec.eudi.iso18013.transfer.DeviceResponseBytes
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.wallet.document.DocumentId

/**
 * Represents a Device Response according to ISO 18013-5 standard.
 * @property deviceResponseBytes the device response bytes
 * @property sessionTranscriptBytes the session transcript bytes
 * @property documentIds the list of document ids in response indexed as positioned in CBOR array in responseBytes
 */
data class DeviceResponse(
    val deviceResponseBytes: DeviceResponseBytes,
    val sessionTranscriptBytes: ByteArray,
    val documentIds: List<DocumentId>
) : Response {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceResponse

        if (!deviceResponseBytes.contentEquals(other.deviceResponseBytes)) return false
        if (!sessionTranscriptBytes.contentEquals(other.sessionTranscriptBytes)) return false
        if (documentIds != other.documentIds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deviceResponseBytes.contentHashCode()
        result = 31 * result + sessionTranscriptBytes.contentHashCode()
        result = 31 * result + documentIds.hashCode()
        return result
    }

}