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

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

/**
 * Data model for the EUDI Wallet Trust Mark Resource as defined in
 * EC TS01 v1.2 (2026-06) — Specification of EUDI Wallet Trust Mark.
 *
 * This object is fetched from the [TrustMarkInformation.trustMarkResourceURL] and
 * contains the official Trust Mark graphics and localised user information for rendering
 * in the Wallet user interface.
 *
 * JSON schema: `http://data.europa.eu/eudi/TrustMarkResource.json`
 *
 * @property image The Trust Mark image resource (logo).
 * @property text The Trust Mark text resource with localised strings.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class TrustMarkResource(
    val image: Image,
    val text: Text,
) {

    /**
     * The Trust Mark image resource.
     *
     * @property name The filename or identifier for the image (e.g. `"eudi-wallet-trustmark-logo.png"`).
     * @property url The relative path or full URL to the image.
     */
    @Serializable
    data class Image(
        val name: String,
        val url: String,
    )

    /**
     * The Trust Mark text resource with localised strings.
     *
     * @property name Description of the text resource purpose.
     * @property localisations A map of language codes (e.g. `"en"`, `"fr"`, `"de"`) to
     *   localised user-facing text strings. All official EU languages should be present
     *   in a production deployment.
     */
    @Serializable
    data class Text(
        val name: String,
        val localisations: Map<String, String>,
    )
}
