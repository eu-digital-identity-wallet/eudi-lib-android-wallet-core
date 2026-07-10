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
package eu.europa.ec.eudi.wallet.registration.relyingparty

/**
 * Whether the relying party's registration certificate is handled during presentation requests.
 */
enum class WrpRegistrationPolicy {

    /**
     * The registration certificate is neither validated nor surfaced to the user.
     */
    Disabled,

    /**
     * The registration certificate is validated and its outcome is surfaced to the user (the default).
     * A certificate that is within scope but fails a validity check (binding, expiry or revocation), and
     * a request that asks for attributes outside the registered scope, are shown to the user as warnings
     * to approve before sharing; an absent, malformed or untrusted certificate is rejected.
     */
    Enabled
}
