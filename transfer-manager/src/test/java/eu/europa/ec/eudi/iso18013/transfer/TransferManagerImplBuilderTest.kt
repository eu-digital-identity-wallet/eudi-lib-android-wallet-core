/*
 * Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer

import android.util.Log
import eu.europa.ec.eudi.iso18013.transfer.engagement.DeviceRetrievalMethod
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.zkp.ZkResponsePolicy
import io.mockk.mockk
import org.mockito.MockedStatic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class TransferManagerImplBuilderTest {

    lateinit var mockLog: MockedStatic<Log>

    @BeforeTest
    fun setUp() {
        mockLog = mockAndroidLog()
    }

    @AfterTest
    fun tearDown() {
        mockLog.close()
    }

    @Test
    fun buildTransferManagerWithDefaults() {
        val transferManager = TransferManagerImpl.Builder(Context)
            .documentManager(createDocumentManager(null))
            .build()

        assertNotNull(transferManager)
        assertIs<DeviceRequestProcessor>(transferManager.requestProcessor)
    }

    @Test
    fun buildTransferManagerWithoutDocumentManagerThrowsException() {
        val throwable = assertFailsWith<IllegalArgumentException> {
            TransferManagerImpl.Builder(Context).build()
        }

        assertEquals("Document manager must be provided", throwable.message)
    }

    @Test
    fun buildTransferManagerWithRetrievalMethods() {
        val retrievalMethods: List<DeviceRetrievalMethod> = listOf(mockk(), mockk())
        val transferManager = TransferManagerImpl.Builder(Context)
            .documentManager(createDocumentManager(null))
            .retrievalMethods(retrievalMethods)
            .build()

        assertNotNull(transferManager)
        assertIs<DeviceRequestProcessor>(transferManager.requestProcessor)
        assertEquals(retrievalMethods, transferManager.retrievalMethods)
    }

    @Test
    fun buildTransferManagerWithDefaultZkResponsePolicy() {
        val transferManager = TransferManagerImpl.Builder(Context)
            .documentManager(createDocumentManager(null))
            .build()

        assertNotNull(transferManager)
        assertIs<DeviceRequestProcessor>(transferManager.requestProcessor)
        assertEquals(
            ZkResponsePolicy.Strict,
            (transferManager.requestProcessor as DeviceRequestProcessor).zkResponsePolicy
        )
    }

    @Test
    fun buildTransferManagerWithReaderTrustStore() {
        val readerTrustStore = mockk<ReaderTrustStore>()
        val transferManager = TransferManagerImpl.Builder(Context)
            .documentManager(createDocumentManager(null))
            .readerTrustStore(readerTrustStore)
            .build()

        assertNotNull(transferManager)
        assertIs<DeviceRequestProcessor>(transferManager.requestProcessor)
        assertEquals(readerTrustStore, transferManager.requestProcessor.readerTrustStore)
    }
}