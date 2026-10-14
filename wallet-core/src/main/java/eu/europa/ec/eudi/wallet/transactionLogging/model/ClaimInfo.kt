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

package eu.europa.ec.eudi.wallet.transactionLogging.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

/**
 * Which claims were requested or presented in a transaction (TS10 §3.19.2).
 *
 * Holds only which claims were involved, not their values.
 *
 * @property credentialIdentifier the credential the claims belong to; the `vct` value for
 * SD-JWT VC or the `docType` value for ISO/IEC 18013-5.
 * @property claims the claim paths involved. Each [ClaimPath] is one OpenID4VP §7 path pointer.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ClaimInfo(
    val credentialIdentifier: String,
    val claims: List<ClaimPath>,
)
