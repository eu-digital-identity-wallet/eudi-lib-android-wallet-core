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

package eu.europa.ec.eudi.wallet.internal

import eu.europa.ec.eudi.wallet.logging.Logger
import com.android.identity.util.Logger as IdentityLogger

internal class LogPrinterImpl(private val logger: Logger) : IdentityLogger.LogPrinter {
    override fun printLn(level: Int, tag: String, msg: String, throwable: Throwable?) {
        val coreLevel = when (level) {
            IdentityLogger.LEVEL_W -> Logger.LEVEL_INFO
            IdentityLogger.LEVEL_E -> Logger.LEVEL_ERROR
            IdentityLogger.LEVEL_I -> Logger.LEVEL_INFO
            IdentityLogger.LEVEL_D -> Logger.LEVEL_DEBUG
            else -> Logger.LEVEL_INFO
        }
        val record = Logger.Record(
            level = coreLevel,
            sourceClassName = tag,
            message = msg,
            thrown = throwable
        )
        logger.log(record)
    }
}