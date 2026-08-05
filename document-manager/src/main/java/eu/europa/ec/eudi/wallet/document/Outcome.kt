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

package eu.europa.ec.eudi.wallet.document

/**
 * Outcome for encapsulating success or failure of a computation for document manager operations.
 * Wraps a [Result] instance to provide Java interop.
 *
 * @property kotlinResult the [Result] instance that this [Outcome] wraps.
 * @property isSuccess `true` if instance represents a successful outcome.
 * @property isFailure `true` if instance represents a failed outcome.
 *
 */
class Outcome<out T> private constructor(val kotlinResult: Result<T>) {
    companion object {
        /**
         * Returns an instance that encapsulates the given [value] as successful value.
         * @param value the value to encapsulate
         * @return an instance that encapsulates the given [value] as successful value
         */
        @JvmStatic
        fun <T> success(value: T): Outcome<T> = Outcome(Result.success(value))

        /**
         * Returns an instance that encapsulates the given [Throwable] as failure.
         * @param throwable the [Throwable] to encapsulate
         * @return an instance that encapsulates the given [Throwable] as failure
         */
        @JvmStatic
        fun <T> failure(throwable: Throwable): Outcome<T> =
            Outcome(Result.failure(throwable))
    }

    /**
     * Returns `true` if instance represents a successful outcome.
     * In this case [eu.europa.ec.eudi.wallet.document.Outcome.isFailure] return `false` .
     */
    val isSuccess: Boolean
        get() = kotlinResult.isSuccess

    /**
     * Returns `true` if instance represents a failed outcome.
     * In this case [eu.europa.ec.eudi.wallet.document.Outcome.isSuccess] returns `false`.
     */
    val isFailure: Boolean
        get() = kotlinResult.isFailure

    /**
     * Returns the encapsulated value if this instance represents a successful outcome or `null`
     * if it is failure.
     * @return the encapsulated value if this instance represents a successful outcome or `null`
     */
    fun getOrNull(): T? = kotlinResult.getOrNull()

    /**
     * Returns the encapsulated exception if this instance represents a failure outcome or `null`
     * if it is success.
     * @return the encapsulated exception if this instance represents a failure outcome or `null`
     */
    fun exceptionOrNull(): Throwable? = kotlinResult.exceptionOrNull()

    /**
     * Returns the encapsulated value if this instance represents a successful outcome or throws
     * the encapsulated exception if it is failure.
     * @return the encapsulated value if this instance represents a successful outcome
     * @throws Throwable the encapsulated exception if this instance represents a failure outcome
     */
    fun getOrThrow(): T = kotlinResult.getOrThrow()

    override fun toString(): String =
        if (isSuccess) "Outcome(value = ${getOrNull()})"
        else "Outcome(error = ${exceptionOrNull()?.message})"
}