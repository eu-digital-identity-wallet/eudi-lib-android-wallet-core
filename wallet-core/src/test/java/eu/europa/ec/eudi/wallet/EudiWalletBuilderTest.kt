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
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.presentation.PresentationManager
import eu.europa.ec.eudi.wallet.presentation.PresentationManagerImpl
import eu.europa.ec.eudi.wallet.statium.DocumentStatusResolver
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionAlgorithm
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionMethod
import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpConfig
import io.ktor.client.HttpClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.verify
import org.multipaz.securearea.AndroidKeystoreSecureArea
import org.multipaz.securearea.SecureArea
import org.multipaz.storage.Storage
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class EudiWalletBuilderTest {

    @Test
    fun `build with default configuration should create EudiWalletImpl with defaults`() {
        // Setup
        val context: Context = mockk {
            every { applicationContext } returns this
            every { noBackupFilesDir } returns File("no-backup")
        }

        val capabilities = mockk<AndroidKeystoreSecureArea.Capabilities> {
            every { secureLockScreenSetup } returns true
            every { strongBoxSupported } returns true
        }

        val config = EudiWalletConfig()

        val builder = spyk(EudiWallet.Builder(context, config)) {
            every { getDefaultDocumentManager(any(), any()) } returns mockk(relaxed = true)
            every { getTransferManager(any(), any()) } returns mockk(relaxed = true)
            every { getDocumentStatusResolver() } returns mockk(relaxed = true)
            every { this@spyk.capabilities } returns capabilities
        }

        // Execute
        val wallet = builder.build()

        // Verify
        assertIs<EudiWalletImpl>(wallet)
        verify(exactly = 1) { builder.getDefaultDocumentManager(null, null) }
        verify(exactly = 1) { builder.getTransferManager(any(), null) }
        verify(exactly = 1) { builder.getDocumentStatusResolver() }
    }

    @Test
    fun `withSecureAreas should use custom secure areas`() {
        // Setup
        val context: Context = mockk {
            every { applicationContext } returns this
            every { noBackupFilesDir } returns File("no-backup")
        }

        val capabilities = mockk<AndroidKeystoreSecureArea.Capabilities> {
            every { secureLockScreenSetup } returns true
            every { strongBoxSupported } returns true
        }

        val config = EudiWalletConfig()
        val customSecureArea: SecureArea = mockk()

        val builder = spyk(EudiWallet.Builder(context, config)) {
            every { getDefaultDocumentManager(any(), any()) } answers {
                // Verify secure areas are passed to document manager
                val secureAreas = secondArg<List<SecureArea>?>()
                assertEquals(1, secureAreas?.size)
                assertEquals(customSecureArea, secureAreas?.first())
                mockk(relaxed = true)
            }
            every { getTransferManager(any(), any()) } returns mockk(relaxed = true)
            every { getDocumentStatusResolver() } returns mockk(relaxed = true)
            every { this@spyk.capabilities } returns capabilities
        }

        // Execute
        builder.withSecureAreas(listOf(customSecureArea)).build()

        // Verify
        verify(exactly = 1) { builder.getDefaultDocumentManager(null, listOf(customSecureArea)) }
    }

    @Test
    fun `withStorageEngine should use custom storage engine`() {
        // Setup
        val context: Context = mockk {
            every { applicationContext } returns this
            every { noBackupFilesDir } returns File("no-backup")
        }

        val capabilities = mockk<AndroidKeystoreSecureArea.Capabilities> {
            every { secureLockScreenSetup } returns true
            every { strongBoxSupported } returns true
        }

        val config = EudiWalletConfig()
        val customStorageEngine: Storage = mockk()

        val builder = spyk(EudiWallet.Builder(context, config)) {
            every { getDefaultDocumentManager(any(), any()) } answers {
                // Verify storage engine is passed to document manager
                val storageEngine = firstArg<Storage?>()
                assertEquals(customStorageEngine, storageEngine)
                mockk(relaxed = true)
            }
            every { getTransferManager(any(), any()) } returns mockk(relaxed = true)
            every { getDocumentStatusResolver() } returns mockk(relaxed = true)
            every { this@spyk.capabilities } returns capabilities
        }

        // Execute
        builder.withStorage(customStorageEngine).build()

        // Verify
        verify(exactly = 1) { builder.getDefaultDocumentManager(customStorageEngine, null) }
    }

    @Test
    fun `withDocumentManager should use custom document manager`() {
        // Setup
        val context: Context = mockk {
            every { applicationContext } returns this
        }

        val capabilities = mockk<AndroidKeystoreSecureArea.Capabilities> {
            every { secureLockScreenSetup } returns true
            every { strongBoxSupported } returns true
        }

        val config = EudiWalletConfig()
        val customDocumentManager: DocumentManager = mockk()

        val builder = spyk(EudiWallet.Builder(context, config)) {
            every { getTransferManager(any(), any()) } returns mockk(relaxed = true)
            every { getDocumentStatusResolver() } returns mockk(relaxed = true)
            every { this@spyk.capabilities } returns capabilities
        }

        // Execute
        val wallet = builder.withDocumentManager(customDocumentManager).build()

        // Verify
        assertIs<EudiWalletImpl>(wallet)
        assertEquals(customDocumentManager, wallet.documentManager)
        verify(exactly = 0) { builder.getDefaultDocumentManager(any(), any()) }
        verify(exactly = 1) { builder.getTransferManager(customDocumentManager, null) }
    }

    @Test
    fun `withReaderTrustStore should use custom reader trust store`() {
        // Setup
        val context: Context = mockk {
            every { applicationContext } returns this
            every { noBackupFilesDir } returns File("no-backup")
        }

        val capabilities = mockk<AndroidKeystoreSecureArea.Capabilities> {
            every { secureLockScreenSetup } returns true
            every { strongBoxSupported } returns true
        }

        val config = EudiWalletConfig()
        val customReaderTrustStore: ReaderTrustStore = mockk()
        val documentManager: DocumentManager = mockk(relaxed = true)
        val transferManager: TransferManager = mockk(relaxed = true)

        val builder = spyk(EudiWallet.Builder(context, config)) {
            every { getDefaultDocumentManager(any(), any()) } returns documentManager
            every { getTransferManager(any(), any()) } answers {
                // Verify reader trust store is passed to transfer manager
                val readerTrustStore = secondArg<ReaderTrustStore?>()
                assertEquals(customReaderTrustStore, readerTrustStore)
                transferManager
            }
            every { getDocumentStatusResolver() } returns mockk(relaxed = true)
            every { this@spyk.capabilities } returns capabilities
        }

        // Execute
        val wallet = builder.withReaderTrustStore(customReaderTrustStore).build()

        // Verify
        assertIs<EudiWalletImpl>(wallet)
        verify(exactly = 1) { builder.getTransferManager(any(), customReaderTrustStore) }
    }

    @Test
    fun `withPresentationManager should use custom presentation manager`() {
        // Setup
        val context: Context = mockk {
            every { applicationContext } returns this
            every { noBackupFilesDir } returns File("no-backup")
        }

        val capabilities = mockk<AndroidKeystoreSecureArea.Capabilities> {
            every { secureLockScreenSetup } returns true
            every { strongBoxSupported } returns true
        }

        val config = EudiWalletConfig()
        val customPresentationManager: PresentationManager = mockk()

        val builder = spyk(EudiWallet.Builder(context, config)) {
            every { getDefaultDocumentManager(any(), any()) } returns mockk(relaxed = true)
            every { getTransferManager(any(), any()) } returns mockk(relaxed = true)
            every {
                getDefaultPresentationManager(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns mockk(relaxed = true)
            every { getDocumentStatusResolver() } returns mockk(relaxed = true)
            every { this@spyk.capabilities } returns capabilities
        }

        // Execute
        val wallet = builder.withPresentationManager(customPresentationManager).build()

        // Verify
        assertIs<EudiWalletImpl>(wallet)
        assertEquals(customPresentationManager, wallet.presentationManager)
        verify(exactly = 0) { builder.getDefaultPresentationManager(any(), any(), any(), any()) }
    }

    @Test
    fun `withLogger should use custom logger`() {
        // Setup
        val context: Context = mockk {
            every { applicationContext } returns this
            every { noBackupFilesDir } returns File("no-backup")
        }

        val capabilities = mockk<AndroidKeystoreSecureArea.Capabilities> {
            every { secureLockScreenSetup } returns true
            every { strongBoxSupported } returns true
        }

        val config = EudiWalletConfig()
        val customLogger: Logger = mockk()

        val builder = spyk(EudiWallet.Builder(context, config)) {
            every { getDefaultDocumentManager(any(), any()) } returns mockk(relaxed = true)
            every { getTransferManager(any(), any()) } returns mockk(relaxed = true)
            every { getDocumentStatusResolver() } returns mockk(relaxed = true)
            every { this@spyk.capabilities } returns capabilities
        }

        // Execute
        val wallet = builder.withLogger(customLogger).build()

        // Verify
        assertIs<EudiWalletImpl>(wallet)
        assertEquals(customLogger, wallet.logger)
    }

    @Test
    fun `withKtorHttpClientFactory should use custom HTTP client factory`() {
        // Setup
        val context: Context = mockk {
            every { applicationContext } returns this
            every { noBackupFilesDir } returns File("no-backup")
        }

        val capabilities = mockk<AndroidKeystoreSecureArea.Capabilities> {
            every { secureLockScreenSetup } returns true
            every { strongBoxSupported } returns true
        }

        val config = EudiWalletConfig()
        val customHttpClientFactory: () -> HttpClient = { mockk() }

        val builder = spyk(EudiWallet.Builder(context, config)) {
            every { getDefaultDocumentManager(any(), any()) } returns mockk(relaxed = true)
            every { getTransferManager(any(), any()) } returns mockk(relaxed = true)
            every { getDocumentStatusResolver() } answers {
                // Custom HTTP client factory should be used by DocumentStatusResolver
                mockk(relaxed = true)
            }
            every { this@spyk.capabilities } returns capabilities
        }

        // Execute
        val wallet = builder.withKtorHttpClientFactory(customHttpClientFactory).build()

        // Verify
        assertIs<EudiWalletImpl>(wallet)
        assertEquals(customHttpClientFactory, wallet.ktorHttpClientFactory)
    }

    @Test
    fun `withTransactionLogger should wrap presentation manager with transaction logger`() {
        // Setup
        val context: Context = mockk {
            every { applicationContext } returns this
            every { noBackupFilesDir } returns File("no-backup")
        }

        val capabilities = mockk<AndroidKeystoreSecureArea.Capabilities> {
            every { secureLockScreenSetup } returns true
            every { strongBoxSupported } returns true
        }

        val config = EudiWalletConfig()
        val customTransactionLogger: TransactionLogger = mockk()
        val documentManager: DocumentManager = mockk(relaxed = true)
        val defaultPresentationManager: PresentationManagerImpl = mockk(relaxed = true)

        val builder = spyk(EudiWallet.Builder(context, config)) {
            every { getDefaultDocumentManager(any(), any()) } returns documentManager
            every { getTransferManager(any(), any()) } returns mockk(relaxed = true)
            every {
                getDefaultPresentationManager(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns defaultPresentationManager
            every { getDocumentStatusResolver() } returns mockk(relaxed = true)
            every { this@spyk.capabilities } returns capabilities
            // Mock the wrapWithTrasactionLogger function to verify it's called
            every {
                with(this) {
                    defaultPresentationManager.wrapWithTrasactionLogger(any(), any())
                }
            } returns mockk(relaxed = true)
        }

        // Execute
        builder.withTransactionLogger(customTransactionLogger).build()

        // Verify
        verify(exactly = 1) {
            with(builder) { defaultPresentationManager.wrapWithTrasactionLogger(any(), any()) }
        }


    }

    @Test
    fun `withDocumentStatusResolver should use custom document status resolver`() {
        // Setup
        val context: Context = mockk {
            every { applicationContext } returns this
            every { noBackupFilesDir } returns File("no-backup")
        }

        val capabilities = mockk<AndroidKeystoreSecureArea.Capabilities> {
            every { secureLockScreenSetup } returns true
            every { strongBoxSupported } returns true
        }

        val config = EudiWalletConfig()
        val customDocumentStatusResolver: DocumentStatusResolver = mockk()

        val builder = spyk(EudiWallet.Builder(context, config)) {
            every { getDefaultDocumentManager(any(), any()) } returns mockk(relaxed = true)
            every { getTransferManager(any(), any()) } returns mockk(relaxed = true)
            every { this@spyk.capabilities } returns capabilities
        }

        // Execute
        val wallet = builder.withDocumentStatusResolver(customDocumentStatusResolver).build()

        // Verify
        assertIs<EudiWalletImpl>(wallet)
        assertEquals(customDocumentStatusResolver, wallet.documentStatusResolver)
        verify(exactly = 1) { builder.getDocumentStatusResolver() }
    }

    @Test
    fun `openId4VpManager should be created when config is set`() {
        // Setup
        val context: Context = mockk {
            every { applicationContext } returns this
            every { noBackupFilesDir } returns File("no-backup")
        }

        val capabilities = mockk<AndroidKeystoreSecureArea.Capabilities> {
            every { secureLockScreenSetup } returns true
            every { strongBoxSupported } returns true
        }

        val openId4VpConfig = OpenId4VpConfig.Builder()
            .withEncryptionAlgorithms(EncryptionAlgorithm.ECDH_ES)
            .withEncryptionMethods(EncryptionMethod.A192GCM)
            .withClientIdSchemes(ClientIdScheme.X509SanDns)
            .withSchemes("openid4vp")
            .withFormats(Format.MsoMdoc.ES256)
            .build()

        val config = EudiWalletConfig()
            .configureOpenId4Vp(openId4VpConfig)

        val builder = spyk(EudiWallet.Builder(context, config)) {
            every { getDefaultDocumentManager(any(), any()) } returns mockk(relaxed = true)
            every { getTransferManager(any(), any()) } returns mockk(relaxed = true)
            every { getDocumentStatusResolver() } returns mockk(relaxed = true)
            every { this@spyk.capabilities } returns capabilities
        }

        // Mock the OpenId4VpManager constructor
        mockkConstructor(eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpManager::class)

        // Execute
        val wallet = builder.build()

        // Verify
        assertIs<EudiWalletImpl>(wallet)
        assertIs<PresentationManagerImpl>(wallet.presentationManager)
        assertNotNull(wallet.presentationManager.openId4vpManager)
    }
}
