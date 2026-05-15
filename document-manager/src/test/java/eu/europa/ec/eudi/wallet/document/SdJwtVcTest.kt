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

package eu.europa.ec.eudi.wallet.document

import android.util.Log
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.recreateClaimsAndDisclosuresPerClaim
import eu.europa.ec.eudi.sdjwt.NimbusSdJwtOps
import eu.europa.ec.eudi.sdjwt.vc.IssuerVerificationMethod
import eu.europa.ec.eudi.sdjwt.vc.SelectPath.Default.query
import eu.europa.ec.eudi.sdjwt.vc.TypeMetadataPolicy
import eu.europa.ec.eudi.sdjwt.vc.X509CertificateTrust
import eu.europa.ec.eudi.wallet.document.format.MutableSdJwtClaim
import eu.europa.ec.eudi.wallet.document.internal.parse
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.Storage
import org.multipaz.storage.ephemeral.EphemeralStorage
import java.security.cert.X509Certificate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SdJwtVcTest {

    @BeforeTest
    fun setup() {
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.v(any(), any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.d(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.i(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0
    }

    @AfterTest
    fun tearDown() {
        clearAllMocks()
        documentManager.getDocuments().forEach { documentManager.deleteDocumentById(it.id) }
    }

    @Test
    @Ignore("This test is failing because of invalid sd jwt vc, need to fix the test")
    fun `validate sd-jwt vc`() {
        runBlocking {
            val sdJwtVcString = getResourceAsText("sample_sd_jwt_vc.txt")
                .replace("\n", "")
                .replace("\r", "")

            val issuerVerificationMethod = IssuerVerificationMethod.usingX5cOrIssuerMetadata<List<X509Certificate>>(
                httpClient = mockk(relaxed = true), // Pass the mock instance directly, not a factory
                x509CertificateTrust = X509CertificateTrust { chain, claimSet ->
                    true
                }
            )

            val verifier = NimbusSdJwtOps.SdJwtVcVerifier(
                issuerVerificationMethod = issuerVerificationMethod,
                typeMetadataPolicy = TypeMetadataPolicy.NotUsed,
                checkStatus = null
            )

            val result = verifier.verify(sdJwtVcString)
            assertTrue(result.isSuccess)
        }
    }

    @Test
    @Ignore("This is test for parsing the sd-jwt vc. See eu.europa.ec.eudi.wallet.document.format.SdJwtVcDataTest for the actual test")
    fun `parse sd-jwt vc`() {

        val sdJwtVcString = getResourceAsText("sample_sd_jwt_vc.txt")
            .replace("\n", "")
            .replace("\r", "")

        val sdJwtVc = DefaultSdJwtOps.unverifiedIssuanceFrom(sdJwtVcString).getOrNull()
        assertTrue(sdJwtVc != null)

        val (claims, disclosuresPerClaim) = sdJwtVc.recreateClaimsAndDisclosuresPerClaim()
        val excluded = arrayOf("iss")

        val claimValueList = disclosuresPerClaim.map {
            println(
                "Path: ${it.key}, Value: ${
                    claims.query(it.key).getOrNull()?.toJsonElement()
                }, SelectivelyDisclosable: ${it.value.isNotEmpty()}"
            )
            Triple(it.key, claims.query(it.key).getOrNull()?.toJsonElement(), it.value.isNotEmpty())
        }.filterNot { it.first.head().toString() in excluded }

        val sdJwtVcClaims = mutableListOf<MutableSdJwtClaim>()
        for ((path, value, selectivelyDisclosable) in claimValueList) {
            var current = sdJwtVcClaims
            for (key in path.value) {
                val existingNode = current.find { it.pathElement == key }
                if (existingNode != null) {
                    current = existingNode.children
                } else {
                    val newClaim = MutableSdJwtClaim(
                        pathElement = key,
                        value = value?.parse(),
                        rawValue = value?.toString() ?: "",
                        selectivelyDisclosable = selectivelyDisclosable,
                        metadata = null
                    )
                    current.add(newClaim)
                    current = newClaim.children
                }
            }
        }

        printSdJwtVcClaims(sdJwtVcClaims)

        assertEquals(claims.filterNot { it.key in excluded }.size, sdJwtVcClaims.size)
        assertEquals(
            claimValueList.size,
            getSdJwtClaims(sdJwtVcClaims).size
        )
    }

    lateinit var documentManager: DocumentManagerImpl
    lateinit var storage: Storage
    lateinit var secureArea: SecureArea
    lateinit var secureAreaRepository: SecureAreaRepository

    @BeforeTest
    fun setUp() {
        storage = EphemeralStorage()
        secureArea = runBlocking { SoftwareSecureArea.create(storage) }
        secureAreaRepository = SecureAreaRepository.Builder()
            .add(secureArea)
            .build()
        documentManager = DocumentManagerImpl(
            identifier = "document_manager_1",
            storage = EphemeralStorage(),
            secureAreaRepository = secureAreaRepository,
            ktorHttpClientFactory = { mockk(relaxed = true) }
        )
    }

    private fun printSdJwtVcClaims(sdJwtVcClaims: List<MutableSdJwtClaim>, indent: String = "") {
        for (sdJwtVcClaim in sdJwtVcClaims) {
            println("$indent- PathElement: ${sdJwtVcClaim.pathElement}")
            if (sdJwtVcClaim.value != null) println("$indent  Value: ${sdJwtVcClaim.value}")
            if (sdJwtVcClaim.rawValue.isNotEmpty()) println("$indent  Raw Value: ${sdJwtVcClaim.rawValue}")
            println("$indent  Selectively Disclosable: ${sdJwtVcClaim.selectivelyDisclosable}")
            if (sdJwtVcClaim.metadata != null) println("$indent  Metadata: ${sdJwtVcClaim.metadata}")
            if (sdJwtVcClaim.children.isNotEmpty()) {
                println("$indent  Children:")
                printSdJwtVcClaims(sdJwtVcClaim.children, "$indent    ")
            }
        }
    }

    private fun getSdJwtClaims(claims: List<MutableSdJwtClaim>): List<MutableSdJwtClaim> {
        return claims.flatMap { claim ->
            listOf(claim) + getSdJwtClaims(claim.children)
        }
    }
}