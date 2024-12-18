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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.logging.Logger
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentCreatorTest {


    @Test
    fun `assert continuation is not null when resume is executed from event`() = runTest {
        val docTypeMock = "docType"
        val documentFormatMock = MsoMdocFormat(docType = docTypeMock)
        val credentialConfigurationMock = mockk<MsoMdocCredential> {
            every { docType } returns docTypeMock
            every { claims } returns emptyMap()
            every { display } returns emptyList()
        }
        val createSettings = mockk<CreateDocumentSettings>()
        val offeredDocument = mockk<Offer.OfferedDocument> {
            every { offer } returns mockk(relaxed = true)
            every { configurationIdentifier } returns CredentialConfigurationIdentifier("id")
            every { configuration } returns credentialConfigurationMock
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
                    is IssueEvent.DocumentRequiresCreateSettings -> {
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
}