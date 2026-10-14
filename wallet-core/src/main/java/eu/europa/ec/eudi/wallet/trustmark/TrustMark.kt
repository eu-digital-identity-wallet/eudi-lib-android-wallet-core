/*
 * Copyright (c) 2025-2026 European Commission
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

package eu.europa.ec.eudi.wallet.trustmark

/**
 * Combined Trust Mark data containing both the configuration information and
 * the fetched resource (logo and localised text).
 *
 * Returned by [TrustMarkManager.getTrustMark] as a single result combining
 * the [TrustMarkInformation] (URLs, QR codes) with the [TrustMarkResource]
 * (image and localised text) fetched from the EC-hosted endpoint.
 *
 * @property information The Trust Mark configuration (URLs, optional QR codes).
 * @property resource The Trust Mark resource (logo and localised user text).
 */
data class TrustMark(
    val information: TrustMarkInformation,
    val resource: TrustMarkResource,
)
