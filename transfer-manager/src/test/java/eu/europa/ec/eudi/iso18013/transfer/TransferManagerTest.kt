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
import io.mockk.mockk
import org.mockito.MockedStatic
import org.mockito.kotlin.mock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TransferManagerTest {

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
    fun `makeMsoMdocTransferManager method should return a MsoMdocTransferManager instance`() {
        val transferManager =
            TransferManager.getDefault(Context, createDocumentManager(null))

        assertIs<eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl>(transferManager)
        assertIs<DeviceRequestProcessor>(transferManager.requestProcessor)
    }

    @Test
    fun `makeMsoMdocTransferManager with deviceRetrievalMethods method should return a MsoMdocTransferManager instance`() {
        val deviceRetrievalMethods: List<DeviceRetrievalMethod> = listOf(mockk(), mock())
        val transferManager = TransferManager.getDefault(
            context = Context,
            documentManager = createDocumentManager(null),
            retrievalMethods = deviceRetrievalMethods
        )

        assertIs<eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl>(transferManager)
        assertIs<DeviceRequestProcessor>(transferManager.requestProcessor)
        assertEquals(deviceRetrievalMethods, transferManager.retrievalMethods)
    }

    @Test
    fun `makeMsoMdocTransferManager with readerTrustStore method should return a MsoMdocTransferManager instance`() {
        val readerTrustStore: ReaderTrustStore = mockk()
        val transferManager = TransferManager.getDefault(
            context = Context,
            documentManager = createDocumentManager(null),
            readerTrustStore = readerTrustStore
        )

        assertIs<eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl>(transferManager)
        assertIs<DeviceRequestProcessor>(transferManager.requestProcessor)
        assertEquals(readerTrustStore, transferManager.requestProcessor.readerTrustStore)
    }
}