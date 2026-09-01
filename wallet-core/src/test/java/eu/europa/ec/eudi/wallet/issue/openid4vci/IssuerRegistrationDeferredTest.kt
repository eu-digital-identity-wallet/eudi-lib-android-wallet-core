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
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.StoredDeferredContext
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.StoredIssuerRegistration
import eu.europa.ec.eudi.wallet.registration.QualifiedIdentifier
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import eu.europa.ec.eudi.wallet.registration.RegistrationIdentifier
import eu.europa.ec.eudi.wallet.registration.structuredIdentifier
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests how the issuer's registration is captured with a deferred document and read back to name the
 * interacting party (TS10 §3.5) once the credential resolves, when the certificate itself is gone.
 */
class IssuerRegistrationDeferredTest {

    private val certificate = RegistrationCertificate(
        identifiers = listOf(RegistrationIdentifier("LEIXG-529900T8BM49AURSDO55")),
        name = "ACME PID Provider",
        legalName = "ACME Corp AE",
        country = "GR",
        entitlements = listOf("https://uri.etsi.org/19475/Entitlement/PID_Provider"),
        infoUri = "https://acme.example/info",
        supportUri = "https://acme.example/support",
    )

    private val leiIdentifier = QualifiedIdentifier(QualifiedIdentifier.LEI, "529900T8BM49AURSDO55")

    @Test
    fun `a verified result stores the log-relevant registration fields`() {
        val stored = RegistrationCertificateResult.Verified(certificate).toStoredIssuerRegistration()

        assertEquals(
            StoredIssuerRegistration(
                name = "ACME PID Provider",
                legalName = "ACME Corp AE",
                identifiers = listOf("LEIXG-529900T8BM49AURSDO55"),
                entitlements = listOf("https://uri.etsi.org/19475/Entitlement/PID_Provider"),
                country = "GR",
                infoUri = "https://acme.example/info",
                supportUri = "https://acme.example/support"
            ),
            stored,
        )
    }

    @Test
    fun `a failed but parsed result still stores its registration`() {
        val failed = RegistrationCertificateResult.Failed(RegistrationFailureReason.EXPIRED, certificate)

        assertEquals("ACME Corp AE", failed.toStoredIssuerRegistration()?.legalName)
    }

    @Test
    fun `a result without a parsed registration stores nothing`() {
        val failed = RegistrationCertificateResult.Failed(RegistrationFailureReason.EXPIRED, registration = null)

        assertNull(failed.toStoredIssuerRegistration())
        assertNull((null as RegistrationCertificateResult?).toStoredIssuerRegistration())
    }

    @Test
    fun `the reconstructed certificate carries the interacting-party fields for the log`() {
        val reconstructed = RegistrationCertificateResult.Verified(certificate)
            .toStoredIssuerRegistration()!!
            .toRegistrationCertificate()

        assertEquals("ACME PID Provider", reconstructed.name)
        assertEquals("ACME Corp AE", reconstructed.legalName)
        assertEquals("GR", reconstructed.country)
        assertEquals("https://acme.example/info", reconstructed.infoUri)
        assertEquals("https://acme.example/support", reconstructed.supportUri)
        assertEquals(listOf("https://uri.etsi.org/19475/Entitlement/PID_Provider"), reconstructed.entitlements)
        // structuredIdentifier() works on the reconstructed certificate, exactly as on the original.
        assertEquals(leiIdentifier, reconstructed.structuredIdentifier())
    }

    @Test
    fun `the accessor reads the stored registration from a deferred document`() {
        val document = deferredDocumentWith(
            RegistrationCertificateResult.Verified(certificate).toStoredIssuerRegistration(),
        )

        val registration = document.storedIssuerRegistration()

        assertEquals("ACME Corp AE", registration?.legalName)
        assertEquals(leiIdentifier, registration?.structuredIdentifier())
    }

    @Test
    fun `the accessor returns null when no registration was captured`() {
        assertNull(deferredDocumentWith(interactingParty = null).storedIssuerRegistration())
    }

    @Test
    fun `the accessor never throws on unreadable related data`() {
        val document = mockk<DeferredDocument> { every { relatedData } returns byteArrayOf(1, 2, 3) }

        assertNull(document.storedIssuerRegistration())
    }

    @Test
    fun `the accessor reads whether the User started the issuance`() {
        assertEquals(true, deferredDocumentWith(isUserTriggered = true).storedIsUserTriggered())
        assertEquals(false, deferredDocumentWith(isUserTriggered = false).storedIsUserTriggered())
        assertNull(deferredDocumentWith().storedIsUserTriggered())
        assertNull(
            mockk<DeferredDocument> { every { relatedData } returns byteArrayOf(1, 2, 3) }
                .storedIsUserTriggered(),
        )
    }

    private fun deferredDocumentWith(
        interactingParty: StoredIssuerRegistration? = null,
        isUserTriggered: Boolean? = null,
    ): DeferredDocument {
        val bytes = Json.encodeToString(
            StoredDeferredContext(
                credentialIssuerId = "https://issuer.example.com",
                deferredEndpoint = "https://issuer.example.com/deferred",
                tokenEndpoint = "https://auth.example.com/token",
                authorizationServerId = "https://auth.example.com",
                clientId = "wallet-client",
                popKeyAliases = listOf("key-1"),
                transactionId = "tx-123",
                accessToken = "access-token",
                interactingParty = interactingParty,
                isUserTriggered = isUserTriggered,
            ),
        ).toByteArray(Charsets.UTF_8)
        return mockk { every { relatedData } returns bytes }
    }
}
