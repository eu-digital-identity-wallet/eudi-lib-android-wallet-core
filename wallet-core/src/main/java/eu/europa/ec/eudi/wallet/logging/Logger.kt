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

package eu.europa.ec.eudi.wallet.logging

import androidx.annotation.IntDef
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import java.time.Instant

/**
 * Logger interface for the EudiWallet
 *
 * Implementors can provide their own logging mechanism by implementing this interface
 */
fun interface Logger {

    /**
     * Log record
     * @property level the log level
     * @property instant the instant the log was created
     * @property message the log message
     * @property thrown the throwable that was thrown
     * @property sourceClassName the source class name
     * @property sourceMethod the source method name
     */
    data class Record(
        @Level val level: Int,
        val instant: Instant = Instant.now(),
        val message: String,
        val thrown: Throwable? = null,
        val sourceClassName: String? = null,
        val sourceMethod: String? = null,
    )

    /**
     * Log a record
     * @param record the record to log
     * @see Record
     */
    fun log(record: Record)

    /**
     * Companion object for the [Logger] interface
     *
     * @property OFF the log level OFF (no logging)
     * @property LEVEL_ERROR the log level ERROR (only errors)
     * @property LEVEL_INFO the log level INFO (errors and info)
     * @property LEVEL_DEBUG the log level DEBUG (errors, info and debug)
     */
    companion object {
        const val OFF = 0
        const val LEVEL_ERROR = 1
        const val LEVEL_INFO = 2
        const val LEVEL_DEBUG = 3

        /**
         * Construct a default instance of [Logger] with the provided [EudiWalletConfig]
         */
        operator fun invoke(config: EudiWalletConfig): Logger =
            LoggerImpl(config.logLevel, config.logSizeLimit)
    }

    /**
     * Log level annotation for configuring logging with [EudiWalletConfig.configureLogging]
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [OFF, LEVEL_ERROR, LEVEL_INFO, LEVEL_DEBUG])
    annotation class Level
}


