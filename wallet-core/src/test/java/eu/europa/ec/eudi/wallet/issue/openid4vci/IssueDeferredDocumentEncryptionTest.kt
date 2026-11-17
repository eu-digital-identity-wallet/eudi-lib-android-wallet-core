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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.internal.crypto.AeadCipher
import eu.europa.ec.eudi.wallet.internal.crypto.DataSealer
import eu.europa.ec.eudi.wallet.internal.crypto.SealedDataUnreadableException
import eu.europa.ec.eudi.wallet.internal.crypto.SoftwareAeadCipher
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.security.ProviderException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.multipaz.storage.ephemeral.EphemeralStorage
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class IssueDeferredDocumentEncryptionTest {

    private val tokenCipher = SoftwareAeadCipher()
    private val documentManager = mockk<DocumentManager>()
    private var requestSent = false

    // A context stored before the tokens were encrypted
    private val plainContext = """
        {"credentialIssuerId":"https://issuer.example.com","deferredEndpoint":"https://issuer.example.com/deferred","tokenEndpoint":"https://auth.example.com/token","authorizationServerId":"https://auth.example.com","clientId":"wallet-client","popKeyAliases":["key-1"],"transactionId":"tx-secret","accessToken":"at-secret","accessTokenType":"Bearer","refreshToken":"rt-secret"}
    """.trimIndent().toByteArray()

    @Test
    fun `a context stored in plain text is stored encrypted before the request`() = runBlocking {
        val deferredDocument = deferredDocument(plainContext)
        every { documentManager.getDocumentById(DOCUMENT_ID) } returns deferredDocument(plainContext)
        val stored = slot<ByteArray>()
        val requestSentWhenStored = mutableListOf<Boolean>()
        every { documentManager.storeDeferredDocument(any(), capture(stored)) } answers {
            requestSentWhenStored += requestSent
            Outcome.success(deferredDocument(stored.captured))
        }

        // The issuer answers with an error, so the deferred issuance fails after the request
        val result = assertIs<DeferredIssueResult.DocumentFailed>(issueDeferred(deferredDocument))

        assertEquals(listOf(false), requestSentWhenStored)
        // The result has the document as stored, so it can be issued again
        assertContentEquals(stored.captured, assertIs<DeferredDocument>(result.document).relatedData)
        val storedText = stored.captured.toString(Charsets.UTF_8)
        listOf("tx-secret", "at-secret", "rt-secret").forEach { assertFalse(it in storedText) }
        val transaction = readContext(stored.captured).issuanceContext.authorizedTransaction
        assertEquals("tx-secret", transaction.transactionId.value)
        assertEquals("rt-secret", transaction.authorizedRequest.refreshToken?.refreshToken)
    }

    @Test
    fun `a deferred issuance stops when the context stored in plain text changed since it was read`() = runBlocking {
        val deferredDocument = deferredDocument(plainContext)
        every { documentManager.getDocumentById(DOCUMENT_ID) } returns
            deferredDocument("""{"changed":true}""".toByteArray())

        val result = issueDeferred(deferredDocument)

        assertIs<IllegalStateException>(assertIs<DeferredIssueResult.DocumentFailed>(result).cause)
        verify(exactly = 0) { documentManager.storeDeferredDocument(any(), any()) }
        assertFalse(requestSent)
    }

    @Test
    fun `no request is sent when the context could not be stored again`() = runBlocking {
        val deferredDocument = deferredDocument(readContext(plainContext).toBytes(DOCUMENT_ID, DataSealer(tokenCipher)))
        val encryptionFails = object : AeadCipher {
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray) =
                throw ProviderException("Keystore operation failed")
            override fun decrypt(ivAndCiphertext: ByteArray, associatedData: ByteArray) =
                tokenCipher.decrypt(ivAndCiphertext, associatedData)
        }

        assertIs<DeferredIssueResult.DocumentFailed>(issueDeferred(deferredDocument, encryptionFails))
        assertFalse(requestSent)
    }

    @Test
    fun `a context whose tokens cannot be decrypted fails without a request`() = runBlocking {
        val encryptedWithAnotherKey = readContext(plainContext).toBytes(DOCUMENT_ID, DataSealer(SoftwareAeadCipher()))

        val result = issueDeferred(deferredDocument(encryptedWithAnotherKey))

        assertIs<SealedDataUnreadableException>(assertIs<DeferredIssueResult.DocumentFailed>(result).cause)
        assertFalse(requestSent)
    }

    private suspend fun issueDeferred(
        deferredDocument: DeferredDocument,
        cipher: AeadCipher = tokenCipher,
    ): DeferredIssueResult {
        val manager = DefaultOpenId4VciManager(
            context = mockk(relaxed = true),
            documentManager = documentManager,
            walletAttestationKeyManager = mockk(),
            config = OpenId4VciManager.Config(
                clientAuthenticationType = OpenId4VciManager.ClientAuthenticationType.None("wallet-client"),
                authFlowRedirectionURI = "app://redirect",
                dpopConfig = DPopConfig.Disabled,
                issuanceMetadataStorage = EphemeralStorage(),
            ),
            ktorHttpClientFactory = {
                HttpClient(MockEngine {
                    requestSent = true
                    respondError(HttpStatusCode.InternalServerError)
                })
            },
            tokenCipher = cipher,
        )
        val result = CompletableDeferred<DeferredIssueResult>()
        manager.issueDeferredDocument(deferredDocument, Executor { it.run() }) { result.complete(it) }
        return withTimeout(10_000) { result.await() }
    }

    private fun deferredDocument(relatedData: ByteArray) = mockk<DeferredDocument>(relaxed = true) {
        every { id } returns DOCUMENT_ID
        every { this@mockk.relatedData } returns relatedData
    }

    private fun readContext(bytes: ByteArray) = DeferredContext.fromBytes(
        bytes,
        documentId = DOCUMENT_ID,
        tokenSealer = DataSealer(tokenCipher),
        walletKeyManager = mockk(),
        walletInstanceAttestationProvider = null,
    )

    private companion object {
        const val DOCUMENT_ID = "deferred-doc"
    }
}
