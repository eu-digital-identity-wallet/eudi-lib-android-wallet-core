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

import org.multipaz.securearea.KeyLockedException
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.KeyUnlockDataProvider
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.UnlockReason

/**
 * Extension function to convert a nullable [KeyUnlockData] to a [KeyUnlockDataProvider].
 *
 * This allows the existing API that accepts [KeyUnlockData] to work with the new
 * multipaz 0.95 API that requires [KeyUnlockDataProvider] in coroutine context.
 *
 * @return A [KeyUnlockDataProvider] that returns the [KeyUnlockData] when requested,
 *         or throws [KeyLockedException] if the data is null.
 */
internal fun KeyUnlockData?.asProvider(): KeyUnlockDataProvider {
    val data = this
    return object : KeyUnlockDataProvider {
        override suspend fun getKeyUnlockData(
            secureArea: SecureArea,
            alias: String,
            unlockReason: UnlockReason
        ): KeyUnlockData {
            return data ?: throw KeyLockedException("No unlock data provided")
        }
    }
}
