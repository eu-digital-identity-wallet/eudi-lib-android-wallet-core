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

import android.content.Context
import eu.europa.ec.eudi.etsi119602.consultation.DownloadSingleLoTE
import eu.europa.ec.eudi.etsi119602.consultation.LoadLoTEAndPointers
import eu.europa.ec.eudi.etsi119602.consultation.LoadSingleLoTEWithFileCache
import eu.europa.ec.eudi.etsi119602.consultation.LotEMeta
import eu.europa.ec.eudi.etsi119602.consultation.ProvisionTrustAnchorsFromLoTEs
import eu.europa.ec.eudi.etsi119602.consultation.VerifyJwtSignature
import eu.europa.ec.eudi.etsi119602.consultation.eu
import eu.europa.ec.eudi.etsi119602.consultation.eudiwJvm
import eu.europa.ec.eudi.etsi1196x2.consultation.DisposableContainer
import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW
import eu.europa.ec.eudi.etsi1196x2.consultation.SupportedLists
import eu.europa.ec.eudi.etsi1196x2.consultation.ValidateCertificateChainUsingPKIXJvm
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.logging.Logger
import io.ktor.client.HttpClient
import kotlinx.io.files.Path
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate

/**
 * Internal provider that constructs the full ETSI LoTE trust pipeline from an [EtsiTrustConfig].
 *
 * This encapsulates the construction of:
 * - HTTP client for downloading LoTEs
 * - File cache for downloaded LoTEs
 * - JWT signature verification for LoTE JWTs
 * - Trust anchor provisioning from LoTEs
 * - Cached chain trust validator
 *
 * The resulting [isChainTrusted] can be used as the shared trust source across all
 * trust verification areas (issuer trust, status list trust, reader authentication).
 *
 * @param config the ETSI trust configuration
 * @param context the Android context (used for cache directory)
 * @param logger optional logger for diagnostic output
 */
internal class EtsiTrustProvider(
    config: EtsiTrustConfig,
    context: Context,
    logger: Logger? = null,
    ktorHttpClientFactory: (() -> HttpClient)? = null,
) {
    private val disposableContainer = DisposableContainer()

    /**
     * The shared chain trust validator constructed from the LoTE infrastructure.
     * This implements [IsChainTrustedForEUDIW] and can be used with all trust areas.
     */
    val isChainTrusted: IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>

    init {
        logger?.d(TAG, "Initializing ETSI trust provider: " +
            "cacheTtl=${config.cacheTtl}, " +
            "fileCacheExpiration=${config.fileCacheExpiration}, " +
            "relaxProfiles=${config.relaxCertificateProfiles}, " +
            "relaxPkixRevocation=${config.relaxPkixRevocation}")

        // HTTP client (use injected factory or default)
        val httpClient = ktorHttpClientFactory?.invoke() ?: HttpClient()

        // LoTE loader with file cache
        val loadLoTE = LoadSingleLoTEWithFileCache(
            cacheDirectory = Path(context.cacheDir.path, "lote-cache"),
            downloadSingleLoTE = DownloadSingleLoTE(httpClient),
            fileCacheExpiration = config.fileCacheExpiration,
        )

        // LoTE pointer loading + JWT verification
        val jwtVerifier = config.customJwtSignatureVerifier
            ?: LoteJwtVerifier(logger)
        val loadLoTEAndPointers = LoadLoTEAndPointers(
            constraints = config.loteConstraints,
            verifyJwtSignature = jwtVerifier,
            loadLoTE = loadLoTE,
        )

        // Trust anchor provisioner
        val svcTypePerCtx = if (config.relaxCertificateProfiles) {
            relaxProfiles(SupportedLists.eu())
        } else {
            SupportedLists.eu()
        }
        val provisionTrustAnchors = if (config.relaxPkixRevocation) {
            ProvisionTrustAnchorsFromLoTEs.eudiwJvm(
                loadLoTEAndPointers = loadLoTEAndPointers,
                svcTypePerCtx = svcTypePerCtx,
                pkix = ValidateCertificateChainUsingPKIXJvm(
                    customization = { isRevocationEnabled = false }
                ),
            )
        } else {
            ProvisionTrustAnchorsFromLoTEs.eudiwJvm(
                loadLoTEAndPointers = loadLoTEAndPointers,
                svcTypePerCtx = svcTypePerCtx,
            )
        }

        // Cached chain trust validator
        isChainTrusted = provisionTrustAnchors.cached(
            disposableContainer,
            config.loteLocations,
            ttl = config.cacheTtl,
        )

        logger?.d(TAG, "ETSI trust provider initialized")
    }

    private companion object {
        const val TAG = "EtsiTrust"

        /**
         * Relaxes certificate profile checks by stripping `endEntityProfile` from all
         * provider types. Used for DEV/testing environments.
         */
        fun relaxProfiles(
            eu: SupportedLists<LotEMeta<VerificationContext>>,
        ): SupportedLists<LotEMeta<VerificationContext>> {
            fun LotEMeta<VerificationContext>.relax() = copy(
                svcTypePerCtx = svcTypePerCtx.mapValues { (_, v) ->
                    v.copy(endEntityProfile = null)
                }
            )
            return eu.copy(
                pidProviders = eu.pidProviders?.relax(),
                walletProviders = eu.walletProviders?.relax(),
                wrpacProviders = eu.wrpacProviders?.relax(),
                wrprcProviders = eu.wrprcProviders?.relax(),
                pubEaaProviders = eu.pubEaaProviders?.relax(),
                qeaProviders = eu.qeaProviders?.relax(),
                eaaProviders = eu.eaaProviders.mapValues { (_, v) -> v.relax() },
            )
        }
    }
}
