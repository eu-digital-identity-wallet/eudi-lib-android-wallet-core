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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import android.util.Log
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciLogger.Companion.TAG

internal interface OpenId4VciLogger {
    fun log(message: String, throwable: Throwable? = null)

    companion object {
        internal const val TAG = "OpenId4VciManager"
        operator fun invoke(config: OpenId4VciManager.Config): OpenId4VciLogger = DefaultLogger(config)
    }
}

internal class DefaultLogger(val config: OpenId4VciManager.Config, val maxLogSize: Int = 1000) : OpenId4VciLogger {

    override fun log(message: String, throwable: Throwable?) {
        splitMessage(message).forEachIndexed { i, m ->
            when {
                config.errorLoggingStatus && i == 0 && throwable != null -> Log.e(TAG, m, throwable)
                config.errorLoggingStatus && throwable != null -> Log.e(TAG, m)
                config.basicLoggingStatus -> Log.d(TAG, m)
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