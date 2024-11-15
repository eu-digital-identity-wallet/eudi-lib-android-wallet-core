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
import eu.europa.ec.eudi.wallet.logging.Logger.Companion.LEVEL_DEBUG
import eu.europa.ec.eudi.wallet.logging.Logger.Companion.LEVEL_ERROR
import eu.europa.ec.eudi.wallet.logging.Logger.Companion.LEVEL_INFO

@JvmSynthetic
internal fun Logger.d(tag: String, message: String) = log(
    Logger.Record(
        level = LEVEL_DEBUG,
        sourceClassName = tag,
        message = message
    )
)

@JvmSynthetic
internal fun Logger.i(tag: String, message: String) = log(
    Logger.Record(
        level = LEVEL_INFO,
        sourceClassName = tag,
        message = message
    )
)

@JvmSynthetic
internal fun Logger.e(tag: String, message: String, throwable: Throwable? = null) =
    log(
        Logger.Record(
            level = LEVEL_ERROR,
            sourceClassName = tag,
            message = message,
            thrown = throwable
        )
    )