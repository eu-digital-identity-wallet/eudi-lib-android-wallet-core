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

package eu.europa.ec.eudi.wallet.dcapi

/**
 * The Digital Credential API (DCAPI) protocols the wallet can process.
 *
 * Enable the protocols the wallet should accept through [DCAPIConfig.supportedProtocols]. Only the
 * enabled protocols are processed; a request for any other protocol is rejected.
 *
 * @property identifier the protocol identifier as it appears in the request.
 */
enum class DCAPIProtocol(val identifier: String) {

    /** ISO mdoc presentation, as defined in ISO/IEC TS 18013-7:2025 Annex C. */
    ISO_MDOC("org-iso-mdoc"),

    /** OpenID4VP 1.0 over the Digital Credential API, using a signed request. */
    OPENID4VP_V1_SIGNED("openid4vp-v1-signed"),

    /** OpenID4VP 1.0 over the Digital Credential API, using an unsigned request. */
    OPENID4VP_V1_UNSIGNED("openid4vp-v1-unsigned");

    /** Whether this protocol is an OpenID4VP protocol, as opposed to ISO mdoc. */
    internal val isOpenId4Vp: Boolean
        get() = this == OPENID4VP_V1_SIGNED || this == OPENID4VP_V1_UNSIGNED
}
