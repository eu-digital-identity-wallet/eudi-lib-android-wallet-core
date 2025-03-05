/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing

import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.metadata.DocumentMetaData
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedClaim
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedDocument
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParserTest {

    @Before
    fun setup() {
        mockkStatic(::parseMsoMdoc)
        mockkStatic(::parseVp)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun parsePresentationTransactionLog_validCborTransactionLog_returnsPresentationTransactionLog() {
        // Setup mock response
        val mockDocuments = listOf(
            PresentedDocument(
                format = MsoMdocFormat(docType = "org.iso.18013.5.1.mDL"),
                metadata = null,
                claims = listOf(
                    PresentedClaim(
                        path = listOf("org.iso.18013.5.1", "family_name"),
                        value = "Doe",
                        rawValue = byteArrayOf(0x01, 0x02),
                        metadata = null
                    )
                )
            )
        )
        
        every { 
            parseMsoMdoc(
                rawResponse = any(), 
                sessionTranscript = any(), 
                metadata = any()
            ) 
        } returns mockDocuments

        val relyingParty = TransactionLog.RelyingParty(
            name = "RelyingParty",
            certificateChain = listOf(),
            isVerified = true,
            readerAuth = ""
        )
        val transactionLog = TransactionLog(
            type = TransactionLog.Type.Presentation,
            dataFormat = TransactionLog.DataFormat.Cbor,
            rawRequest = byteArrayOf(0x01),
            rawResponse = byteArrayOf(0x02),
            relyingParty = relyingParty,
            timestamp = System.currentTimeMillis(),
            status = TransactionLog.Status.Completed,
            sessionTranscript = byteArrayOf(0x03),
            metadata = listOf("{}")
        )
        
        val result = parsePresentationTransactionLog(transactionLog)
        
        assertNotNull(result)
        assertEquals(relyingParty, result.relyingParty)
        assertEquals(transactionLog.status, result.status)
        assertEquals(1, result.documents.size)
        assertEquals("org.iso.18013.5.1.mDL", (result.documents.first().format as MsoMdocFormat).docType)
        assertEquals(1, result.documents.first().claims.size)
        assertEquals("Doe", result.documents.first().claims.first().value)
    }

    @Test
    fun parsePresentationTransactionLog_validJsonTransactionLog_returnsPresentationTransactionLog() {
        // Setup mock response
        val mockDocuments = listOf(
            PresentedDocument(
                format = SdJwtVcFormat(vct = "https://credentials.example.com/identity_credential"),
                metadata = null,
                claims = listOf(
                    PresentedClaim(
                        path = listOf("given_name"),
                        value = "John",
                        rawValue = JsonPrimitive("John"),
                        metadata = null
                    )
                )
            )
        )
        
        every { 
            parseVp(
                rawResponse = any(), 
                metadata = any()
            ) 
        } returns mockDocuments

        val relyingParty = TransactionLog.RelyingParty(
            name = "RelyingParty",
            certificateChain = listOf(),
            isVerified = true,
            readerAuth = ""
        )
        val transactionLog = TransactionLog(
            type = TransactionLog.Type.Presentation,
            dataFormat = TransactionLog.DataFormat.Json,
            rawRequest = byteArrayOf(0x01),
            rawResponse = byteArrayOf(0x02),
            relyingParty = relyingParty,
            timestamp = System.currentTimeMillis(),
            status = TransactionLog.Status.Completed,
            sessionTranscript = null,
            metadata = listOf("{}")
        )
        
        val result = parsePresentationTransactionLog(transactionLog)
        
        assertNotNull(result)
        assertEquals(relyingParty, result.relyingParty)
        assertEquals(transactionLog.status, result.status)
        assertEquals(1, result.documents.size)
        assertTrue(result.documents.first().format is SdJwtVcFormat)
        assertEquals(1, result.documents.first().claims.size)
        assertEquals("John", result.documents.first().claims.first().value)
    }

    @Test
    fun parsePresentationTransactionLog_invalidType_throwsIllegalArgumentException() {
        val relyingParty = TransactionLog.RelyingParty(
            name = "RelyingParty",
            certificateChain = listOf(),
            isVerified = true,
            readerAuth = ""
        )
        val transactionLog = TransactionLog(
            type = TransactionLog.Type.Issuance,
            dataFormat = TransactionLog.DataFormat.Json,
            rawRequest = byteArrayOf(0x01),
            rawResponse = byteArrayOf(0x02),
            relyingParty = relyingParty,
            timestamp = System.currentTimeMillis(),
            status = TransactionLog.Status.Completed,
            sessionTranscript = null,
            metadata = null
        )
        assertThrows(IllegalArgumentException::class.java) {
            parsePresentationTransactionLog(transactionLog)
        }
    }

    @Test
    fun parsePresentationTransactionLog_nullDataFormat_throwsIllegalArgumentException() {
        val relyingParty = TransactionLog.RelyingParty(
            name = "RelyingParty",
            certificateChain = listOf(),
            isVerified = true,
            readerAuth = ""
        )
        val transactionLog = TransactionLog(
            type = TransactionLog.Type.Presentation,
            dataFormat = null,
            rawRequest = byteArrayOf(0x01),
            rawResponse = byteArrayOf(0x02),
            relyingParty = relyingParty,
            timestamp = System.currentTimeMillis(),
            status = TransactionLog.Status.Completed,
            sessionTranscript = null,
            metadata = null
        )
        assertThrows(IllegalArgumentException::class.java) {
            parsePresentationTransactionLog(transactionLog)
        }
    }

    @Test
    fun parsePresentationTransactionLog_nullRawRequest_throwsIllegalArgumentException() {
        val relyingParty = TransactionLog.RelyingParty(
            name = "RelyingParty",
            certificateChain = listOf(),
            isVerified = true,
            readerAuth = ""
        )
        val transactionLog = TransactionLog(
            type = TransactionLog.Type.Presentation,
            dataFormat = TransactionLog.DataFormat.Json,
            rawRequest = null,
            rawResponse = byteArrayOf(0x02),
            relyingParty = relyingParty,
            timestamp = System.currentTimeMillis(),
            status = TransactionLog.Status.Completed,
            sessionTranscript = null,
            metadata = null
        )
        assertThrows(IllegalArgumentException::class.java) {
            parsePresentationTransactionLog(transactionLog)
        }
    }

    @Test
    fun parsePresentationTransactionLog_nullRawResponse_throwsIllegalArgumentException() {
        val relyingParty = TransactionLog.RelyingParty(
            name = "RelyingParty",
            certificateChain = listOf(),
            isVerified = true,
            readerAuth = ""
        )
        val transactionLog = TransactionLog(
            type = TransactionLog.Type.Presentation,
            dataFormat = TransactionLog.DataFormat.Json,
            rawRequest = byteArrayOf(0x01),
            rawResponse = null,
            relyingParty = relyingParty,
            timestamp = System.currentTimeMillis(),
            status = TransactionLog.Status.Completed,
            sessionTranscript = null,
            metadata = null
        )
        assertThrows(IllegalArgumentException::class.java) {
            parsePresentationTransactionLog(transactionLog)
        }
    }

    @Test
    fun parsePresentationTransactionLog_nullRelyingParty_throwsIllegalArgumentException() {
        val transactionLog = TransactionLog(
            type = TransactionLog.Type.Presentation,
            dataFormat = TransactionLog.DataFormat.Json,
            rawRequest = byteArrayOf(0x01),
            rawResponse = byteArrayOf(0x02),
            relyingParty = null,
            timestamp = System.currentTimeMillis(),
            status = TransactionLog.Status.Completed,
            sessionTranscript = null,
            metadata = null
        )
        assertThrows(IllegalArgumentException::class.java) {
            parsePresentationTransactionLog(transactionLog)
        }
    }
}
