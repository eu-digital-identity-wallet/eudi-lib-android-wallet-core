/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.transactionLogging.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

/**
 * An identifier of a legal entity from an official record (TS02 §2.8.2).
 *
 * @property type URI of the identifier scheme (e.g. [EUID], [LEI]).
 * @property identifier the identifier value itself.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Identifier(
    val type: String,
    val identifier: String,
) {
    companion object {
        const val EORI = "http://data.europa.eu/eudi/id/EORI-No"
        const val LEI = "http://data.europa.eu/eudi/id/LEI"
        const val EUID = "http://data.europa.eu/eudi/id/EUID"
        const val VATIN = "http://data.europa.eu/eudi/id/VATIN"
        const val TIN = "http://data.europa.eu/eudi/id/TIN"
        const val EXCISE = "http://data.europa.eu/eudi/id/Excise"
    }
}
