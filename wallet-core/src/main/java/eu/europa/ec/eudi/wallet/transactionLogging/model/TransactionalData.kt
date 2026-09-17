/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.eudi.wallet.transactionLogging.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

/**
 * Data of an external transaction the user authorized, such as a signature activation, carried in
 * a presentation request (TS10 §3.19.12).
 *
 * Its attributes are the ones the specification or rulebook of the attestation type defines, so
 * they are recorded without being read into the payload of any type. OpenID4VP carries them in the
 * `transaction_data` parameter, a non-empty array of base64url-encoded objects, so [content] is an
 * array of the objects they encode, even when the request carried only one.
 *
 * @property content the objects the request carried, as the JSON they encode
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
@JvmInline
value class TransactionalData(val content: JsonArray)
