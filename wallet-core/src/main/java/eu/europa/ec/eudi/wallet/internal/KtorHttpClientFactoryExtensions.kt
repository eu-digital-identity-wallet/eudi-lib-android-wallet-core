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
 * Wraps the [HttpClient] with a logging interceptor.
 * @receiver the [HttpClient] factory
 * @param libraryLogger the logger
 * @return the wrapped [HttpClient] factory
 */
@JvmSynthetic
internal fun (() -> HttpClient).wrappedWithLogging(libraryLogger: Logger?): (() -> HttpClient) {
    return if (libraryLogger != null) {
        {
            val ktorLogger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    libraryLogger.d(TAG, message)
                }
            }
            this().let { client ->
                client.config {
                    install(Logging) {
                        logger = ktorLogger
                        level = LogLevel.ALL
                    }
                }
            }
        }
    } else this
}

@JvmSynthetic
internal fun (() -> HttpClient).wrappedWithContentNegotiation(): (() -> HttpClient) {
    return {
        val jsonSupport = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
        this().let { client ->
            client.config {
                install(ContentNegotiation) {
                    json(
                        json = jsonSupport,
                    )
                }
            }
        }
    }
}