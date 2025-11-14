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

package eu.europa.ec.eudi.wallet.transactionLogging.presentation

import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuth
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VerifiablePresentations
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.openid4vp.dcql.ClaimsQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.Credentials
import eu.europa.ec.eudi.openid4vp.dcql.DCQL
import eu.europa.ec.eudi.openid4vp.dcql.DCQLMetaMsoMdocExtensions
import eu.europa.ec.eudi.openid4vp.dcql.MsoMdocDocType
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transfer.openId4vp.FORMAT_MSO_MDOC
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpResponse
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.cert.X509Certificate
import java.time.Instant

class TransactionLogBuilderTest {

    private lateinit var metadataResolver: (Response) -> List<String>
    private lateinit var builder: TransactionLogBuilder

    @Before
    fun setup() {
        metadataResolver = { _ ->
            listOf("Metadata 1", "Metadata 2")
        }
        builder = TransactionLogBuilder(metadataResolver)
    }

    @Test
    fun `createEmptyPresentationLog creates a log with correct initial values`() {
        val log = builder.createEmptyPresentationLog()

        assertEquals(TransactionLog.Status.Incomplete, log.status)
        assertEquals(TransactionLog.Type.Presentation, log.type)
        assertNull(log.relyingParty)
        assertNull(log.rawRequest)
        assertNull(log.rawResponse)
        assertNull(log.dataFormat)
        assertNull(log.sessionTranscript)
        assertNull(log.metadata)
        assertTrue(log.timestamp > 0)
    }

    @Test
    fun `withRequest with DeviceRequest updates log correctly`() {
        val initialLog = builder.createEmptyPresentationLog()
        val deviceRequestBytes = "test request".toByteArray()
        val deviceRequest = mockk<DeviceRequest> {
            every { this@mockk.deviceRequestBytes } returns deviceRequestBytes
        }

        val updatedLog = builder.withRequest(initialLog, deviceRequest)

        assertEquals(deviceRequestBytes, updatedLog.rawRequest)
        assertEquals(TransactionLog.Status.Incomplete, updatedLog.status)
        assertTrue(updatedLog.timestamp >= initialLog.timestamp)
    }

    @Test
    fun `withRequest with DigitalCredentialsQuery updates log correctly`() {
        val initialLog = builder.createEmptyPresentationLog()
        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.mdoc(
                        id = QueryId("test-id"),
                        msoMdocMeta = DCQLMetaMsoMdocExtensions(doctypeValue = MsoMdocDocType("test-docType")),
                        claims = listOf(
                            ClaimsQuery.mdoc(
                                path = ClaimPath(
                                    listOf(
                                        ClaimPathElement.Claim("nameSpace"),
                                        ClaimPathElement.Claim("elementIdentifier")
                                    )
                                )

                            )
                        )
                    )
                )
            ),
        )

        val resolvedRequestObject = ResolvedRequestObject(
            client = mockk(),
            responseMode = ResponseMode.DirectPostJwt(responseURI = mockk()),
            query = dcql,
            nonce = "test-nonce",
            responseEncryptionSpecification = null,
            vpFormatsSupported = null,
            state = "test-state",
            transactionData = null,
            verifierInfo = null,
        )

        val openId4VpRequest = mockk<OpenId4VpRequest> {
            every { this@mockk.resolvedRequestObject } returns resolvedRequestObject
        }

        val updatedLog = builder.withRequest(initialLog, openId4VpRequest)

        assertNotNull(updatedLog.rawRequest)
        assertEquals(TransactionLog.Status.Incomplete, updatedLog.status)
        assertTrue(updatedLog.timestamp >= initialLog.timestamp)
    }

    @Test
    fun `withRequest with unsupported request type sets error status`() {
        val initialLog = builder.createEmptyPresentationLog()
        val unsupportedRequest = mockk<Request>()

        val updatedLog = builder.withRequest(initialLog, unsupportedRequest)

        assertEquals(TransactionLog.Status.Error, updatedLog.status)
        assertTrue(updatedLog.timestamp >= initialLog.timestamp)
    }

    @Test
    fun `withRequest with non-presentation log returns unchanged log`() {
        val nonPresentationLog = TransactionLog(
            timestamp = Instant.now().toEpochMilli(),
            status = TransactionLog.Status.Incomplete,
            type = TransactionLog.Type.Issuance,
            relyingParty = null,
            rawRequest = null,
            rawResponse = null,
            dataFormat = null,
            sessionTranscript = null,
            metadata = null
        )

        val deviceRequest = mockk<DeviceRequest>()

        val result = builder.withRequest(nonPresentationLog, deviceRequest)

        assertEquals(nonPresentationLog, result)
    }

    @Test
    fun `withRelyingParty with valid processed request updates log correctly`() {
        val initialLog = builder.createEmptyPresentationLog()

        val x509Certificate = mockk<X509Certificate> {
            every { encoded } returns "cert-data".toByteArray()
        }

        val readerAuth = mockk<ReaderAuth> {
            every { readerCommonName } returns "Test Relying Party"
            every { readerCertificateChain } returns listOf(x509Certificate)
            every { isVerified } returns true
            every { this@mockk.readerAuth } returns "reader-auth-data".toByteArray()
        }

        val requestedDocument = mockk<RequestedDocument> {
            every { this@mockk.readerAuth } returns readerAuth
        }

        val processedRequest = mockk<RequestProcessor.ProcessedRequest> {
            every { getOrNull() } returns mockk<RequestProcessor.ProcessedRequest.Success>() {
                every { requestedDocuments } returns RequestedDocuments(listOf(requestedDocument))
            }
        }

        val updatedLog = builder.withRelyingParty(initialLog, processedRequest)

        assertNotNull(updatedLog.relyingParty)
        assertEquals("Test Relying Party", updatedLog.relyingParty?.name)
        assertEquals(true, updatedLog.relyingParty?.isVerified)
        assertEquals(1, updatedLog.relyingParty?.certificateChain?.size)
        assertNotNull(updatedLog.relyingParty?.readerAuth)
        assertTrue(updatedLog.timestamp >= initialLog.timestamp)
    }

    @Test
    fun `withRelyingParty with unverified relying party sets correct values`() {
        val initialLog = builder.createEmptyPresentationLog()

        val x509Certificate = mockk<X509Certificate> {
            every { encoded } returns "cert-data".toByteArray()
        }

        val readerAuth = mockk<ReaderAuth> {
            every { readerCommonName } returns "Test Relying Party"
            every { readerCertificateChain } returns listOf(x509Certificate)
            every { isVerified } returns false
            every { this@mockk.readerAuth } returns "reader-auth-data".toByteArray()
        }

        val requestedDocument = mockk<RequestedDocument> {
            every { this@mockk.readerAuth } returns readerAuth
        }
        val requestedDocuments = RequestedDocuments(listOf(requestedDocument))

        val processedRequestSuccess = mockk<RequestProcessor.ProcessedRequest.Success> {
            every { this@mockk.requestedDocuments } returns requestedDocuments
        }
        val processedRequest = mockk<RequestProcessor.ProcessedRequest> {
            every { getOrNull() } returns processedRequestSuccess
        }

        val updatedLog = builder.withRelyingParty(initialLog, processedRequest)

        assertNotNull(updatedLog.relyingParty)
        assertEquals(false, updatedLog.relyingParty?.isVerified)
    }

    @Test
    fun `withRelyingParty with null readerAuth returns log with default relyingParty`() {
        val initialLog = builder.createEmptyPresentationLog()

        val requestedDocument = mockk<RequestedDocument> {
            every { readerAuth } returns null
        }
        val requestedDocuments = RequestedDocuments(listOf(requestedDocument))

        val processedRequestSuccess = mockk<RequestProcessor.ProcessedRequest.Success> {
            every { this@mockk.requestedDocuments } returns requestedDocuments
        }
        val processedRequest = mockk<RequestProcessor.ProcessedRequest> {
            every { getOrNull() } returns processedRequestSuccess
        }

        val updatedLog = builder.withRelyingParty(initialLog, processedRequest)

        assertEquals("Unidentified Relying Party", updatedLog.relyingParty?.name)
    }

    @Test
    fun `withRelyingParty with non-presentation log returns unchanged log`() {
        val nonPresentationLog = TransactionLog(
            timestamp = Instant.now().toEpochMilli(),
            status = TransactionLog.Status.Incomplete,
            type = TransactionLog.Type.Issuance,
            relyingParty = null,
            rawRequest = null,
            rawResponse = null,
            dataFormat = null,
            sessionTranscript = null,
            metadata = null
        )

        val processedRequest = mockk<RequestProcessor.ProcessedRequest>()

        val result = builder.withRelyingParty(nonPresentationLog, processedRequest)

        assertEquals(nonPresentationLog, result)
    }

    @Test
    fun `withRelyingParty with null processedRequest result returns log with updated timestamp`() {
        val initialLog = builder.createEmptyPresentationLog()

        val processedRequest = mockk<RequestProcessor.ProcessedRequest> {
            every { getOrNull() } returns null
        }

        val updatedLog = builder.withRelyingParty(initialLog, processedRequest)

        assertNull(updatedLog.relyingParty)
        assertTrue(updatedLog.timestamp >= initialLog.timestamp)
    }

    @Test
    fun `withResponse with DeviceResponse updates log correctly`() {
        val initialLog = builder.createEmptyPresentationLog()
        val responseBytes = "test response".toByteArray()
        val sessionTranscriptBytes = "session transcript".toByteArray()
        val documentIds = listOf("id1", "id2")

        val deviceResponse = mockk<DeviceResponse> {
            every { deviceResponseBytes } returns responseBytes
            every { this@mockk.documentIds } returns documentIds
            every { this@mockk.sessionTranscriptBytes } returns sessionTranscriptBytes
        }

        val updatedLog = builder.withResponse(initialLog, deviceResponse)

        assertEquals(responseBytes, updatedLog.rawResponse)
        assertEquals(TransactionLog.DataFormat.Cbor, updatedLog.dataFormat)
        assertEquals(listOf("Metadata 1", "Metadata 2"), updatedLog.metadata)
        assertEquals(sessionTranscriptBytes, updatedLog.sessionTranscript)
        assertEquals(TransactionLog.Status.Completed, updatedLog.status)
        assertTrue(updatedLog.timestamp >= initialLog.timestamp)
    }

    @Test
    fun `withResponse with DeviceResponse and error sets error status`() {
        val initialLog = builder.createEmptyPresentationLog()
        val responseBytes = "test response".toByteArray()
        val documentIds = listOf("id1", "id2")

        val deviceResponse = mockk<DeviceResponse> {
            every { deviceResponseBytes } returns responseBytes
            every { this@mockk.documentIds } returns documentIds
            every { sessionTranscriptBytes } returns "session".toByteArray()
        }

        val error = RuntimeException("Test error")

        val updatedLog = builder.withResponse(initialLog, deviceResponse, error)

        assertEquals(TransactionLog.Status.Error, updatedLog.status)
    }

    @Test
    fun `withResponse with OpenId4VpResponse updates log correctly`() {
        val initialLog = builder.createEmptyPresentationLog()
        val documentIds = listOf("id1")

        val verifiablePresentation = VerifiablePresentation.Generic(
            value = ""
        )
        val queryId = QueryId("query1")

        val vpTokenMock = Consensus.PositiveConsensus(
            verifiablePresentations = VerifiablePresentations(
                mapOf(
                    queryId to listOf(
                        verifiablePresentation
                    )
                )
            ),
        )

        val openId4VpGenericResponse = mockk<OpenId4VpResponse> {
            every { vpToken } returns vpTokenMock
            every { this@mockk.respondedDocuments } returns mapOf(
                queryId to documentIds.map { id ->
                    OpenId4VpResponse.RespondedDocument(
                        documentId = id,
                        format = FORMAT_MSO_MDOC
                    )
                }
            )
        }

        val updatedLog = builder.withResponse(initialLog, openId4VpGenericResponse)

        assertNotNull(updatedLog.rawResponse)
        assertEquals(TransactionLog.DataFormat.Json, updatedLog.dataFormat)
        assertEquals(listOf("Metadata 1", "Metadata 2"), updatedLog.metadata)
        assertEquals(TransactionLog.Status.Completed, updatedLog.status)
    }

    @Test
    fun `withResponse with non-presentation log returns unchanged log`() {
        val nonPresentationLog = TransactionLog(
            timestamp = Instant.now().toEpochMilli(),
            status = TransactionLog.Status.Incomplete,
            type = TransactionLog.Type.Issuance,
            relyingParty = null,
            rawRequest = null,
            rawResponse = null,
            dataFormat = null,
            sessionTranscript = null,
            metadata = null
        )

        val deviceResponse = mockk<DeviceResponse>()

        val result = builder.withResponse(nonPresentationLog, deviceResponse)

        assertEquals(nonPresentationLog, result)
    }

    @Test
    fun `withError updates log with error status`() {
        val initialLog = builder.createEmptyPresentationLog()

        val updatedLog = builder.withError(initialLog)

        assertEquals(TransactionLog.Status.Error, updatedLog.status)
        assertTrue(updatedLog.timestamp >= initialLog.timestamp)
    }

    @Test
    fun `withError with non-presentation log also updates error status`() {
        val nonPresentationLog = TransactionLog(
            timestamp = Instant.now().toEpochMilli(),
            status = TransactionLog.Status.Incomplete,
            type = TransactionLog.Type.Issuance,
            relyingParty = null,
            rawRequest = null,
            rawResponse = null,
            dataFormat = null,
            sessionTranscript = null,
            metadata = null
        )

        val updatedLog = builder.withError(nonPresentationLog)

        assertEquals(TransactionLog.Status.Error, updatedLog.status)
        assertTrue(updatedLog.timestamp >= nonPresentationLog.timestamp)
    }
}

