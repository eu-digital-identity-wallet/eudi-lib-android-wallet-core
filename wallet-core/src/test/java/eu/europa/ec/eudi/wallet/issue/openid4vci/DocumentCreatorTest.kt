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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerId
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.CredentialReusePolicy
import eu.europa.ec.eudi.openid4vci.IssuanceRequestPayload
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.logging.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DocumentCreatorTest {


    @Test
    fun `assert continuation is not null when resume is executed from event`() = runTest {
        val docTypeMock = "docType"
        val documentFormatMock = MsoMdocFormat(docType = docTypeMock)
        val credentialConfigurationMock = mockk<MsoMdocCredential> {
            every { docType } returns docTypeMock
            every { credentialMetadata } returns mockk {
                every { claims } returns emptyList()
                every { display } returns emptyList()
            }
        }
        val configurationIdentifiers = listOf(
            CredentialConfigurationIdentifier("id")
        )
        val createSettings = mockk<CreateDocumentSettings>()
        val credentialIssuerId = CredentialIssuerId("https://issuer.example.com").getOrThrow()
        val credentialOfferMock = mockk<CredentialOffer>(relaxed = true)
        every { credentialOfferMock.credentialIssuerIdentifier } returns credentialIssuerId
        val offerMock = mockk<Offer>(relaxed = true) {
            every { credentialOffer } returns credentialOfferMock
        }
        val offeredDocument = mockk<Offer.OfferedDocument> {
            every { offer } returns offerMock
            every { configurationIdentifier } returns configurationIdentifiers.first()
            every { configuration } returns credentialConfigurationMock
            every { credentialReusePolicy } returns CredentialReusePolicy.None
            every { documentFormat } answers { callOriginal() }
        }
        val unsignedDocument = mockk<UnsignedDocument>(relaxed = true)
        val result = Outcome.success(unsignedDocument)

        val documentManager = mockk<DocumentManager> {
            every { createDocument(documentFormatMock, createSettings, any()) } returns result
        }

        val listener = OpenId4VciManager.OnResult<IssueEvent> { event ->
            CoroutineScope(Dispatchers.Default).launch {
                when (event) {
                    is IssueEvent.DocumentRequiresCreateSettings.OptionalReusePolicy -> {
                        event.resume(createSettings)
                    }

                    else -> {
                        // do nothing
                    }
                }
            }
        }

        val creator = DocumentCreator(
            documentManager = documentManager,
            listener = listener,
            logger = Logger {
                println(it)
            }
        )


        val unsignedDocumentResult = creator.createDocument(offeredDocument)
        assertEquals(unsignedDocument, unsignedDocumentResult)
    }

    @Test
    fun `deletes the documents already created when creating a later document fails`() = runTest {
        val createSettings = mockk<CreateDocumentSettings>()
        val createdDocument = mockk<UnsignedDocument>(relaxed = true) {
            every { id } returns "created"
        }
        val failure = IllegalStateException("Key creation failed")
        val documentManager = mockk<DocumentManager> {
            every { createDocument(MsoMdocFormat("first"), createSettings, any()) } returns
                Outcome.success(createdDocument)
            every { createDocument(MsoMdocFormat("second"), createSettings, any()) } returns
                Outcome.failure(failure)
            every { deleteDocumentById("created") } returns Outcome.success(null)
        }
        val listener = OpenId4VciManager.OnResult<IssueEvent> { event ->
            if (event is IssueEvent.DocumentRequiresCreateSettings.OptionalReusePolicy) {
                CoroutineScope(Dispatchers.Default).launch { event.resume(createSettings) }
            }
        }
        val creator = DocumentCreator(documentManager = documentManager, listener = listener)

        val thrown = assertFailsWith<IllegalStateException> {
            creator.createDocuments(
                listOf(
                    offeredDocument("first") to mockk<IssuanceRequestPayload>(),
                    offeredDocument("second") to mockk<IssuanceRequestPayload>(),
                )
            )
        }

        assertSame(failure, thrown)
        verify(exactly = 1) { documentManager.deleteDocumentById("created") }
    }

    private fun offeredDocument(type: String): Offer.OfferedDocument {
        val configuration = mockk<MsoMdocCredential> {
            every { docType } returns type
            every { credentialMetadata } returns mockk {
                every { claims } returns emptyList()
                every { display } returns emptyList()
            }
        }
        val credentialOffer = mockk<CredentialOffer>(relaxed = true) {
            every { credentialIssuerIdentifier } returns
                CredentialIssuerId("https://issuer.example.com").getOrThrow()
        }
        return mockk {
            every { offer } returns mockk(relaxed = true) {
                every { this@mockk.credentialOffer } returns credentialOffer
            }
            every { configurationIdentifier } returns CredentialConfigurationIdentifier(type)
            every { this@mockk.configuration } returns configuration
            every { credentialReusePolicy } returns CredentialReusePolicy.None
            every { documentFormat } answers { callOriginal() }
        }
    }
}