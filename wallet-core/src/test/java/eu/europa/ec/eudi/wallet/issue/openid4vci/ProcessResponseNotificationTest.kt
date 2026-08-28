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

import eu.europa.ec.eudi.openid4vci.AuthorizedRequest
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.CredentialIssuanceEvent
import eu.europa.ec.eudi.openid4vci.IssuedCredential
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.NotificationId
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.multipaz.storage.Storage
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProcessResponseNotificationTest {

    private lateinit var documentManager: DocumentManager
    private lateinit var issuer: Issuer
    private lateinit var authorizedRequest: AuthorizedRequest
    private lateinit var issuedDocument: IssuedDocument
    private lateinit var unsignedDocument: UnsignedDocument

    private val emittedEvents = mutableListOf<IssueEvent>()
    private val listener = OpenId4VciManager.OnResult<IssueEvent> { emittedEvents.add(it) }

    @BeforeTest
    fun setup() {
        documentManager = mockk(relaxed = true)
        issuer = mockk(relaxed = true)
        authorizedRequest = mockk(relaxed = true)
        issuedDocument = mockk(relaxed = true)
        unsignedDocument = mockk(relaxed = true) {
            every { format } returns SdJwtVcFormat(vct = "test-vct")
        }
        emittedEvents.clear()
        every {
            documentManager.storeIssuedDocument(unsignedDocument, any())
        } returns Outcome.success(issuedDocument)
        coEvery {
            with(issuer) { authorizedRequest.notify(any()) }
        } returns Result.success(authorizedRequest)
    }

    private fun buildSut(): ProcessResponse = ProcessResponse(
        documentManager = documentManager,
        deferredContextFactory = { _, _, _, _ -> mockk(relaxed = true) },
        clientAttestationPopKeyId = null,
        listener = listener,
        issuedDocumentIds = mutableListOf(),
        deferredDocumentIds = mutableListOf(),
        logger = null,
        authorizedRequest = authorizedRequest,
        issuer = issuer,
        documentToConfigurationMap = mapOf(unsignedDocument to mockk(relaxed = true)),
        dpopKeyAlias = null,
        issuanceMetadataStorage = mockk<Storage>(relaxed = true),
        clientAuthentication = mockk<ClientAuthentication.None>(relaxed = true),
    )

    private fun successOutcome(notificationId: NotificationId?): SubmissionOutcome.Success =
        SubmissionOutcome.Success(
            credentials = listOf(IssuedCredential.string("dummy-credential")),
            notificationId = notificationId,
        )

    @Test
    fun `processSubmittedRequest sends Accepted notification when store succeeds and notificationId is present`() {
        val notifId = NotificationId("test-notif-id")
        val notifySent = CompletableDeferred<Unit>()
        val eventSlot = slot<CredentialIssuanceEvent>()

        coEvery {
            with(issuer) { authorizedRequest.notify(capture(eventSlot)) }
        } answers {
            notifySent.complete(Unit)
            Result.success(authorizedRequest)
        }

        buildSut().processSubmittedRequest(unsignedDocument, listOf("key1"), successOutcome(notifId))

        runBlocking { withTimeout(2_000) { notifySent.await() } }

        assertIs<CredentialIssuanceEvent.Accepted>(eventSlot.captured)
        assertEquals(notifId, eventSlot.captured.id)
    }

    @Test
    fun `processSubmittedRequest sends Failed notification when store fails and notificationId is present`() {
        val notifId = NotificationId("test-notif-id")
        val storeError = RuntimeException("disk full")
        val notifySent = CompletableDeferred<Unit>()
        val eventSlot = slot<CredentialIssuanceEvent>()

        every {
            documentManager.storeIssuedDocument(unsignedDocument, any())
        } returns Outcome.failure(storeError)
        coEvery {
            with(issuer) { authorizedRequest.notify(capture(eventSlot)) }
        } answers {
            notifySent.complete(Unit)
            Result.success(authorizedRequest)
        }

        buildSut().processSubmittedRequest(unsignedDocument, listOf("key1"), successOutcome(notifId))

        runBlocking { withTimeout(2_000) { notifySent.await() } }

        assertIs<CredentialIssuanceEvent.Failed>(eventSlot.captured)
        assertEquals(notifId, eventSlot.captured.id)
        assertEquals(storeError.message, eventSlot.captured.description)
    }

    @Test
    fun `processSubmittedRequest does not notify issuer when notificationId is absent`() {
        buildSut().processSubmittedRequest(unsignedDocument, listOf("key1"), successOutcome(null))

        coVerify(exactly = 0) {
            with(issuer) { authorizedRequest.notify(any()) }
        }
    }

    @Test
    fun `processSubmittedRequest emits DocumentIssued even when notification fails`() {
        val notifId = NotificationId("test-notif-id")
        val notifyReached = CompletableDeferred<Unit>()

        coEvery {
            with(issuer) { authorizedRequest.notify(any()) }
        } answers {
            notifyReached.complete(Unit)
            throw RuntimeException("network timeout")
        }

        buildSut().processSubmittedRequest(unsignedDocument, listOf("key1"), successOutcome(notifId))

        runBlocking { withTimeout(2_000) { notifyReached.await() } }

        assertTrue(emittedEvents.filterIsInstance<IssueEvent.DocumentIssued>().size == 1)
    }
}
