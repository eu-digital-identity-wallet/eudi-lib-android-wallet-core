/*
 * Copyright (c) 2023-2024 European Commission
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
package eu.europa.ec.eudi.iso18013.transfer.response

/**
 * Represents the result of a response
 */
sealed interface ResponseResult {

    /**
     * The response generation was successful
     * @property response the response
     */
    data class Success(val response: Response) : ResponseResult

    /**
     * The response generation failed
     * @property throwable the throwable
     */
    data class Failure(val throwable: Throwable) : ResponseResult

    /**
     * Returns the response or throws the throwable
     * @throws Throwable the throwable
     * @return the response
     */
    fun getOrThrow(): Response {
        return when (this) {
            is Success -> response
            is Failure -> throw throwable
        }
    }

    /**
     * Returns the response or null
     * @return the response or null
     */
    fun getOrNull(): Response? {
        return when (this) {
            is Success -> response
            is Failure -> null
        }
    }
}
