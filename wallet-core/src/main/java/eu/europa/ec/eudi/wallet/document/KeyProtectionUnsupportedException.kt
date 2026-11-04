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
 * Thrown when the wallet is built with [KeyProtectionPolicy.Required] for a protection that the
 * device does not support.
 *
 * @property protection the protection that the device does not support
 */
class KeyProtectionUnsupportedException(
    val protection: Protection,
    message: String,
) : IllegalStateException(message) {

    /** A key protection that the device may not support. */
    enum class Protection {
        /** User authentication; the device must be secured with a PIN, pattern or password. */
        UserAuthentication,

        /** StrongBox. */
        StrongBox
    }
}
