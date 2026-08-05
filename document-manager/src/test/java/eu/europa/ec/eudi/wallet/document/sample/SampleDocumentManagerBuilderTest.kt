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

package eu.europa.ec.eudi.wallet.document.sample

import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.DocumentManagerImpl
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.Storage
import org.multipaz.storage.ephemeral.EphemeralStorage
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class SampleDocumentManagerBuilderTest {

    val storageFixture: Storage = EphemeralStorage()
    val secureAreaFixture: SecureArea = runBlocking {
        SoftwareSecureArea.create(storageFixture)
    }

    @Test
    fun `build should throw exception when document manager is not set`() {
        // Given
        val builder = SampleDocumentManager.Builder()

        // When
        val exception = assertFailsWith<IllegalArgumentException> {
            builder.build()
        }

        // Then
        assertEquals("DocumentManager implementation must be set", exception.message)
    }

    @Test
    fun `build should return SampleDocumentManagerImpl with the provided documentManager as dependencies`() {
        // Given
        val delegate = mockk<DocumentManager>()
        val builder = SampleDocumentManager.Builder()
            .setDocumentManager(delegate)

        // When
        val documentManager = builder.build()

        // Then
        assertIs<SampleDocumentManagerImpl>(documentManager)
        assertSame(delegate, documentManager.delegate)
    }

    @Test
    fun `verify that companion object operator invoke returns SampleDocumentManagerImpl instance`() {
        // Given
        val delegate = mockk<DocumentManager>()

        // When
        val documentManager = SampleDocumentManager {
            documentManager = delegate
        }

        // Then
        assertIs<SampleDocumentManagerImpl>(documentManager)
        assertSame(delegate, documentManager.delegate)
    }

    @Test
    fun `verify that companion object build method returns SampleDocumentManagerImpl instance`() {

        // When
        val documentManager = SampleDocumentManager.build {
            setIdentifier("document_manager")
            setSecureAreaRepository(SecureAreaRepository.Builder().apply {
                add(secureAreaFixture)
            }.build())
            setStorage(storageFixture)
        }

        // Then
        assertIs<SampleDocumentManagerImpl>(documentManager)
        assertIs<DocumentManagerImpl>(documentManager.delegate)
    }
}