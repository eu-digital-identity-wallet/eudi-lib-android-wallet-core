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

package eu.europa.ec.eudi.wallet.registration.issuer

/**
 * Whether the wallet validates a credential issuer's registration certificate during OpenID4VCI
 * issuance.
 *
 * The registration certificate is carried in the signed issuer metadata, so [Enabled] validation
 * only runs when signed metadata is configured (via `configureIssuerTrust { requireSignedMetadata() }`);
 * otherwise it is silently skipped.
 */
enum class IssuerRegistrationPolicy {
    /** The issuer registration certificate is validated and its outcome surfaced (default). */
    Enabled,

    /** The issuer registration certificate is neither validated nor surfaced. */
    Disabled
}
