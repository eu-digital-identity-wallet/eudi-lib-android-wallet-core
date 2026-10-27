/*
 * Copyright (c) 2023-2026 European Commission
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
package eu.europa.ec.eudi.wallet.trust

/**
 * Defines the PKIX revocation checking policy for ETSI LoTE certificate chain validation.
 *
 * Each policy maps to specific [java.security.cert.PKIXRevocationChecker.Option] flags
 * that control how CRL and OCSP revocation data is fetched and validated during
 * certificate chain verification.
 *
 * **IMPORTANT**: Android blocks HTTP URLs for CRL/OCSP endpoints. Only HTTPS endpoints
 * are supported. Certificate chains with HTTP-only revocation endpoints will fail
 * validation unless [Disabled] or [SoftFail] policies are used.
 */
enum class PkixRevocationPolicy {
    /**
     * Disables revocation checking entirely.
     *
     * Sets `isRevocationEnabled = false` on PKIX parameters.
     */
    Disabled,

    /**
     * Enables revocation checking with OCSP primary, CRL fallback, tolerating
     * unavailable revocation data.
     *
     * Maps to: `PKIXRevocationChecker.Option.SOFT_FAIL`
     *
     * Validation succeeds even when:
     * - OCSP responder is unreachable
     * - CRL distribution point is unreachable
     * - Certificate lacks revocation endpoints
     */
    SoftFail,

    /**
     * Enables revocation checking with CRL primary, OCSP fallback. **(Default)**
     *
     * Maps to: `PKIXRevocationChecker.Option.PREFER_CRLS`
     *
     * Attempts CRL first:
     * - If CRL fetch succeeds → uses CRL
     * - If CRL fetch fails → falls back to OCSP
     * - If both fail → validation fails
     */
    CRLPriority,

    /**
     * Enables revocation checking with CRL only, no OCSP fallback.
     *
     * Maps to: `PKIXRevocationChecker.Option.PREFER_CRLS + NO_FALLBACK`
     *
     * Only uses CRL:
     * - If CRL fetch succeeds → uses CRL
     * - If CRL fetch fails → validation fails (no OCSP fallback)
     */
    CRLOnly,

    /**
     * Enables revocation checking with OCSP primary, CRL fallback. Java's default behavior.
     *
     * Maps to: No options (Java's default PKIXRevocationChecker behavior)
     *
     * Attempts OCSP first:
     * - If OCSP fetch succeeds → uses OCSP
     * - If OCSP fetch fails → falls back to CRL
     * - If both fail → validation fails
     */
    OCSPPriority,
}
