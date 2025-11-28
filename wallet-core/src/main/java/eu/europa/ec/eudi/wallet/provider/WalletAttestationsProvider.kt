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

package eu.europa.ec.eudi.wallet.provider

import eu.europa.ec.eudi.openid4vci.Nonce
import org.multipaz.securearea.KeyInfo

/**
 * Interface defining the bridge between the Wallet Core SDK and the Wallet Provider Service.
 *
 * Implementations of this interface are responsible for communicating with the Wallet Provider's
 * backend to retrieve cryptographic proofs (Attestations) regarding the integrity of the
 * Wallet Application and the security of the Device.
 *
 * These attestations are required during the OpenID for Verifiable Credential Issuance (OID4VCI) flow.
 */
interface WalletAttestationsProvider {

    /**
     * Retrieves the Wallet Instance Attestation (WIA).
     *
     * This attestation proves that the Wallet Application is genuine, untampered with,
     * and trusted by the Wallet Provider. It is typically used for **Client Authentication**
     * at the Authorization Server's Token Endpoint (OAuth 2.0).
     *
     * @param keyInfo Information about the cryptographic key that will be
     * bound to this attestation. The Wallet Provider must sign the WIA such that
     * it confirms this key belongs to a valid app instance.
     * @return A [Result] containing the WIA as a signed JWT string (e.g., Client Attestation JWT).
     */
    suspend fun getWalletAttestation(keyInfo: KeyInfo): Result<String>

    /**
     * Retrieves the Wallet Unit Attestation (WUA) or Key Attestation.
     *
     * This method is used when issuing with Attestation Proof Type or JWT with Attestation Proof Type
     *
     * @param keys The list of public keys that need to be
     * certified. These keys will be bound to the issuance session.
     * @param nonce An optional nonce provided by the Issuer.
     * If provided, it must be embedded in the attestation.
     * @return A [Result] containing the WUA as a signed JWT
     */
    suspend fun getKeyAttestation(keys: List<KeyInfo>, nonce: Nonce?): Result<String>
}