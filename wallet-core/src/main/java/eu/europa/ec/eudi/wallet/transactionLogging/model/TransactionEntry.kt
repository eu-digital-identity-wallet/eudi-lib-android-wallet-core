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

package eu.europa.ec.eudi.wallet.transactionLogging.model

import android.annotation.SuppressLint
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import java.time.Instant

/**
 * A single entry in the transaction log, as a typed Kotlin model.
 *
 * Every kind of transaction — a presentation, an issuance, a deletion, and so on — is its own
 * subtype, so you can tell them apart with a `when` and read their fields type-safely. The model
 * follows the EUDI Wallet Technical Specification 10 (TS10): the shared fields below come from
 * §3.1, and each subtype maps to a TS10 transaction class (its §3.x reference is noted on it).
 *
 * To save and load entries, use [toJson] and [toTransactionEntryOrNull]. To produce the read-only
 * TS10 §4.1 export format, use [TransactionLogExport].
 *
 * Many fields are nullable and are left `null` when the wallet can't fill them in yet — usually
 * because the value comes from a registry or trust source that isn't integrated. Whatever the
 * current wallet flows can provide is always filled in.
 */
@OptIn(ExperimentalSerializationApi::class)
@SuppressLint("UnsafeOptInUsageError")
@Serializable
@JsonClassDiscriminator("transactionType")
sealed interface TransactionEntry {

    /** Unique identifier of the transaction (TS10 §3.1 `transactionIdentifier`). */
    val transactionIdentifier: String

    /** Date and time of the transaction (TS10 §3.1 `time`; serialized as ISO 8601). */
    val time: Instant

    /** The transaction type discriminator (TS10 §3.1 `transactionType`). */
    val transactionType: TransactionType

    /** The transaction result (TS10 §3.1 `transactionResult`). */
    val transactionResult: TransactionResult

    /**
     * A presentation transaction (TS10 §3.2).
     *
     * Records which claims were involved (via [ClaimInfo]), never their values.
     */
    @Serializable
    @SerialName("Presentation")
    data class Presentation(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        val listOfClaimsRequested: List<ClaimInfo>,
        val listOfClaimsPresented: List<ClaimInfo>,
        /** Always `ServiceProvider` here (TS10 §3.2). */
        val interactingPartyType: String = INTERACTING_PARTY_TYPE,
        val interactingPartyName: MultiLangString? = null,
        val interactingPartyIdentifier: Identifier? = null,
        val interactingPartyContact: List<String>? = null,
        val isIntermediary: Boolean? = null,
        val intermediaryIdentifier: Identifier? = null,
        val intermediaryName: MultiLangString? = null,
        val intermediaryContact: List<String>? = null,
        val registrarURL: String? = null,
        val purpose: List<MultiLangString>? = null,
        val privacyPolicy: List<Policy>? = null,
        val dpaName: MultiLangString? = null,
        val dpaCountry: MultiLangString? = null,
        val dpaContact: List<String>? = null,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.Presentation

        companion object {
            const val INTERACTING_PARTY_TYPE = "ServiceProvider"
        }
    }

    /**
     * Shared payload for credential issuance and re-issuance (TS10 §3.5).
     *
     * Used by both [CredentialIssuance] and [CredentialReissuance].
     */
    @Serializable
    data class CredentialIssuanceDetails(
        val credentialNumberRequested: Int,
        val credentialNumberIssued: Int,
        /** IDs of the issued credentials; each is the `vct` or `docType` value. */
        val credentialIdentifier: List<String>,
        val isUserTriggered: Boolean? = null,
        val interactingPartyName: MultiLangString? = null,
        val interactingPartyIdentifier: Identifier? = null,
        /** One of `QEAAProvider`, `NonQEAAProvider`, `PubEEAProvider`, `PIDProvider` (TS10 §3.5). */
        val interactingPartyType: String? = null,
        val interactingPartyContact: List<String>? = null,
    )

    /** A credential (PID or attestation) issuance transaction (TS10 §3.5). */
    @Serializable
    @SerialName("CredentialIssuance")
    data class CredentialIssuance(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        val details: CredentialIssuanceDetails,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.CredentialIssuance
    }

    /**
     * A credential (PID or attestation) re-issuance transaction.
     *
     * Same shape as [CredentialIssuance]; differs only by transaction type (TS10 §3.5).
     */
    @Serializable
    @SerialName("CredentialReissuance")
    data class CredentialReissuance(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        val details: CredentialIssuanceDetails,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.CredentialReissuance
    }

    /** A credential (PID or attestation) deletion by the user (TS10 §3.6). */
    @Serializable
    @SerialName("CredentialDeletion")
    data class CredentialDeletion(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        /** ID of the deleted credential; the `vct` or `docType` value. */
        val credentialIdentifier: String,
        val credentialIssuerIdentifier: Identifier? = null,
        val credentialIssuerName: MultiLangString? = null,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.CredentialDeletion
    }

    /** A signature or seal creation transaction (TS10 §3.12). */
    @Serializable
    @SerialName("SigningSealing")
    data class SigningSealing(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        /** Always `ESigESealCreationProvider` here (TS10 §3.12). */
        val interactingPartyType: String = INTERACTING_PARTY_TYPE,
        val signingTransactionIdentifier: String? = null,
        /** ID of the certificate used; the X.509 `serialNumber` (RFC 5280). */
        val certificateIdentifier: String? = null,
        /** Data-to-be-signed representation. */
        val dtbsr: String? = null,
        val fileIdentifier: String? = null,
        val fileName: String? = null,
        val fileSize: String? = null,
        val interactingPartyName: MultiLangString? = null,
        val interactingPartyIdentifier: Identifier? = null,
        val interactingPartyContact: List<String>? = null,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.SigningSealing

        companion object {
            const val INTERACTING_PARTY_TYPE = "ESigESealCreationProvider"
        }
    }

    /** A data deletion request sent to a relying party (TS10 §3.13). */
    @Serializable
    @SerialName("DataDeletionRequest")
    data class DataDeletionRequest(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        val listOfClaims: List<ClaimInfo>,
        val interactingPartyIdentifier: Identifier? = null,
        val interactingPartyName: MultiLangString? = null,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.DataDeletionRequest
    }

    /** A suspicious transaction report sent to a data protection authority (TS10 §3.14). */
    @Serializable
    @SerialName("DPAReport")
    data class DPAReport(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        val dpaName: MultiLangString? = null,
        val dpaCountry: MultiLangString? = null,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.DPAReport
    }

    // region Other TS10 subtypes, modeled for completeness.

    /** A certificate or key pair issuance for signing/sealing (TS10 §3.10). */
    @Serializable
    @SerialName("CertificateIssuance")
    data class CertificateIssuance(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        /** One of `QCertForESealProvider`, `QCertForESigProvider` (TS10 §3.10). */
        val interactingPartyType: String? = null,
        /** ID of the issued certificate; the X.509 `serialNumber` (RFC 5280). */
        val certificateIdentifier: String? = null,
        val interactingPartyName: MultiLangString? = null,
        val interactingPartyIdentifier: Identifier? = null,
        val interactingPartyContact: List<String>? = null,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.CertificateIssuance
    }

    /** Certificate or key pair deletion (TS10 §3.11). */
    @Serializable
    @SerialName("CertificateDeletion")
    data class CertificateDeletion(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        /** ID of the deleted certificate; the X.509 `serialNumber` (RFC 5280). */
        val certificateIdentifier: String,
        val certificateIssuerIdentifier: Identifier? = null,
        val certificateIssuerName: MultiLangString? = null,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.CertificateDeletion
    }

    /** Wallet-to-Wallet presentation transaction (TS10 §3.4). */
    @Serializable
    @SerialName("W2WPresentation")
    data class W2WPresentation(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        val listOfClaimsRequested: List<ClaimInfo>,
        val listOfClaimsPresented: List<ClaimInfo>,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.W2WPresentation
    }

    /** Wallet-to-Wallet presentation request transaction (TS10 §3.3). */
    @Serializable
    @SerialName("W2WPresentationRequest")
    data class W2WPresentationRequest(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        val listOfClaimsRequested: List<ClaimInfo>,
        val listOfClaimsPresented: List<ClaimInfo>,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.W2WPresentationRequest
    }

    /** Pseudonym generation transaction (TS10 §3.7). */
    @Serializable
    @SerialName("PseudonymGeneration")
    data class PseudonymGeneration(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        val pseudonym: Pseudonym,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.PseudonymGeneration
    }

    /** Pseudonym deletion transaction (TS10 §3.8). */
    @Serializable
    @SerialName("PseudonymDeletion")
    data class PseudonymDeletion(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        val pseudonym: Pseudonym,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.PseudonymDeletion
    }

    /** A pseudonym presentation (pseudonymous authentication) transaction (TS10 §3.9). */
    @Serializable
    @SerialName("PseudonymPresentation")
    data class PseudonymousAuthentication(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        val pseudonym: Pseudonym,
        val interactingPartyIdentifier: Identifier? = null,
        val interactingPartyType: String? = null,
        val interactingPartyName: MultiLangString? = null,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.PseudonymPresentation
    }

    /** Any other transaction, e.g. backup or migration export (TS10 §3.15). */
    @Serializable
    @SerialName("OtherTransaction")
    data class OtherTransaction(
        override val transactionIdentifier: String,
        @Serializable(with = InstantIso8601Serializer::class) override val time: Instant,
        override val transactionResult: TransactionResult,
        val description: List<String>,
    ) : TransactionEntry {
        override val transactionType: TransactionType get() = TransactionType.OtherTransaction
    }

    // endregion
}
