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

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.openid4vp.Client
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.dcql.ClaimsQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.Credentials
import eu.europa.ec.eudi.openid4vp.dcql.DCQL
import eu.europa.ec.eudi.openid4vp.dcql.DCQLMetaMsoMdocExtensions
import eu.europa.ec.eudi.openid4vp.dcql.MsoMdocDocType
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import eu.europa.ec.eudi.wallet.registration.relyingparty.DefaultWrpRegistrationValidator
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderAuth
import eu.europa.ec.eudi.wallet.trust.ecIntermediateCertificate
import eu.europa.ec.eudi.wallet.trust.ecLeafSignedByIntermediateCertificate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.multipaz.crypto.X509CertChain
import org.multipaz.crypto.fromJavaX509Certificates
import org.multipaz.request.OpenID4VPRequesterIdentity
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests that [DcqlRequestProcessor.process] takes the requester identity, the trust verdict and the
 * registration access chain of a request only from the [OpenId4VpRequest.readerAuthentication] of
 * that request.
 */
class DcqlRequestProcessorReaderTrustTest {

    private val verifierChain = listOf(ecLeafSignedByIntermediateCertificate, ecIntermediateCertificate)
    private val verifierClient = Client.X509SanDns("verifier.example", verifierChain.first())

    private val readerTrustStore = mockk<ReaderTrustStore> {
        every { validateCertificationTrustPath(verifierChain) } returns true
    }

    @Test
    fun `a request is processed with the certificate chain and verdict of its verifier`(): Unit =
        runBlocking {
            val processed = process(verifierClient, OpenId4VpReaderAuth.X509(verifierChain, isTrusted = true))

            val identity = assertIs<OpenID4VPRequesterIdentity>(processed.requester.requesterIdentities.single())
            assertEquals(X509CertChain.fromJavaX509Certificates(verifierChain), identity.certChain)
            assertEquals("x509_san_dns:verifier.example", identity.clientId)
            assertEquals("EC Leaf Signed By RSA", assertNotNull(processed.trustMetadata).displayName)
        }

    @Test
    fun `a request with an untrusted certificate chain is not trusted`(): Unit = runBlocking {
        val processed = process(verifierClient, OpenId4VpReaderAuth.X509(verifierChain, isTrusted = false))

        assertEquals(1, processed.requester.requesterIdentities.size)
        assertNull(processed.trustMetadata, "Expected no trust metadata")
    }

    @Test
    fun `the registration access chain of a request is its certificate chain`(): Unit = runBlocking {
        val validator = registrationValidator()
        val processor = processor().apply { wrpRegistrationValidator = validator }

        process(verifierClient, OpenId4VpReaderAuth.X509(verifierChain, isTrusted = true), processor)

        coVerify { validator.validateAttestations(null, verifierChain, any()) }
    }

    private fun registrationValidator(): DefaultWrpRegistrationValidator = mockk {
        coEvery { validateAttestations(any(), any(), any()) } returns
            RegistrationCertificateResult.Failed(RegistrationFailureReason.CERTIFICATE_ABSENT)
    }

    private suspend fun process(
        requestClient: Client,
        readerAuthentication: OpenId4VpReaderAuth,
        processor: DcqlRequestProcessor = processor(),
    ): ProcessedDcqlRequest {
        val resolved = mockk<ResolvedRequestObject> {
            every { query } returns mdlQuery()
            every { transactionData } returns null
            every { client } returns requestClient
            every { verifierInfo } returns null
        }
        val processed = processor.process(OpenId4VpRequest(resolved, readerAuthentication))
        return assertIs<ProcessedDcqlRequest>(processed)
    }

    private fun processor(): DcqlRequestProcessor {
        val documentManager = mockk<DocumentManager> {
            every { getDocuments(predicate = any()) } returns emptyList()
            every { getDocuments(predicate = null) } returns emptyList()
        }
        val readerAuthPolicy = ReaderAuthPolicy.EnforceIfPresent(readerTrustStore)
        return DcqlRequestProcessor(documentManager, readerTrustStore, readerAuthPolicy)
    }

    private fun mdlQuery(): DCQL = DCQL(
        credentials = Credentials(
            listOf(
                CredentialQuery.mdoc(
                    id = QueryId("mdl"),
                    msoMdocMeta = DCQLMetaMsoMdocExtensions(MsoMdocDocType(MDL_DOCTYPE)),
                    claims = listOf(ClaimsQuery.mdoc(namespace = MDL_NAMESPACE, claimName = "family_name")),
                ),
            ),
        ),
        credentialSets = null,
    )

    private companion object {
        const val MDL_DOCTYPE = "org.iso.18013.5.1.mDL"
        const val MDL_NAMESPACE = "org.iso.18013.5.1"
    }
}
