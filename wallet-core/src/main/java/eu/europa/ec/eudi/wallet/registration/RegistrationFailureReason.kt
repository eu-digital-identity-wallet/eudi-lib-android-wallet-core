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
package eu.europa.ec.eudi.wallet.registration

/**
 * The reason a relying party's registration certificate could not be validated.
 */
enum class RegistrationFailureReason {

    /** The request carried no registration certificate. */
    CERTIFICATE_ABSENT,

    /** The certificate is malformed and could not be parsed. */
    MALFORMED,

    /** The certificate does not carry the required status list reference. */
    STATUS_MISSING,

    /** The certificate signature could not be cryptographically verified. */
    SIGNATURE_INVALID,

    /** The certificate was not issued by a valid trusted provider of registration certificates. */
    UNTRUSTED_PROVIDER,

    /** The certificate had expired. */
    EXPIRED,

    /** The certificate had been revoked. */
    REVOKED,

    /** The certificate's revocation status could not be determined. */
    REVOCATION_STATUS_UNKNOWN,

    /** The certificate is not bound to the relying party that signed the request. */
    NOT_BOUND_TO_REQUESTER,

    /**
     * The certificate does not confirm that the provider is registered for the operation at hand: on
     * the issuance path, its `entitlements` do not cover issuing the offered attestations (ARF
     * ISSU_24a for a PID, ISSU_34a for any other attestation).
     */
    ENTITLEMENT_MISSING
}
