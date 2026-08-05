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

package eu.europa.ec.eudi.wallet.document.sample

import android.util.Log
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentManagerImpl
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.getResourceAsText
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.securearea.software.SoftwareCreateKeySettings
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.Storage
import org.multipaz.storage.ephemeral.EphemeralStorage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SampleDocumentManagerImplTest {

    companion object {
        lateinit var documentManager: SampleDocumentManagerImpl
        lateinit var secureArea: SecureArea
        lateinit var storage: Storage

        @OptIn(ExperimentalEncodingApi::class)
        val sampleDocuments
            get() = getResourceAsText("sample_documents.txt").let {
                Base64.Default.decode(it)
            }

        @BeforeClass
        @JvmStatic
        fun setUp() {

            mockkStatic(Log::class)
            every { Log.v(any(), any()) } returns 0
            every { Log.v(any(), any(), any()) } returns 0
            every { Log.d(any(), any()) } returns 0
            every { Log.d(any(), any(), any()) } returns 0
            every { Log.i(any(), any()) } returns 0
            every { Log.i(any(), any(), any()) } returns 0
            every { Log.e(any(), any()) } returns 0
            every { Log.e(any(), any(), any()) } returns 0
            every { Log.w(any(), any<Throwable>()) } returns 0
            every { Log.w(any(), any<String>()) } returns 0
            every { Log.w(any(), any(), any()) } returns 0

            storage = EphemeralStorage()
            secureArea = runBlocking { SoftwareSecureArea.create(storage) }
            val secureAreaRepository = SecureAreaRepository.Builder().apply {
                add(secureArea)
            }.build()
            documentManager = SampleDocumentManagerImpl(
                DocumentManagerImpl(
                    identifier = SampleDocumentManagerImpl::class.simpleName!!,
                    storage = storage,
                    secureAreaRepository = secureAreaRepository
                )
            )
            val createKeySettings = SoftwareCreateKeySettings.Builder().build()
            val loadResult = documentManager.loadMdocSampleDocuments(
                sampleData = sampleDocuments,
                createSettings = CreateDocumentSettings(
                    secureAreaIdentifier = secureArea.identifier,
                    createKeySettings = createKeySettings,

                ),
                documentNamesMap = mapOf(
                    "eu.europa.ec.eudi.pid.1" to "EU PID",
                    "org.iso.18013.5.1.mDL" to "mDL"
                )
            )
            assertTrue(loadResult.isSuccess)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            clearAllMocks()
            documentManager.getDocuments().forEach { documentManager.deleteDocumentById(it.id) }
        }
    }

    @Test
    fun `getDocuments should return the two sample documents`() {
        val documents = documentManager.getDocuments()
        assertEquals(2, documents.size)
    }

    @Test
    fun `documents should have their names set according to namesMap given to load method`() {
        val documents = documentManager.getDocuments()
        assertTrue(documents.any { it.name == "EU PID" })
        assertTrue(documents.any { it.name == "mDL" })
    }

    @Test
    fun `getDocuments using query with docType should return the appropriate document`() {
        val documents = documentManager.getDocuments().filter {
            (it.format as MsoMdocFormat).docType == "eu.europa.ec.eudi.pid.1"
        }
        assertEquals(1, documents.size)
        val document = documents.first()
        assertEquals("EU PID", document.name)
    }

    @Test
    fun `deleteDocumentById should delete the document`() {
        val storage = EphemeralStorage()
        val secureArea = runBlocking { SoftwareSecureArea.create(storage) }
        val secureAreaRepository = SecureAreaRepository.Builder().apply {
            add(secureArea)
        }.build()
        val documentManager = SampleDocumentManagerImpl(
            DocumentManagerImpl(
                identifier = SampleDocumentManagerImpl::class.simpleName!!,
                storage = storage,
                secureAreaRepository = secureAreaRepository
            )
        )
        val createKeySettings = SoftwareCreateKeySettings.Builder().build()
        documentManager.loadMdocSampleDocuments(
            sampleData = sampleDocuments,
            createSettings = CreateDocumentSettings(
                secureAreaIdentifier = secureArea.identifier,
                createKeySettings = createKeySettings
            ),
            documentNamesMap = mapOf(
                "eu.europa.ec.eudi.pid.1" to "EU PID",
                "org.iso.18013.5.1.mDL" to "mDL"
            )
        )

        val document = documentManager.getDocuments().first()
        val documentId = document.id

        val deleteResult = documentManager.deleteDocumentById(documentId)

        assertTrue(deleteResult.isSuccess)
        val documents = documentManager.getDocuments()
        assertEquals(1, documents.size)
    }

    @Test
    fun `issued document nameSpacedDataValues parses correctly the cbor values`() = runTest {
        val document = documentManager.getDocuments().first {
            (it.format as MsoMdocFormat).docType == "org.iso.18013.5.1.mDL"
        }

        assertIs<IssuedDocument>(document)
        val data = (document.data as MsoMdocData).nameSpacedDataDecoded
        assertEquals(1, data.size)
        assertEquals("org.iso.18013.5.1", data.keys.first())
    }

    @Test
    fun `getDocumentById should return the document`() {
        val firstDocument = documentManager.getDocuments().first()
        val documentId = firstDocument.id
        val document = documentManager.getDocumentById(documentId)

        assertNotNull(document)
        assertEquals(firstDocument.id, document.id)
    }
}