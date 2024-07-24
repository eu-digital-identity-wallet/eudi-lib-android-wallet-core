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
import eu.europa.ec.eudi.wallet.EudiWalletConfig

internal class LoggerImpl(val config: EudiWalletConfig, private val maxLogSize: Int = 1000) : Logger {
    override fun log(record: Logger.Record) {
        val tag = record.sourceClassName ?: ""
        splitMessage(record.message).forEachIndexed { i, m ->
            when {
                record.level == Logger.LEVEL_ERROR && config.logLevel >= Logger.LEVEL_ERROR && i == 0 && record.thrown != null ->
                    Log.e(tag, m, record.thrown)

                record.level == Logger.LEVEL_ERROR && config.logLevel >= Logger.LEVEL_ERROR -> Log.e(tag, m)
                record.level == Logger.LEVEL_INFO && config.logLevel >= Logger.LEVEL_INFO -> Log.i(tag, m)
                record.level == Logger.LEVEL_DEBUG && config.logLevel >= Logger.LEVEL_DEBUG -> Log.d(tag, m)
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