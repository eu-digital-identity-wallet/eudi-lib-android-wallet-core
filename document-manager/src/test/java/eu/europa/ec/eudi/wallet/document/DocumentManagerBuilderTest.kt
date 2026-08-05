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

package eu.europa.ec.eudi.wallet.document

import kotlinx.coroutines.runBlocking
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.ephemeral.EphemeralStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class DocumentManagerBuilderTest {

    val storage = EphemeralStorage()
    val secureArea = runBlocking { SoftwareSecureArea.create(storage) }
    val secureAreaRepository = SecureAreaRepository.Builder().apply {
        add(secureArea)
    }.build()

    @Test
    fun `build should throw exception when identifier is not set`() {
        // Given
        val builder = DocumentManager.Builder()
            .setStorage(storage)
            .setSecureAreaRepository(secureAreaRepository)

        // When
        val exception = assertFailsWith<IllegalArgumentException> {
            builder.build()
        }

        // Then
        assertEquals("Identifier is required", exception.message)
    }

    @Test
    fun `build should throw exception when storage is not set`() {
        // Given
        val builder = DocumentManager.Builder()
            .setIdentifier("document_manager")
            .setSecureAreaRepository(secureAreaRepository)

        // When
        val exception = assertFailsWith<IllegalArgumentException> {
            builder.build()
        }

        // Then
        assertEquals("Storage is required", exception.message)
    }

    @Test
    fun `build should throw exception when secureAreaRepository is not set`() {
        // Given
        val builder = DocumentManager.Builder()
            .setIdentifier("document_manager")
            .setStorage(storage)

        // When
        val exception = assertFailsWith<IllegalArgumentException> {
            builder.build()
        }

        // Then
        assertEquals("SecureAreaRepository is required", exception.message)
    }

    @Test
    fun `build should return DocumentManagerImpl with the provided storageEngine and secureArea as dependencies`() {
        val builder = DocumentManager.Builder()
            .setIdentifier("document_manager")
            .setStorage(storage)
            .setSecureAreaRepository(secureAreaRepository)

        val documentManager = builder.build()

        assertIs<DocumentManagerImpl>(documentManager)
        assertEquals("document_manager", documentManager.identifier)
        assertEquals(storage, documentManager.storage)
        assertEquals(
            secureArea,
            runBlocking { documentManager.secureAreaRepository.getImplementation(secureArea.identifier) })
    }

    @Test
    fun `verify that companion object operator invoke returns DocumentManagerImpl instance`() {
        // When
        val documentManager = DocumentManager {
            setIdentifier("document_manager")
            setStorage(this@DocumentManagerBuilderTest.storage)
            setSecureAreaRepository(SecureAreaRepository.Builder().apply {
                add(secureArea)
            }.build())
        }

        // Then
        assertIs<DocumentManagerImpl>(documentManager)
        assertEquals("document_manager", documentManager.identifier)
        assertEquals(storage, documentManager.storage)
        assertEquals(
            secureArea,
            runBlocking { documentManager.secureAreaRepository.getImplementation(secureArea.identifier) })
    }

    @Test
    fun `verify that setSecureAreaRepository method overrides the default`() {
        val builder = DocumentManager.Builder()
            .setIdentifier("document_manager")
            .setStorage(storage)
            .setSecureAreaRepository(secureAreaRepository)

        builder.setSecureAreaRepository(secureAreaRepository)

        val documentManager = builder.build() as DocumentManagerImpl

        assertSame(secureAreaRepository, documentManager.secureAreaRepository)
    }

    @Test
    fun `verify that setIdentifier method sets the identifier correctly`() {
        // Given
        val builder = DocumentManager.Builder()

        // When
        builder.setIdentifier("custom_identifier")
            .setStorage(storage)
            .setSecureAreaRepository(secureAreaRepository)

        val documentManager = builder.build()

        // Then
        assertEquals("custom_identifier", documentManager.identifier)
    }

    @Test
    fun `verify that setStorage method sets the storage correctly`() {
        // Given
        val customStorage = EphemeralStorage()
        val builder = DocumentManager.Builder()

        // When
        builder.setIdentifier("document_manager")
            .setStorage(customStorage)
            .setSecureAreaRepository(secureAreaRepository)

        val documentManager = builder.build()

        // Then
        assertSame(customStorage, documentManager.storage)
    }

    @Test
    fun `verify builder fluent interface allows method chaining`() {
        // Given & When
        val builder = DocumentManager.Builder()
        val result1 = builder.setIdentifier("document_manager")
        val result2 = result1.setStorage(storage)
        val result3 = result2.setSecureAreaRepository(secureAreaRepository)

        // Then
        assertSame(builder, result1, "setIdentifier should return the same builder instance")
        assertSame(builder, result2, "setStorage should return the same builder instance")
        assertSame(
            builder,
            result3,
            "setSecureAreaRepository should return the same builder instance"
        )

        // Verify build works after chaining
        val documentManager = result3.build()
        assertIs<DocumentManagerImpl>(documentManager)
    }

    @Test
    fun `verify builder can be reused to create multiple DocumentManager instances`() {
        // Given
        val builder = DocumentManager.Builder()
            .setIdentifier("document_manager_1")
            .setStorage(storage)
            .setSecureAreaRepository(secureAreaRepository)

        // When
        val documentManager1 = builder.build()

        // Change identifier for second instance
        builder.setIdentifier("document_manager_2")
        val documentManager2 = builder.build()

        // Then
        assertIs<DocumentManagerImpl>(documentManager1)
        assertIs<DocumentManagerImpl>(documentManager2)
        assertEquals("document_manager_1", documentManager1.identifier)
        assertEquals("document_manager_2", documentManager2.identifier)
        // Storage and secure area should be the same
        assertSame(documentManager1.storage, documentManager2.storage)
        assertSame(documentManager1.secureAreaRepository, documentManager2.secureAreaRepository)
    }
}

