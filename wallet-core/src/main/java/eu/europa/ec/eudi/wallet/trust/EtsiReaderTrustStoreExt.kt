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
package eu.europa.ec.eudi.wallet.trust

import eu.europa.ec.eudi.etsi1196x2.consultation.IsChainTrustedForEUDIW
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.Dispatchers
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import kotlin.coroutines.CoroutineContext

/**
 * Creates a [ReaderTrustStore] backed by this ETSI chain trust validator.
 *
 * Convenience extension for creating an [EtsiReaderTrustStore] with sensible defaults.
 * Used internally by [eu.europa.ec.eudi.wallet.EudiWalletConfig.configureReaderTrustStore].
 *
 * @param verificationContext the EUDI verification context for reader authentication
 *        (defaults to [VerificationContext.WalletRelyingPartyAccessCertificate])
 * @param coroutineContext the coroutine context for the sync/async bridge
 *        (defaults to [Dispatchers.IO])
 * @param logger optional [Logger] for diagnostic output
 * @return a [ReaderTrustStore] that delegates to this chain trust validator
 */
internal fun IsChainTrustedForEUDIW<List<X509Certificate>, TrustAnchor>.asReaderTrustStore(
    verificationContext: VerificationContext = VerificationContext.WalletRelyingPartyAccessCertificate,
    coroutineContext: CoroutineContext = Dispatchers.IO,
    logger: Logger? = null,
): ReaderTrustStore = EtsiReaderTrustStore(this, verificationContext, coroutineContext, logger)
