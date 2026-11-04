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

package eu.europa.ec.eudi.wallet.document

/**
 * The protections applied to the keys created with [DocumentExtensions.getDefaultCreateKeySettings]
 * and [DocumentExtensions.getDefaultCreateDocumentSettings], when no `configure` block is given.
 *
 * They are determined when the wallet is built, from the [KeyProtectionPolicy] values and the device
 * support. With [KeyProtectionPolicy.IfSupported], a value is `false` when the device does not support
 * the protection.
 *
 * @property userAuthenticationRequired whether the keys require user authentication
 * @property useStrongBox whether the keys are created in StrongBox
 */
data class KeyProtectionState(
    val userAuthenticationRequired: Boolean,
    val useStrongBox: Boolean,
)
