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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql

import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.openid4vp.Client
import eu.europa.ec.eudi.openid4vp.HashAlgorithm
import eu.europa.ec.eudi.openid4vp.ResolutionError
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.TransactionData
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.openid4vp.dcql.ClaimsQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.Credentials
import eu.europa.ec.eudi.openid4vp.dcql.DCQL
import eu.europa.ec.eudi.openid4vp.dcql.DCQLMetaSdJwtVcExtensions
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderTrust
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequestException
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ReaderTrustResult
import eu.europa.ec.eudi.wallet.transfer.openId4vp.TransactionDataType
import eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData.QesApprovalRequest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Test
import org.multipaz.claim.Claim
import org.multipaz.claim.JsonClaim
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.sdjwt.credential.SdJwtVcCredential
import java.net.URI
import java.util.Base64
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import eu.europa.ec.eudi.openid4vp.TransactionDataType as RequestedTransactionDataType

/**
 * Tests for the transaction data the processor attaches to the matches it produces, and for the
 * requests it rejects because their transaction data cannot be presented.
 */
class DcqlRequestProcessorTransactionDataTest {

    @Test
    fun `transaction data is attached to the matches of the Credential that carries its hash`(): Unit =
        runBlocking {
            val processor = buildProcessor(queryIds = listOf("query_0"), withCnf = true)

            val processed = processor.process(
                request(dcql("query_0"), qesApproval("query_0"))
            )

            val match = assertIs<ProcessedDcqlRequest>(processed).flatMatches().single()
            val transactionData = match.transactionData.single()
            val payload = assertIs<QesApprovalRequest>(transactionData.payload)
            assertEquals("GX0112348", payload.credentialId)
            assertEquals(QesApprovalRequest.TYPE, transactionData.type.identifier)
        }

    @Test
    fun `the hash is carried by only one of the referenced Credentials`(): Unit = runBlocking {
        val processor = buildProcessor(queryIds = listOf("query_0", "query_1"), withCnf = true)

        val processed = processor.process(
            request(dcql("query_0", "query_1"), qesApproval("query_0", "query_1"))
        )

        val matches = assertIs<ProcessedDcqlRequest>(processed).flatMatches()
        assertEquals(2, matches.size)
        assertEquals(1, matches.count { it.transactionData.isNotEmpty() })
    }

    @Test
    fun `a transaction data type that is not accepted is rejected`(): Unit = runBlocking {
        val processor = buildProcessor(queryIds = listOf("query_0"), withCnf = true, accepts = emptyList())

        val processed = processor.process(
            request(dcql("query_0"), qesApproval("query_0"))
        )

        assertInvalidTransactionData(processed, "is not accepted")
    }

    @Test
    fun `a transaction data that does not conform to its type is rejected`(): Unit = runBlocking {
        val processor = buildProcessor(queryIds = listOf("query_0"), withCnf = true)

        val processed = processor.process(
            request(dcql("query_0"), qesApproval("query_0", numSignatures = 0))
        )

        assertInvalidTransactionData(processed, "numSignatures")
    }

    @Test
    fun `a transaction data whose Credentials the wallet does not have is rejected`(): Unit =
        runBlocking {
            val processor = buildProcessor(queryIds = listOf("query_0"), withCnf = false)

            val processed = processor.process(
                request(dcql("query_0"), qesApproval("query_0"))
            )

            assertInvalidTransactionData(processed, "references no Credential")
        }

    /**
     * OpenID4VP Appendix B.3.3: "The transaction data mechanism requires the use of an SD-JWT VC
     * with Cryptographic Holder Binding. Wallets MUST reject requests with transaction data types
     * that have the require_cryptographic_holder_binding parameter set to false."
     */
    @Test
    fun `transaction data on a Credential that does not require holder binding is rejected`(): Unit =
        runBlocking {
            val processor = buildProcessor(queryIds = listOf("query_0"), withCnf = true)

            val processed = processor.process(
                request(dcqlWithoutHolderBinding("query_0"), qesApproval("query_0"))
            )

            assertInvalidTransactionData(processed, "cryptographic holder binding")
        }

    @Test
    fun `a request without transaction data produces matches that carry none`(): Unit = runBlocking {
        val processor = buildProcessor(queryIds = listOf("query_0"), withCnf = true)

        val processed = processor.process(request(dcql("query_0"), transactionData = null))

        val match = assertIs<ProcessedDcqlRequest>(processed).flatMatches().single()
        assertTrue(match.transactionData.isEmpty())
    }

    private fun assertInvalidTransactionData(
        processed: RequestProcessor.ProcessedRequest,
        because: String,
    ) {
        val failure = assertIs<RequestProcessor.ProcessedRequest.Failure>(processed)
        val exception = assertIs<OpenId4VpRequestException>(failure.error)
        val error = assertIs<ResolutionError.InvalidTransactionData>(exception.error)
        assertContains(error.cause.message.orEmpty(), because)
    }

    private fun qesApproval(
        vararg credentialIds: String,
        numSignatures: Int = 1,
    ): TransactionData = TransactionData.sdJwtVc(
        type = RequestedTransactionDataType(QesApprovalRequest.TYPE),
        credentialIds = credentialIds.map { QueryId(it) },
        hashAlgorithms = listOf(HashAlgorithm.SHA_256),
    ) {
        put("credentialID", "GX0112348")
        put("signatureQualifier", "eu_eidas_qes")
        put("numSignatures", numSignatures)
        putJsonArray("documentDigests") {
            addJsonObject {
                put("label", "Service Agreement 2026-09")
                put("hash", "sTOgwOm+474gFj0q0x1iSNspKqbcse4IeiqlDg/HWuI=")
            }
        }
        put("hashAlgorithmOID", "2.16.840.1.101.3.4.2.1")
    }

    private fun dcql(vararg queryIds: String): DCQL = DCQL(
        credentials = Credentials(
            queryIds.map { id ->
                CredentialQuery.sdJwtVc(
                    id = QueryId(id),
                    sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(VCT)),
                    claims = listOf(
                        ClaimsQuery.sdJwtVc(
                            path = ClaimPath(listOf(ClaimPathElement.Claim("family_name"))),
                        ),
                    ),
                )
            },
        ),
        credentialSets = null,
    )

    /**
     * The same query as [dcql], with `require_cryptographic_holder_binding` explicitly false.
     */
    private fun dcqlWithoutHolderBinding(queryId: String): DCQL = DCQL(
        credentials = Credentials(
            listOf(
                CredentialQuery.sdJwtVc(
                    id = QueryId(queryId),
                    sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(VCT)),
                    requireCryptographicHolderBinding = false,
                    claims = listOf(
                        ClaimsQuery.sdJwtVc(
                            path = ClaimPath(listOf(ClaimPathElement.Claim("family_name"))),
                        ),
                    ),
                ),
            ),
        ),
        credentialSets = null,
    )

    private fun buildProcessor(
        queryIds: List<String>,
        withCnf: Boolean,
        accepts: List<TransactionDataType> = listOf(TransactionDataType.QES_APPROVAL),
    ): DcqlRequestProcessor {
        val credential = mockk<SecureAreaBoundCredential>(
            moreInterfaces = arrayOf(SdJwtVcCredential::class),
        )
        coEvery { credential.getClaims(documentTypeRepository = null) } returns
                listOf(jsonClaim("family_name", JsonPrimitive("Doe")))
        every { (credential as SdJwtVcCredential).issuerProvidedData } returns
                ByteString(sdJwtCompactSerialization(withCnf).toByteArray())

        val issuedDoc = mockk<IssuedDocument> {
            every { format } returns SdJwtVcFormat(VCT) as DocumentFormat
            coEvery { findCredential(now = any()) } returns credential
        }
        val documentManager = mockk<DocumentManager> {
            every { getDocuments(predicate = any()) } returns listOf(issuedDoc)
            every { getDocuments(predicate = null) } returns listOf(issuedDoc)
        }
        val trust = mockk<OpenId4VpReaderTrust> {
            every { result } returns ReaderTrustResult.Pending
            every { readerTrustStore } returns null
            every { readerTrustStore = any() } returns Unit
        }
        return DcqlRequestProcessor(
            documentManager = documentManager,
            openid4VpX509CertificateTrust = trust,
            readerAuthPolicy = ReaderAuthPolicy.DoNotEnforce,
            transactionDataTypes = accepts
        )
    }

    private fun request(dcql: DCQL, vararg transactionData: TransactionData): OpenId4VpRequest =
        request(dcql, transactionData.toList())

    private fun request(dcql: DCQL, transactionData: List<TransactionData>?): OpenId4VpRequest {
        val resolved = mockk<ResolvedRequestObject> {
            every { query } returns dcql
            every { this@mockk.transactionData } returns transactionData
            every { client } returns Client.RedirectUri(URI.create("https://verifier.example"))
        }
        return mockk { every { resolvedRequestObject } returns resolved }
    }

    private fun jsonClaim(claimName: String, value: JsonElement): JsonClaim = JsonClaim(
        displayName = claimName,
        attribute = null,
        vct = VCT,
        claimPath = JsonArray(listOf(JsonPrimitive(claimName))),
        value = value,
    )

    private fun sdJwtCompactSerialization(includeCnf: Boolean): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"ES256","typ":"dc+sd-jwt"}""".toByteArray())
        val cnf = if (includeCnf) {
            ""","cnf":{"jwk":{"kty":"EC","crv":"P-256","x":"$X","y":"$Y"}}"""
        } else {
            ""
        }
        val payload = encoder.encodeToString("""{"vct":"$VCT"$cnf}""".toByteArray())
        return "$header.$payload.signature~"
    }

    private fun ProcessedDcqlRequest.flatMatches() =
        presentmentData.credentialSets
            .flatMap { it.options }
            .flatMap { it.members }
            .flatMap { it.matches }

    private companion object {
        const val VCT = "urn:eudi:pid:1"
        const val X = "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU"
        const val Y = "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0"
    }
}
