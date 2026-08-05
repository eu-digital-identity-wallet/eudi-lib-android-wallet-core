/*
 * Copyright (c) 2024 European Commission
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

import eu.europa.ec.eudi.iso18013.transfer.DeviceRequestBytes
import eu.europa.ec.eudi.iso18013.transfer.SessionTranscriptBytes
import eu.europa.ec.eudi.iso18013.transfer.response.Request

/**
 * Represents a Device Request according to ISO 18013-5 standard.
 * @property deviceRequestBytes the device request bytes
 * @property sessionTranscriptBytes the session transcript bytes
 */
data class DeviceRequest(
    val deviceRequestBytes: DeviceRequestBytes,
    val sessionTranscriptBytes: SessionTranscriptBytes
) : Request