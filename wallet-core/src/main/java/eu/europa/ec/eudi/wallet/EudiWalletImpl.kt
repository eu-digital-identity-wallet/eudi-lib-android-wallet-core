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

package eu.europa.ec.eudi.wallet

import android.content.Context
import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.sample.SampleDocumentManager
import eu.europa.ec.eudi.wallet.document.sample.SampleDocumentManagerImpl
import eu.europa.ec.eudi.wallet.internal.getCertificate
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.presentation.PresentationManager
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogger
import java.security.cert.X509Certificate

/**
 * Implementation of [EudiWallet]
 * @property context the android context
 * @property config the wallet configuration
 * @property documentManager the document manager
 * @property presentationManager the presentation manager
 * @property transferManager the transfer manager
 * @property logger the logger
 */
class EudiWalletImpl internal constructor(
    val context: Context,
    override val config: EudiWalletConfig,
    override val documentManager: DocumentManager,
    override val presentationManager: PresentationManager,
    override val transferManager: TransferManager,
    override val logger: Logger,
    internal val readerTrustStoreConsumer: ((ReaderTrustStore) -> Unit),
    internal val openId4VciManagerFactory: (() -> OpenId4VciManager),
    val transactionLogger: TransactionLogger?,
) : EudiWallet, DocumentManager, PresentationManager by presentationManager,
    SampleDocumentManager by SampleDocumentManagerImpl(documentManager) {

    override fun setReaderTrustStore(readerTrustStore: ReaderTrustStore) = apply {
        readerTrustStoreConsumer(readerTrustStore)
    }

    override fun setTrustedReaderCertificates(trustedReaderCertificates: List<X509Certificate>) =
        apply {
            readerTrustStoreConsumer(ReaderTrustStore.getDefault(trustedReaderCertificates))
        }

    override fun setTrustedReaderCertificates(vararg rawRes: Int) = apply {
        readerTrustStoreConsumer(ReaderTrustStore.getDefault(rawRes.map { context.getCertificate(it) }))
    }

    override fun createOpenId4VciManager(): OpenId4VciManager {
        return openId4VciManagerFactory()
    }
}