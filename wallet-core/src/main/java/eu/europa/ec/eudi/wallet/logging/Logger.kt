/*
 *  Copyright (c) 2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet.logging

import android.util.Log
import androidx.annotation.IntDef
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.logging.Logger.Companion.LEVEL_DEBUG
import eu.europa.ec.eudi.wallet.logging.Logger.Companion.LEVEL_ERROR
import eu.europa.ec.eudi.wallet.logging.Logger.Companion.LEVEL_INFO


interface Logger {

    @Level
    val level: Int
    fun log(level: Int, tag: String, message: String, throwable: Throwable? = null)

    fun d(tag: String, message: String) = log(LEVEL_DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LEVEL_INFO, tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LEVEL_ERROR, tag, message, throwable)

    companion object {
        const val OFF = 0
        const val LEVEL_ERROR = 1
        const val LEVEL_INFO = 2
        const val LEVEL_DEBUG = 3
        operator fun invoke(config: EudiWalletConfig): Logger = LoggerImpl(config)
    }

    /**
     * Log level for the OpenId4Vci issuer
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [OFF, LEVEL_ERROR, LEVEL_INFO, LEVEL_DEBUG])
    annotation class Level
}

class LoggerImpl(val config: EudiWalletConfig, private val maxLogSize: Int = 1000) : Logger {
    override val level: Int = config.logLevel
    override fun log(level: Int, tag: String, message: String, throwable: Throwable?) {
        splitMessage(message).forEachIndexed { i, m ->
            when {
                level == LEVEL_ERROR && config.logLevel >= LEVEL_ERROR && i == 0 && throwable != null -> Log.e(
                    tag,
                    m,
                    throwable
                )

                level == LEVEL_ERROR && config.logLevel >= LEVEL_ERROR -> Log.e(tag, m)
                level == LEVEL_INFO && config.logLevel >= LEVEL_INFO -> Log.i(tag, m)
                level == LEVEL_DEBUG && config.logLevel >= LEVEL_DEBUG -> Log.d(tag, m)
            }
        }
    }

    private fun splitMessage(message: String): List<String> {
        val messages = mutableListOf<String>()
        for (i in 0..message.length step maxLogSize) {
            val end = if (i + maxLogSize < message.length) i + maxLogSize else message.length
            messages.add(message.substring(i, end))
        }
        return messages
    }


}

