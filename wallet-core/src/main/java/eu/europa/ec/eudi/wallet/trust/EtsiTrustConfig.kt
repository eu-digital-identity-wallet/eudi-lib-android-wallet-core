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

import eu.europa.ec.eudi.etsi119602.datamodel.Uri
import eu.europa.ec.eudi.etsi119602.consultation.LoadLoTEAndPointers
import eu.europa.ec.eudi.etsi119602.consultation.LotEMeta
import eu.europa.ec.eudi.etsi119602.consultation.VerifyJwtSignature
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationClassifications
import eu.europa.ec.eudi.etsi1196x2.consultation.SupportedLists
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Configuration for the ETSI LoTE (List of Trusted Entities) trust infrastructure.
 *
 * This centralizes the LoTE pipeline parameters so that the core can build the
 * [eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW] trust source
 * internally, eliminating the need for consumers to construct the ETSI library
 * plumbing themselves.
 *
 * @property loteLocations the LoTE download locations (e.g., PID, PubEAA, WRPAC provider URLs)
 * @property classifications attestation classifications mapping credential types to verification contexts
 * @property fileCacheExpiration how long downloaded LoTE files are cached on disk (default: 24 hours)
 * @property cacheTtl how long the in-memory trust anchor cache is valid (default: 20 minutes)
 * @property relaxCertificateProfiles whether to strip end-entity profile constraints (for DEV/testing)
 * @property relaxPkixRevocation whether to disable PKIX revocation checking (for DEV/testing)
 * @property customJwtSignatureVerifier optional custom JWT signature verifier for LoTE JWTs;
 *   when null, the core uses its built-in [LoteJwtVerifier]
 * @property loteConstraints controls whether additional LoTE pointers are followed
 */
data class EtsiTrustConfig(
    val loteLocations: SupportedLists<Uri>,
    val classifications: AttestationClassifications,
    val fileCacheExpiration: Duration = DEFAULT_FILE_CACHE_EXPIRATION,
    val cacheTtl: Duration = DEFAULT_CACHE_TTL,
    val relaxCertificateProfiles: Boolean = false,
    val relaxPkixRevocation: Boolean = false,
    val customJwtSignatureVerifier: VerifyJwtSignature? = null,
    val loteConstraints: LoadLoTEAndPointers.Constraints = LoadLoTEAndPointers.Constraints.DoNotLoadOtherPointers,
) {
    companion object {
        val DEFAULT_FILE_CACHE_EXPIRATION: Duration = 24.hours
        val DEFAULT_CACHE_TTL: Duration = 20.minutes
    }
}

/**
 * DSL builder for constructing an [EtsiTrustConfig].
 *
 * [loteLocations] and [classifications] are required. All other settings have sensible defaults.
 *
 * Example:
 * ```
 * configureEtsiTrust {
 *     loteLocations(SupportedLists(
 *         pidProviders = Uri("https://trustedlist.../PIDProviders.jwt"),
 *         wrpacProviders = Uri("https://trustedlist.../WRPACProviders.jwt"),
 *     ))
 *     classifications(AttestationClassifications(
 *         pids = AttestationIdentifierPredicate.any(setOf(
 *             AttestationIdentifier.MDoc("eu.europa.ec.eudi.pid.1"),
 *         )),
 *     ))
 *     // Optional:
 *     relaxCertificateProfiles()
 *     fileCacheExpiration(12.hours)
 *     cacheTtl(10.minutes)
 * }
 * ```
 */
class EtsiTrustConfigBuilder {

    private var loteLocations: SupportedLists<Uri>? = null
    private var classifications: AttestationClassifications? = null
    private var fileCacheExpiration: Duration = EtsiTrustConfig.DEFAULT_FILE_CACHE_EXPIRATION
    private var cacheTtl: Duration = EtsiTrustConfig.DEFAULT_CACHE_TTL
    private var relaxCertificateProfiles: Boolean = false
    private var relaxPkixRevocation: Boolean = false
    private var customJwtSignatureVerifier: VerifyJwtSignature? = null
    private var loteConstraints: LoadLoTEAndPointers.Constraints =
        LoadLoTEAndPointers.Constraints.DoNotLoadOtherPointers

    /**
     * Sets the LoTE download locations.
     *
     * Example:
     * ```
     * loteLocations(SupportedLists(
     *     pidProviders = Uri("https://trustedlist.../PIDProviders.jwt"),
     *     wrpacProviders = Uri("https://trustedlist.../WRPACProviders.jwt"),
     * ))
     * ```
     *
     * @param locations the supported lists with LoTE URLs
     */
    fun loteLocations(locations: SupportedLists<Uri>) {
        this.loteLocations = locations
    }

    /**
     * Sets the attestation classifications mapping credential types to verification contexts.
     *
     * @param classifications the attestation classifications
     */
    fun classifications(classifications: AttestationClassifications) {
        this.classifications = classifications
    }

    /**
     * Sets how long downloaded LoTE files are cached on disk.
     *
     * @param duration the file cache expiration duration (default: 24 hours)
     */
    fun fileCacheExpiration(duration: Duration) {
        this.fileCacheExpiration = duration
    }

    /**
     * Sets how long the in-memory trust anchor cache is valid.
     *
     * @param duration the cache TTL duration (default: 20 minutes)
     */
    fun cacheTtl(duration: Duration) {
        this.cacheTtl = duration
    }

    /**
     * Relaxes certificate profile checks for all provider types.
     *
     * When enabled, `endEntityProfile` constraints are stripped from all verification
     * contexts. Use this for DEV/testing environments where certificates may not
     * fully conform to ETSI profile requirements.
     */
    fun relaxCertificateProfiles() {
        this.relaxCertificateProfiles = true
    }

    /**
     * Disables PKIX revocation checking during certificate chain validation.
     *
     * When called, `isRevocationEnabled` is set to `false` on the PKIX parameters.
     * Use this for DEV/testing environments where CRL/OCSP endpoints may not be available.
     *
     * When this is **not** called (the default), PKIX revocation checking is enabled and
     * strict — certificate chain validation will fail if a certificate is revoked or if
     * the revocation status cannot be determined (CRL/OCSP unavailable).
     */
    fun relaxPkixRevocation() {
        this.relaxPkixRevocation = true
    }

    /**
     * Sets a custom JWT signature verifier for LoTE JWTs.
     *
     * When not set, the core uses its built-in [LoteJwtVerifier] that
     * verifies JWT signatures using the `x5c` certificate chain from the JWT header.
     *
     * @param verifier the custom JWT signature verifier
     */
    fun jwtSignatureVerifier(verifier: VerifyJwtSignature) {
        this.customJwtSignatureVerifier = verifier
    }

    /**
     * Controls whether additional LoTE pointers are followed when loading.
     *
     * @param constraints the LoTE loading constraints
     *   (default: [LoadLoTEAndPointers.Constraints.DoNotLoadOtherPointers])
     */
    fun loteConstraints(constraints: LoadLoTEAndPointers.Constraints) {
        this.loteConstraints = constraints
    }

    /**
     * Builds the [EtsiTrustConfig] from the current builder state.
     *
     * @return a validated [EtsiTrustConfig]
     * @throws IllegalArgumentException if [loteLocations] or [classifications] are not set
     */
    internal fun build(): EtsiTrustConfig {
        val locations = requireNotNull(loteLocations) {
            "loteLocations must be provided via loteLocations()"
        }
        val cls = requireNotNull(classifications) {
            "classifications must be provided via classifications()"
        }
        return EtsiTrustConfig(
            loteLocations = locations,
            classifications = cls,
            fileCacheExpiration = fileCacheExpiration,
            cacheTtl = cacheTtl,
            relaxCertificateProfiles = relaxCertificateProfiles,
            relaxPkixRevocation = relaxPkixRevocation,
            customJwtSignatureVerifier = customJwtSignatureVerifier,
            loteConstraints = loteConstraints,
        )
    }
}
