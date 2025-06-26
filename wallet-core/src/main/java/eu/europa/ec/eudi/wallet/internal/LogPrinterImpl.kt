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

import eu.europa.ec.eudi.wallet.logging.Logger
import org.multipaz.util.Logger as IdentityLogger

internal class LogPrinterImpl(private val logger: Logger) : IdentityLogger.LogPrinter {
    override fun print(level: IdentityLogger.LogPrinter.Level, tag: String, msg: String, throwable: Throwable?) {
        val coreLevel = when (level) {
            IdentityLogger.LogPrinter.Level.WARNING -> Logger.LEVEL_INFO
            IdentityLogger.LogPrinter.Level.ERROR -> Logger.LEVEL_ERROR
            IdentityLogger.LogPrinter.Level.INFO -> Logger.LEVEL_INFO
            IdentityLogger.LogPrinter.Level.DEBUG -> Logger.LEVEL_DEBUG
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