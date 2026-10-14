/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.transactionLogging.producers

/** Longest error message still considered readable enough to show; longer ones are replaced. */
private const val MAX_REASON_LENGTH = 120

/**
 * Turns a throwable into a short, non-blank reason string for a failed transaction.
 *
 * The reason is shown to the user, so only a message that reads like one sentence is kept: the
 * first line, and only if it is short. Anything longer (stack traces, library diagnostics) is
 * replaced by [default]. The throwable itself still reaches the app through the callback that
 * reported the failure.
 */
internal fun Throwable.toNoncompletionReason(default: String): String =
    message.asReason() ?: cause?.message.asReason() ?: default

private fun String?.asReason(): String? =
    this?.substringBefore('\n')
        ?.trim()
        ?.takeIf { it.isNotBlank() && it.length <= MAX_REASON_LENGTH }
