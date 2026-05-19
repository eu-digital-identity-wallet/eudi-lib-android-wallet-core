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

import eu.europa.ec.eudi.openid4vp.Client
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
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
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ReaderTrustResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.multipaz.claim.Claim
import org.multipaz.claim.JsonClaim
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.sdjwt.credential.SdJwtVcCredential
import java.net.URI
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for the cryptographic holder binding filter applied in
 * [DcqlRequestProcessor.findMatchesForQuery].
 *
 * Per OpenID4VP §B.3 and ARF §6.6.3.8, when the verifier sets
 * `require_cryptographic_holder_binding = true` (the default), SD-JWT credentials
 * without a `cnf` claim cannot satisfy the query — they have no key with which to
 * produce a KB-JWT. The processor filters such credentials before they reach the
 * consent UI. mdoc credentials are not filtered: in this codebase all mdocs carry
 * a device key.
 */
class DcqlRequestProcessorHolderBindingTest {

    /**
     * Default flag is `true`. The wallet's SD-JWT credential has no `cnf` claim —
     * it cannot produce a KB-JWT — so the processor must filter it out.
     */
    @Test
    fun `sdjwt without cnf is filtered when verifier requires holder binding`(): Unit = runBlocking {
        val vct = "urn:eudi:pid:1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.sdJwtVc(
                        id = QueryId("query_0"),
                        sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(vct)),
                        // requireCryptographicHolderBinding left null → defaults to true.
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

        val processor = buildProcessor(
            vct = vct,
            credentialClaims = listOf(jsonClaim(vct, "family_name", JsonPrimitive("Doe"))),
            issuerSignedJwt = buildSdJwtCompactSerialization(includeCnf = false),
        )

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        assertEquals(
            expected = 0,
            actual = success.flatMatches().size,
            message = "An SD-JWT credential without `cnf` must not satisfy a query that " +
                "requires cryptographic holder binding.",
        )
    }

    /**
     * Same default flag (`true`), but this time the credential's issuer-signed JWT
     * carries a `cnf` claim — so the wallet can produce a KB-JWT at presentation
     * time. The processor must surface the credential as a match.
     */
    @Test
    fun `sdjwt with cnf passes when verifier requires holder binding`(): Unit = runBlocking {
        val vct = "urn:eudi:pid:1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.sdJwtVc(
                        id = QueryId("query_0"),
                        sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(vct)),
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

        val processor = buildProcessor(
            vct = vct,
            credentialClaims = listOf(jsonClaim(vct, "family_name", JsonPrimitive("Doe"))),
            issuerSignedJwt = buildSdJwtCompactSerialization(includeCnf = true),
        )

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        assertEquals(
            expected = 1,
            actual = success.flatMatches().size,
            message = "An SD-JWT credential with `cnf` must satisfy a query that requires " +
                "cryptographic holder binding.",
        )
    }

    /**
     * When the verifier explicitly accepts bare presentations
     * (`requireCryptographicHolderBinding = false`), the filter must not apply: an
     * SD-JWT credential without `cnf` becomes a valid match.
     */
    @Test
    fun `sdjwt without cnf passes when verifier does not require holder binding`(): Unit = runBlocking {
        val vct = "urn:eudi:pid:1"

        val dcql = DCQL(
            credentials = Credentials(
                listOf(
                    CredentialQuery.sdJwtVc(
                        id = QueryId("query_0"),
                        sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = listOf(vct)),
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

        val processor = buildProcessor(
            vct = vct,
            credentialClaims = listOf(jsonClaim(vct, "family_name", JsonPrimitive("Doe"))),
            issuerSignedJwt = buildSdJwtCompactSerialization(includeCnf = false),
        )

        val processed = processor.process(buildOpenId4VpRequest(dcql))

        val success = assertIs<ProcessedDcqlRequest>(processed)
        assertEquals(
            expected = 1,
            actual = success.flatMatches().size,
            message = "When the verifier accepts bare presentations, an SD-JWT credential " +
                "without `cnf` must still surface as a match.",
        )
    }
    
    /**
     * Builds a parseable SD-JWT compact serialization. `kbKey` is derived from the
     * issuer-signed JWT's `cnf` claim via pure JSON parsing — the signature is not
     * verified, so an arbitrary placeholder suffices for these tests.
     *
     * When [includeCnf] is true the body carries a `cnf.jwk` with valid P-256 JWK
     * structure (32-byte zero coordinates — multipaz parses them without checking
     * they lie on the curve).
     */
    private fun buildSdJwtCompactSerialization(includeCnf: Boolean): String {
        val header = """{"alg":"ES256","typ":"vc+sd-jwt"}"""
        val zeroCoord = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(32))
        val body = if (includeCnf) {
            """{"iss":"https://issuer.example","cnf":{"jwk":{"kty":"EC","crv":"P-256","x":"$zeroCoord","y":"$zeroCoord"}}}"""
        } else {
            """{"iss":"https://issuer.example"}"""
        }
        val headerB64 = base64Url(header)
        val bodyB64 = base64Url(body)
        return "$headerB64.$bodyB64.placeholder~"
    }

    private fun base64Url(s: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(s.toByteArray())

    /**
     * Build a [DcqlRequestProcessor] backed by a mocked credential that implements
     * both [SecureAreaBoundCredential] (the type [IssuedDocument.findCredential]
     * returns) and [SdJwtVcCredential] (the interface the processor reads
     * `issuerProvidedData` from). [issuerSignedJwt] becomes the credential's
     * SD-JWT compact serialization — feeding the `cnf` filter.
     */
    private fun buildProcessor(
        vct: String,
        credentialClaims: List<Claim>,
        issuerSignedJwt: String,
    ): DcqlRequestProcessor {
        val credential = mockk<SecureAreaBoundCredential>(
            moreInterfaces = arrayOf(SdJwtVcCredential::class),
        )
        coEvery { credential.getClaims(documentTypeRepository = null) } returns credentialClaims
        every { (credential as SdJwtVcCredential).issuerProvidedData } returns
            ByteString(issuerSignedJwt.toByteArray())

        val issuedDoc = mockk<IssuedDocument> {
            every { format } returns SdJwtVcFormat(vct) as DocumentFormat
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
        return DcqlRequestProcessor(documentManager, trust)
    }

    private fun buildOpenId4VpRequest(dcql: DCQL): OpenId4VpRequest {
        val resolved = mockk<ResolvedRequestObject> {
            every { query } returns dcql
            every { transactionData } returns null
            every { client } returns Client.RedirectUri(URI.create("https://verifier.example"))
        }
        return mockk { every { resolvedRequestObject } returns resolved }
    }

    private fun jsonClaim(
        vct: String,
        claimName: String,
        value: JsonElement,
    ): JsonClaim = JsonClaim(
        displayName = claimName,
        attribute = null,
        vct = vct,
        claimPath = JsonArray(listOf(JsonPrimitive(claimName))),
        value = value,
    )

    private fun ProcessedDcqlRequest.flatMatches() =
        presentmentData.credentialSets
            .flatMap { it.options }
            .flatMap { it.members }
            .flatMap { it.matches }
}
