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

package eu.europa.ec.eudi.wallet.internal

import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager.Companion.TAG
import eu.europa.ec.eudi.wallet.logging.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Derives a new [HttpClient] from this one with wallet-core's standard plugins installed:
 * JSON content negotiation (lenient) and, when a [libraryLogger] is provided, Ktor request/response
 * logging.
 *
 * All plugins are composed in a single [HttpClient.config] call so that at most one intermediate
 * client is created (the receiver), rather than one per plugin.
 *
 * @receiver the base [HttpClient] to derive from
 * @param libraryLogger optional logger; when non-null, Ktor [Logging] is installed at [LogLevel.ALL]
 * @return a derived [HttpClient] with the plugins installed
 */
@JvmSynthetic
internal fun HttpClient.withWalletKtorPlugins(libraryLogger: Logger?): HttpClient {
    return this.config {
        libraryLogger?.let { log ->
            val ktorLogger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    log.d(TAG, message)
                }
            }
            install(Logging) {
                logger = ktorLogger
                level = LogLevel.ALL
            }
        }
        install(ContentNegotiation) {
            json(
                json = Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                },
            )
        }
    }
}
