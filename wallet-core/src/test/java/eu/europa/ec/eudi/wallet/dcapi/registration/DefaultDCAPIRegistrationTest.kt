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

package eu.europa.ec.eudi.wallet.dcapi.registration

import android.content.Context
import androidx.credentials.registry.provider.RegisterCredentialsRequest
import androidx.credentials.registry.provider.RegistryManager
import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol
import eu.europa.ec.eudi.wallet.dcapi.internal.getAppName
import eu.europa.ec.eudi.wallet.dcapi.internal.getLocale
import eu.europa.ec.eudi.wallet.dcapi.internal.getMatcher
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Tstr
import org.multipaz.document.NameSpacedData
import java.util.Locale
import kotlin.test.assertEquals

class DefaultDCAPIRegistrationTest {

    private val context = mockk<Context>(relaxed = true)
    private val registryManager = mockk<RegistryManager>()

    /** The calls made while registering, in order. */
    private val events = mutableListOf<String>()
    private val registered = mutableListOf<RegisterCredentialsRequest>()

    @Before
    fun setUp() {
        mockkObject(RegistryManager.Companion)
        every { RegistryManager.create(any()) } returns registryManager
        coEvery { registryManager.clearCredentialRegistry(any()) } answers {
            events += "clear"
            mockk(relaxed = true)
        }
        coEvery { registryManager.registerCredentials(capture(registered)) } answers {
            events += "register"
            mockk(relaxed = true)
        }
        mockkStatic(Context::getAppName, Context::getLocale, Context::getMatcher)
        every { context.getAppName() } returns "Wallet"
        every { context.getLocale() } returns Locale.ENGLISH
        every { context.getMatcher(any()) } returns byteArrayOf(0x00)
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `a document whose data cannot be read is left out and the other documents are registered`(): Unit =
        runBlocking {
            val unreadable = mockk<IssuedDocument> {
                every { id } returns "unreadable"
                every { name } returns "Unreadable document"
                every { format } returns MsoMdocFormat(DOC_TYPE)
                every { issuerMetadata } returns null
                every { data } throws StackOverflowError()
            }

            registration(listOf(unreadable, mdocDocument())).registerCredentials()

            val credentials = CBORObject.DecodeFromBytes(registered.single().credentials)["credentials"]
            assertEquals(listOf("Readable document"), credentials.values.map { it["title"].AsString() })
        }

    @Test
    fun `the current registrations are cleared after the new ones are built`(): Unit = runBlocking {
        registration(listOf(mdocDocument { events += "read" })).registerCredentials()

        assertEquals(listOf("read", "clear", "register"), events)
    }

    private fun registration(documents: List<IssuedDocument>): DefaultDCAPIRegistration {
        val documentManager = mockk<DocumentManager> {
            every { getDocuments(predicate = any()) } returns documents
            every { getDocuments(predicate = null) } returns documents
        }
        return DefaultDCAPIRegistration(
            context = context,
            documentManager = documentManager,
            supportedProtocols = listOf(DCAPIProtocol.ISO_MDOC),
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun mdocDocument(onDataRead: () -> Unit = {}): IssuedDocument {
        val nameSpacedData = NameSpacedData.Builder()
            .putEntry(NAME_SPACE, "family_name", Cbor.encode(Tstr("Doe")))
            .build()
        return mockk {
            every { id } returns "readable"
            every { name } returns "Readable document"
            every { format } returns MsoMdocFormat(DOC_TYPE)
            every { issuerMetadata } returns null
            every { data } answers {
                onDataRead()
                MsoMdocData(MsoMdocFormat(DOC_TYPE), issuerMetadata = null, nameSpacedData = nameSpacedData)
            }
        }
    }

    private companion object {
        const val DOC_TYPE = "org.iso.18013.5.1.mDL"
        const val NAME_SPACE = "org.iso.18013.5.1"
    }
}
