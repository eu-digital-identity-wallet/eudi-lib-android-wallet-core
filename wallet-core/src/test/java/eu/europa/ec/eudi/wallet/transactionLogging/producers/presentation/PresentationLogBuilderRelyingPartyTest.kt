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

package eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation

import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.wallet.registration.Intermediary
import eu.europa.ec.eudi.wallet.registration.LocalizedText
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.RegistrationFailureReason
import eu.europa.ec.eudi.wallet.registration.RegistrationIdentifier
import eu.europa.ec.eudi.wallet.registration.SupervisoryAuthority
import eu.europa.ec.eudi.wallet.registration.QualifiedIdentifier
import eu.europa.ec.eudi.wallet.transactionLogging.model.MultiLangString
import eu.europa.ec.eudi.wallet.transactionLogging.model.Policy
import io.mockk.every
import io.mockk.mockk
import org.multipaz.trustmanagement.TrustMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests how [PresentationLogBuilder.withRelyingParty] fills the interacting-party fields (TS10 §3.2)
 * from the relying party's registration certificate (WRPRC), and how it falls back to the trust
 * metadata display name when no certificate is present.
 */
class PresentationLogBuilderRelyingPartyTest {

    private val builder = PresentationLogBuilder()

    @Test
    fun `a verified registration certificate populates all interacting-party fields`() {
        val rc = RegistrationCertificate(
            identifiers = listOf(RegistrationIdentifier(value = "LEIXG-529900T8BM49AURSDO55")),
            name = "ACME Verifier",
            legalName = "ACME Corp AE",
            country = "GR",
            supportUri = "https://acme.example/support",
            infoUri = "https://acme.example/info",
            registryUri = "https://registrar.example/wrp/123",
            privacyPolicyUri = "https://acme.example/privacy",
            purpose = listOf(
                LocalizedText(language = "en", value = "Age verification"),
                LocalizedText(language = "el", value = "Επαλήθευση ηλικίας"),
            ),
            supervisoryAuthority = SupervisoryAuthority(
                name = "HDPA",
                email = "contact@dpa.gr",
                phone = "+302101000000",
                uri = "https://dpa.gr",
            ),
            intermediary = Intermediary(
                identifier = "LEIXG-987600ABCDEF12345678",
                name = "Intermediary Ltd",
            ),
        )

        val log = builder.withRelyingParty(
            builder.createEmptyPresentationLog(),
            success(registration = RegistrationCertificateResult.Verified(rc)),
        )

        assertEquals(MultiLangString(lang = "en", content = "ACME Corp AE"), log.interactingPartyName)
        assertEquals(QualifiedIdentifier(type = QualifiedIdentifier.LEI, value = "529900T8BM49AURSDO55"), log.interactingPartyIdentifier)
        assertEquals(
            listOf("GR", "https://acme.example/support", "https://acme.example/info"),
            log.interactingPartyContact,
        )
        assertEquals(true, log.isIntermediary)
        assertEquals(
            QualifiedIdentifier(type = QualifiedIdentifier.LEI, value = "987600ABCDEF12345678"),
            log.intermediaryIdentifier,
        )
        assertEquals(MultiLangString(lang = "en", content = "Intermediary Ltd"), log.intermediaryName)
        assertEquals("https://registrar.example/wrp/123", log.registrarURL)
        assertEquals(
            listOf(
                MultiLangString(lang = "en", content = "Age verification"),
                MultiLangString(lang = "el", content = "Επαλήθευση ηλικίας"),
            ),
            log.purpose,
        )
        assertEquals(
            listOf(Policy(type = Policy.PRIVACY_STATEMENT, policyURI = "https://acme.example/privacy")),
            log.privacyPolicy,
        )
        assertEquals(MultiLangString(lang = "en", content = "HDPA"), log.dpaName)
        assertEquals(listOf("contact@dpa.gr", "+302101000000", "https://dpa.gr"), log.dpaContact)
    }

    @Test
    fun `a natural person is named by given and family name`() {
        val rc = RegistrationCertificate(
            name = "ACME Verifier",
            givenName = "Maria",
            familyName = "Papadopoulou",
        )

        val log = builder.withRelyingParty(
            builder.createEmptyPresentationLog(),
            success(registration = RegistrationCertificateResult.Verified(rc)),
        )

        assertEquals(MultiLangString(lang = "en", content = "Maria Papadopoulou"), log.interactingPartyName)
    }

    @Test
    fun `the trade name is used only when the certificate carries no registered name`() {
        val rc = RegistrationCertificate(name = "ACME Verifier")

        val log = builder.withRelyingParty(
            builder.createEmptyPresentationLog(),
            success(registration = RegistrationCertificateResult.Verified(rc)),
        )

        assertEquals(MultiLangString(lang = "en", content = "ACME Verifier"), log.interactingPartyName)
    }

    @Test
    fun `an identifier with an unrecognised scheme prefix is skipped`() {
        val rc = RegistrationCertificate(
            // "XYZ" is not one of the ETSI TS 119 475 Table 2 semantic-identifier prefixes.
            identifiers = listOf(RegistrationIdentifier(value = "XYZAB-123456789")),
            name = "ACME Verifier",
        )

        val log = builder.withRelyingParty(
            builder.createEmptyPresentationLog(),
            success(registration = RegistrationCertificateResult.Verified(rc)),
        )

        assertNull(log.interactingPartyIdentifier)
    }

    @Test
    fun `the semantic identifier prefix is decoded to the TS2 scheme and bare value`() {
        // ETSI TS 119 475 Table 2: VAT -> VATIN (default; TIN also maps from VAT), NTR -> EUID.
        fun identifierOf(sub: String) = builder.withRelyingParty(
            builder.createEmptyPresentationLog(),
            success(
                registration = RegistrationCertificateResult.Verified(
                    RegistrationCertificate(identifiers = listOf(RegistrationIdentifier(value = sub))),
                ),
            ),
        ).interactingPartyIdentifier

        assertEquals(QualifiedIdentifier(type = QualifiedIdentifier.LEI, value = "529900T8BM49AURSDO55"), identifierOf("LEIXG-529900T8BM49AURSDO55"))
        assertEquals(QualifiedIdentifier(type = QualifiedIdentifier.VATIN, value = "123456789"), identifierOf("VATDE-123456789"))
        assertEquals(QualifiedIdentifier(type = QualifiedIdentifier.EUID, value = "HRB12345"), identifierOf("NTRDE-HRB12345"))
        // A recognised prefix is not enough: without an identifier after it there is nothing to record.
        assertNull(identifierOf("LEI529900T8BM49AURSDO55"))
        assertNull(identifierOf("LEIXG-"))
    }

    @Test
    fun `the contact list starts with the country and keeps whatever URIs the certificate carries`() {
        fun contactOf(rc: RegistrationCertificate) = builder.withRelyingParty(
            builder.createEmptyPresentationLog(),
            success(registration = RegistrationCertificateResult.Verified(rc)),
        ).interactingPartyContact

        assertEquals(listOf("GR"), contactOf(RegistrationCertificate(country = "GR")))
        assertEquals(
            listOf("GR", "https://acme.example/info"),
            contactOf(RegistrationCertificate(country = "GR", infoUri = "https://acme.example/info")),
        )
        assertNull(contactOf(RegistrationCertificate(name = "ACME Verifier")))
    }

    @Test
    fun `an intermediary is recorded even when its identifier has no recognised scheme`() {
        val rc = RegistrationCertificate(
            intermediary = Intermediary(identifier = "int-1", name = "Intermediary Ltd"),
        )

        val log = builder.withRelyingParty(
            builder.createEmptyPresentationLog(),
            success(registration = RegistrationCertificateResult.Verified(rc)),
        )

        assertEquals(true, log.isIntermediary)
        assertNull(log.intermediaryIdentifier)
    }

    @Test
    fun `a failed result still maps the parsed registration when one is present`() {
        val rc = RegistrationCertificate(name = "ACME Verifier")
        val failed = RegistrationCertificateResult.Failed(
            reason = RegistrationFailureReason.EXPIRED,
            registration = rc,
        )

        val log = builder.withRelyingParty(builder.createEmptyPresentationLog(), success(registration = failed))

        assertEquals(MultiLangString(lang = "en", content = "ACME Verifier"), log.interactingPartyName)
    }

    @Test
    fun `without a certificate the name comes from the trust metadata display name`() {
        val log = builder.withRelyingParty(
            builder.createEmptyPresentationLog(),
            success(registration = null, displayName = "Web Verifier (PROD)"),
        )

        assertEquals(MultiLangString(lang = "en", content = "Web Verifier (PROD)"), log.interactingPartyName)
        assertNull(log.interactingPartyIdentifier)
        assertNull(log.interactingPartyContact)
        assertTrue(log.purpose == null && log.privacyPolicy == null)
    }

    @Test
    fun `without a certificate or display name the name is the unidentified fallback`() {
        val log = builder.withRelyingParty(
            builder.createEmptyPresentationLog(),
            success(registration = null, displayName = null),
        )

        assertEquals(MultiLangString(lang = "en", content = "Unidentified Relying Party"), log.interactingPartyName)
    }

    private fun success(
        registration: RegistrationCertificateResult?,
        displayName: String? = null,
    ): RequestProcessor.ProcessedRequest.Success {
        val processed = mockk<RequestProcessor.ProcessedRequest.Success>()
        every { processed.wrpRegistration } returns registration
        every { processed.trustMetadata } returns displayName?.let { TrustMetadata(displayName = it) }
        every { processed.getOrNull() } returns processed
        return processed
    }
}
