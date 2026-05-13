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

package eu.europa.ec.eudi.wallet.internal

import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import org.junit.Test
import org.multipaz.credential.Credential
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.EcPublicKey
import org.multipaz.document.Document
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.request.JsonRequestedClaim
import org.multipaz.sdjwt.SdJwt
import org.multipaz.sdjwt.credential.SdJwtVcCredential
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for [verifiablePresentationForSdJwtVc], the OpenID4VP SD-JWT-VC
 * presentation builder.
 *
 *  - **type guard**: a wallet credential that doesn't implement [SdJwtVcCredential]
 *    must short-circuit with [IllegalStateException].
 *  - **no-cnf happy path**: a real SD-JWT (built inline via [SdJwt.create]) without
 *    `cnf` flows through to [SdJwt.compactSerialization] — no KB-JWT is appended.
 *  - **trailing wildcard disclosure**: a request path ending in `null` discloses the
 *    parent array claim.
 *
 * Mocking strategy: the test stubs the wallet credential chain
 * ([match.credential][CredentialPresentmentSetOptionMemberMatch.credential] →
 * [DocumentManager.getDocumentById] → [IssuedDocument.consumingCredential]) and routes
 * the `consumingCredential` block onto a synthetic credential.
 */
class SdJwtVcPresentationTest {

    /**
     * The function casts `this` (the [SecureAreaBoundCredential] handed in by
     * `consumingCredential`) to [SdJwtVcCredential]; if the cast fails the wallet must
     * fail loudly with [IllegalStateException]. Production guarantees this invariant
     * (only SD-JWT VC formats land here) but the guard exists for defense in depth.
     */
    @Test
    fun `type guard rejects credential that is not an SdJwtVcCredential`(): Unit = runBlocking {
        val credentialId = "cred-id-non-sdjwt"
        // Mock the [Credential] carried by the match — only `document.identifier` is
        // read, by requireIssuedDocument(documentManager).
        val matchCredential = mockCredential(documentId = credentialId)
        // A SecureAreaBoundCredential that is NOT also an SdJwtVcCredential — triggers the
        // `as? SdJwtVcCredential` ?: error branch in production.
        val nonSdJwtCredential = mockk<SecureAreaBoundCredential> {
            every { identifier } returns credentialId
        }
        val issuedDocument = mockIssuedDocumentForwardingTo(nonSdJwtCredential)
        val documentManager = mockk<DocumentManager> {
            every { getDocumentById(credentialId) } returns issuedDocument
        }
        val match = mockk<CredentialPresentmentSetOptionMemberMatch> {
            every { credential } returns matchCredential
            every { claims } returns emptyMap()
        }

        assertFailsWith<IllegalStateException> {
            verifiablePresentationForSdJwtVc(
                resolvedRequestObject = mockk<ResolvedRequestObject>(relaxed = true),
                match = match,
                documentManager = documentManager,
                keyUnlockData = null,
                signatureAlgorithm = Algorithm.ESP256,
            )
        }
    }

    /**
     * Happy-path smoke test for a keyless SD-JWT (no `cnf` claim). Since the
     * issuer-signed JWT carries no holder-binding key, [SdJwt.kbKey] is `null` and the
     * function must short-circuit to [SdJwt.compactSerialization] without producing or
     * signing a KB-JWT. The wire representation is the standard SD-JWT compact form —
     * three JWT segments and the disclosures terminated by `~`.
     *
     * Structural assertions (`endsWith("~")`, non-empty) rather than byte-equality,
     * because the issuer-signed JWT carries its own randomness (salts, timestamps) and
     * is not deterministic.
     */
    @Test
    fun `returns a serialized SD-JWT without key binding when kbKey is null`(): Unit = runBlocking {
        // Build a real SD-JWT inline: ephemeral issuer key, no holder binding (kbKey = null).
        // We mimic a tiny PID-like credential ({"given_name": "Alice"}); [SdJwt.create]
        // computes the disclosure hashes and produces the compact serialization, which we
        // feed into a synthetic [SdJwtVcCredential] below.
        val issuerKey = AsymmetricKey.ephemeral(Algorithm.ESP256)
        val realSdJwt = SdJwt.create(
            issuerKey = issuerKey,
            kbKey = null,
            claims = """{"given_name":"Alice"}""",
            nonSdClaims = """{"iss":"https://issuer.example","vct":"urn:eudi:pid:1"}""",
        )
        val issuerBytes = ByteString(realSdJwt.compactSerialization.encodeToByteArray())

        val credentialId = "cred-id-sdjwt"
        val matchCredential = mockCredential(documentId = credentialId)
        // Synthetic credential implementing both SecureAreaBoundCredential and
        // SdJwtVcCredential. Only `issuerProvidedData` is read for the kbKey == null
        // path; the rest is irrelevant.
        val sdJwtCredential = mockk<SecureAreaBoundCredential>(
            moreInterfaces = arrayOf(SdJwtVcCredential::class),
        ) {
            every { identifier } returns credentialId
            every { (this@mockk as SdJwtVcCredential).issuerProvidedData } returns issuerBytes
        }
        val issuedDocument = mockIssuedDocumentForwardingTo(sdJwtCredential)
        val documentManager = mockk<DocumentManager> {
            every { getDocumentById(credentialId) } returns issuedDocument
        }
        val match = mockk<CredentialPresentmentSetOptionMemberMatch> {
            every { credential } returns matchCredential
            // No claim paths to disclose → minimal disclosure path
            every { claims } returns emptyMap()
        }

        val result = verifiablePresentationForSdJwtVc(
            resolvedRequestObject = mockk<ResolvedRequestObject>(relaxed = true),
            match = match,
            documentManager = documentManager,
            keyUnlockData = null,
            signatureAlgorithm = Algorithm.ESP256,
        )

        // The compact form is non-empty and terminates with `~` (the SD-JWT framing tilde).
        // No `~` followed by a non-empty KB-JWT segment, because kbKey was null.
        val serialized = result.value
        assertTrue(serialized.isNotEmpty(), "Serialized SD-JWT must be non-empty")
        assertTrue(serialized.endsWith("~"), "Keyless SD-JWT must terminate with the framing `~`")
    }

    /**
     * Regression guard: a verifier asking `["nationalities", null]` (trailing
     * `AllArrayElements` wildcard) must actually receive the `nationalities` array in
     * the resulting SD-JWT.
     *
     * Negative control via `given_name`: it lives in the same issuer-signed credential
     * but the verifier did NOT request it, so it must NOT be reconstructible from the
     * produced VP.
     */
    @Test
    fun `trailing AllArrayElements wildcard discloses parent array claim`(): Unit = runBlocking {
        val issuerKey = AsymmetricKey.ephemeral(Algorithm.ESP256)
        // Real SD-JWT carrying two selectively-disclosable claims:
        //  - given_name (the "control" — verifier doesn't ask for it)
        //  - nationalities (an array — the verifier asks via wildcard)
        val realSdJwt = SdJwt.create(
            issuerKey = issuerKey,
            kbKey = null,
            claims = """{"given_name":"Alice","nationalities":["DE","FR"]}""",
            nonSdClaims = """{"iss":"https://issuer.example","vct":"urn:eudi:pid:1"}""",
        )
        val issuerBytes = ByteString(realSdJwt.compactSerialization.encodeToByteArray())

        val credentialId = "cred-id-wildcard"
        val matchCredential = mockCredential(documentId = credentialId)
        val sdJwtCredential = mockk<SecureAreaBoundCredential>(
            moreInterfaces = arrayOf(SdJwtVcCredential::class),
        ) {
            every { identifier } returns credentialId
            every { (this@mockk as SdJwtVcCredential).issuerProvidedData } returns issuerBytes
        }
        val issuedDocument = mockIssuedDocumentForwardingTo(sdJwtCredential)
        val documentManager = mockk<DocumentManager> {
            every { getDocumentById(credentialId) } returns issuedDocument
        }

        // The verifier's request: ["nationalities", null] — "give me every entry of the
        // nationalities array".
        val nationalitiesWildcard = JsonRequestedClaim(
            vctValues = listOf("urn:eudi:pid:1"),
            claimPath = buildJsonArray {
                add("nationalities")
                add(JsonNull)
            },
        )
        val match = mockk<CredentialPresentmentSetOptionMemberMatch> {
            every { credential } returns matchCredential
            every { claims } returns mapOf(nationalitiesWildcard to mockk(relaxed = true))
        }

        val result = verifiablePresentationForSdJwtVc(
            resolvedRequestObject = mockk<ResolvedRequestObject>(relaxed = true),
            match = match,
            documentManager = documentManager,
            keyUnlockData = null,
            signatureAlgorithm = Algorithm.ESP256,
        )

        // Parse the produced SD-JWT and recreate the disclosed claim set by verifying
        // against the issuer key. [SdJwt.verify] reconstructs every claim whose
        // disclosure is present in the compact form, so this is the cleanest way to
        // observe what the verifier would see on the receiving side.
        val producedSdJwt = SdJwt.fromCompactSerialization(result.value)
        val disclosed = producedSdJwt.verify(issuerKey.publicKey as EcPublicKey)

        assertTrue(
            disclosed.containsKey("nationalities"),
            "Wildcard request must result in the nationalities array being disclosed; " +
                "decoded payload was $disclosed",
        )
        assertTrue(
            !disclosed.containsKey("given_name"),
            "Negative control: given_name was NOT requested, must not be disclosed; " +
                "decoded payload was $disclosed",
        )
        // And to be exact: the disclosed value is the full array, not a single element.
        val nationalitiesValue = disclosed["nationalities"]
        assertTrue(
            nationalitiesValue is JsonArray && nationalitiesValue.size == 2,
            "Expected the entire nationalities array (2 entries) to be disclosed; was $nationalitiesValue",
        )
    }

    /**
     * Mock a [Credential] whose [Credential.document] exposes the given identifier.
     * That is the only field read by [requireIssuedDocument].
     */
    private fun mockCredential(documentId: String): Credential {
        val mockedDocument = mockk<Document> { every { identifier } returns documentId }
        return mockk<Credential> { every { document } returns mockedDocument }
    }

    /**
     * Mock an [IssuedDocument] whose [IssuedDocument.consumingCredential] routes the
     * test's `block` directly onto [target], wrapping any thrown exception in a
     * [Result.failure] — same surface contract as the production method.
     */
    private fun mockIssuedDocumentForwardingTo(target: SecureAreaBoundCredential): IssuedDocument {
        val doc = mockk<IssuedDocument>()
        coEvery { doc.consumingCredential<Any>(any()) } coAnswers {
            val block = firstArg<suspend SecureAreaBoundCredential.() -> Any>()
            runCatching { block.invoke(target) }
        }
        return doc
    }
}