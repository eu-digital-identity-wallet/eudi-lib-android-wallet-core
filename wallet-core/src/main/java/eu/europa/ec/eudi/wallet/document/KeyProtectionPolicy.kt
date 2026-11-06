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
 * Whether a protection (user authentication or StrongBox) is applied to the keys created with
 * [DocumentExtensions.getDefaultCreateKeySettings] and [DocumentExtensions.getDefaultCreateDocumentSettings].
 *
 * The device support is checked when the wallet is built.
 */
enum class KeyProtectionPolicy {
    /**
     * The protection is applied. Building the wallet fails with [KeyProtectionUnsupportedException]
     * if the device does not support it.
     */
    Required,

    /**
     * The protection is applied if the device supports it; otherwise the keys are created without it.
     * The device support is checked when the wallet is built, so a screen lock set up later applies
     * after the wallet is built again.
     */
    IfSupported,

    /** The protection is not applied. */
    Disabled
}
