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

package eu.europa.ec.eudi.wallet.transactionLogging.producers

import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.issue.openid4vci.DeferredIssueResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.issue.openid4vci.storedIssuerRegistration
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.registration.QualifiedIdentifier
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.registration.issuer.IssuerEntitlements
import eu.europa.ec.eudi.wallet.registration.structuredIdentifier
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogManager
import eu.europa.ec.eudi.wallet.transactionLogging.model.MultiLangString
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executor

/**
 * Wraps an [OpenId4VciManager] and logs a [TransactionEntry.CredentialIssuance] (or
 * [TransactionEntry.CredentialReissuance]) entry for each issuance attempt (TS10 §3.5), by watching
 * the issuance events.
 *
 * An entry is written once issuance actually begins (after the authorization step) and updated when
 * it finishes or fails. An issuance that begins but never finishes stays logged as not completed.
 * An issuance abandoned before it begins (e.g. the user backs out of the authorization browser) is
 * not logged. Deferred credentials get their own entry that is updated once they resolve. The host's
 * callback is always forwarded unchanged.
 *
 * @property delegate the wrapped [OpenId4VciManager].
 * @property transactionLogManager records entries.
 * @property logger optional logger for internal errors.
 * @property documentResolver reads a document by id, only to recover the issuer name of a document
 *   being re-issued so a failed or pending re-issuance is not nameless. Defaults to a no-op; a
 *   failing resolver never disrupts issuance.
 */
class CredentialIssuanceLogger(
    private val delegate: OpenId4VciManager,
    private val transactionLogManager: TransactionLogManager,
    private val logger: Logger? = null,
    private val documentResolver: (DocumentId) -> Document? = { null },
) : OpenId4VciManager by delegate {

    override fun issueDocumentByConfigurationIdentifiers(
        issuerUrl: String,
        credentialConfigurationIds: List<String>,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) = delegate.issueDocumentByConfigurationIdentifiers(
        issuerUrl, credentialConfigurationIds, txCode, executor,
        wrapIssue(onIssueEvent, seed = IssuanceSeed(requested = credentialConfigurationIds.size))
    )

    override fun issueDocumentByFormat(
        issuerUrl: String,
        format: DocumentFormat,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) = delegate.issueDocumentByFormat(issuerUrl, format, txCode, executor, wrapIssue(onIssueEvent))

    override fun issueDocumentByConfigurationIdentifier(
        issuerUrl: String,
        credentialConfigurationId: String,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) = delegate.issueDocumentByConfigurationIdentifier(
        issuerUrl, credentialConfigurationId, txCode, executor,
        wrapIssue(onIssueEvent, seed = IssuanceSeed(requested = 1))
    )

    override fun issueDocumentByDocType(
        issuerUrl: String,
        docType: String,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) = delegate.issueDocumentByDocType(
        issuerUrl, docType, txCode, executor,
        wrapIssue(onIssueEvent, seed = IssuanceSeed(requested = 1))
    )


    override fun issueDocumentByOffer(
        offer: Offer,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) = delegate.issueDocumentByOffer(
        // A credential offer comes from the issuer, so it is not user-triggered (TS10 §3.5).
        offer, txCode, executor,
        wrapIssue(onIssueEvent, seed = offer.seedOrEmpty(), userTriggered = false)
    )

    override fun issueDocumentByOfferUri(
        offerUri: String,
        txCode: String?,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) = delegate.issueDocumentByOfferUri(
        // A credential offer (from a URI) comes from the issuer, so it is not user-triggered.
        offerUri, txCode, executor, wrapIssue(onIssueEvent, userTriggered = false)
    )

    override fun reissueDocument(
        documentId: DocumentId,
        allowAuthorizationFallback: Boolean,
        executor: Executor?,
        onIssueEvent: OpenId4VciManager.OnIssueEvent,
    ) = delegate.reissueDocument(
        documentId, allowAuthorizationFallback, executor,
        // allowAuthorizationFallback tells user-triggered re-issuance (true) from background
        // re-issuance (false), used for isUserTriggered (TS10 §3.5). Pre-fill the issuer name from the
        // document being re-issued, since a failure event does not expose it.
        wrapIssue(
            onIssueEvent,
            reissuance = true,
            userTriggered = allowAuthorizationFallback,
            seed = IssuanceSeed(
                issuerName = documentResolver(documentId)?.issuerMetadata?.issuerName()
            ),
        )
    )

    override fun issueDeferredDocument(
        deferredDocument: DeferredDocument,
        executor: Executor?,
        onIssueResult: OpenId4VciManager.OnDeferredIssueResult,
    ) = delegate.issueDeferredDocument(
        deferredDocument, executor,
        wrapDeferred(onIssueResult, registration = deferredDocument.storedIssuerRegistration())
    )

    /**
     * Wraps an issuance callback so each issuance is logged. An entry is written once issuance begins
     * ([IssueEvent.Started], after the authorization step) and updated when it finishes or fails; all
     * updates share one identifier, so they stay a single row. Nothing is logged before issuance
     * begins. The [seed] pre-fills what is already known (issuer name, requested count).
     *
     * [userTriggered] records TS10 §3.5 `isUserTriggered`: `true` when the user started the issuance
     * from the wallet, `false` for an issuer credential offer or a background re-issuance.
     */
    private fun wrapIssue(
        downstream: OpenId4VciManager.OnIssueEvent,
        reissuance: Boolean = false,
        seed: IssuanceSeed = IssuanceSeed(),
        userTriggered: Boolean = true,
    ): OpenId4VciManager.OnIssueEvent {
        val aggregator = IssuanceAggregator(reissuance, seed, userTriggered)
        return OpenId4VciManager.OnIssueEvent { event ->
            runCatching {
                aggregator.onEvent(event).forEach { transactionLogManager.log(it) }
            }.onFailure { logError(it, "issue") }
            downstream(event)
        }
    }

    /**
     * Wraps a deferred-issuance callback; logs one issuance entry per terminal deferred outcome.
     */
    private fun wrapDeferred(
        downstream: OpenId4VciManager.OnDeferredIssueResult,
        registration: RegistrationCertificate? = null
    ): OpenId4VciManager.OnDeferredIssueResult = OpenId4VciManager.OnDeferredIssueResult { result ->
        runCatching {
            val entry = when (result) {
                is DeferredIssueResult.DocumentIssued ->
                    deferredResolutionEntry(result.document, result.documentId, TransactionResult.Completed, registration)

                is DeferredIssueResult.DocumentFailed ->
                    deferredResolutionEntry(
                        result.document,
                        result.documentId,
                        TransactionResult.NotCompleted(
                            result.cause.toNoncompletionReason(REASON_ISSUANCE_FAILED)
                        ),
                        registration
                    )

                is DeferredIssueResult.DocumentExpired ->
                    deferredResolutionEntry(
                        result.document,
                        result.documentId,
                        TransactionResult.NotCompleted("Deferred credential expired"),
                        registration
                    )

                is DeferredIssueResult.DocumentNotReady -> null // still pending, not a terminal state
            }
            entry?.let { transactionLogManager.log(it) }
        }.onFailure { logError(it, "issueDeferred") }
        downstream(result)
    }

    private fun deferredResolutionEntry(
        document: Document,
        documentId: DocumentId,
        result: TransactionResult,
        registration: RegistrationCertificate?,
    ): TransactionEntry.CredentialIssuance {
        val completed = result is TransactionResult.Completed
        val party = registration.toInteractingParty(fallbackName = document.issuerMetadata?.issuerName())
        return TransactionEntry.CredentialIssuance(
            // Keyed by the DocumentId so this updates the earlier "awaiting" row instead of duplicating it.
            transactionIdentifier = deferredTxId(documentId),
            time = Instant.now(),
            transactionResult = result,
            details = TransactionEntry.CredentialIssuanceDetails(
                credentialNumberRequested = 1,
                credentialNumberIssued = if (completed) 1 else 0,
                credentialIdentifier = if (completed) listOf(document.credentialIdentifier()) else emptyList(),
                isUserTriggered = null,
                interactingPartyName = party.name,
                interactingPartyIdentifier = party.identifier,
                interactingPartyType = party.type,
                interactingPartyContact = party.contact
            ),
        )
    }

    private fun logError(e: Throwable, source: String) {
        logger?.log(
            Logger.Record(
                level = Logger.LEVEL_ERROR,
                message = "Failed to log credential issuance",
                thrown = e,
                sourceClassName = CredentialIssuanceLogger::class.java.name,
                sourceMethod = source,
            )
        )
    }

    /**
     * Collects the [IssueEvent]s of one issuance call and builds the entries to log: a not-completed
     * entry when issuance begins, then the final entry when it finishes or fails. Both share the same
     * [id]/[time], so the final one updates the first row.
     *
     * If issuance fails before it begins (no [IssueEvent.Started]), nothing is logged.
     */
    private class IssuanceAggregator(
        private val reissuance: Boolean,
        seed: IssuanceSeed = IssuanceSeed(),
        private val userTriggered: Boolean = true,
    ) {
        private val id = UUID.randomUUID().toString()
        private val time = Instant.now()
        private var requested = seed.requested
        private val issuedIdentifiers = mutableListOf<String>()
        private var issuerName: MultiLangString? = seed.issuerName
        private var issuerRegistration: RegistrationCertificate? = null
        private val deferredDocIds = mutableListOf<DocumentId>()
        private var failureReason: String? = null
        private var wroteAhead = false
        private var finalized = false

        /**
         * Returns the entries to log for this event, or an empty list if the event changes nothing.
         */
        fun onEvent(event: IssueEvent): List<TransactionEntry> {
            return when (event) {
                is IssueEvent.Started -> {
                    requested = event.total // real count, replaces the seed estimate
                    wroteAhead = true
                    // Write the pending entry with the real requested count.
                    listOf(buildEntry(TransactionResult.NotCompleted()))
                }

                is IssueEvent.DocumentIssued -> {
                    issuedIdentifiers.add(event.document.credentialIdentifier())
                    if (issuerName == null) issuerName = event.document.issuerMetadata?.issuerName()
                    emptyList()
                }

                is IssueEvent.DocumentDeferred -> {
                    // Remember the deferred document by id; it gets its own "awaiting" row at finalize.
                    deferredDocIds.add(event.document.id)
                    // A deferred outcome has no DocumentIssued, so grab the issuer name here too.
                    if (issuerName == null) issuerName = event.document.issuerMetadata?.issuerName()
                    emptyList()
                }

                is IssueEvent.DocumentFailed -> {
                    failureReason = event.cause.toNoncompletionReason(REASON_ISSUANCE_FAILED)
                    emptyList()
                }

                is IssueEvent.DocumentRequiresCreateSettings -> {
                    // The issuer registration rides on the offer this event carries; grab it once for
                    // the entry's interacting-party fields (TS10 §3.5).
                    if (issuerRegistration == null) {
                        issuerRegistration = event.offeredDocument.offer.issuerRegistration.registrationOrNull()
                    }
                    emptyList()
                }

                is IssueEvent.Finished -> finalize(overallError = null)
                is IssueEvent.Failure ->
                    finalize(overallError = event.cause.toNoncompletionReason(REASON_ISSUANCE_FAILED))
                else -> emptyList() // interactive steps, nothing to log yet
            }
        }

        private fun finalize(overallError: String?): List<TransactionEntry> {
            // If issuance never began, do not log a final entry either.
            if (!wroteAhead || finalized) return emptyList()
            finalized = true
            val deferredCount = deferredDocIds.size
            // The non-deferred credentials are the ones this call resolved right away (issued or failed).
            val nonDeferred = requested - deferredCount
            return when {
                // The whole flow failed (e.g. token or network error): one entry with the failure reason.
                overallError != null ->
                    listOf(buildEntry(TransactionResult.NotCompleted(overallError)))

                // Only deferred credentials: the batch entry itself carries the "awaiting" state. This
                // reason marks it as pending, not failed, so the UI can show it neutrally.
                nonDeferred <= 0 && deferredCount > 0 ->
                    listOf(buildEntry(TransactionResult.NotCompleted(REASON_ISSUANCE_DEFERRED)))

                // Mixed or fully synchronous: the batch entry counts only the non-deferred credentials,
                // and each deferred credential gets its own "awaiting" row.
                else -> {
                    val batchResult =
                        if (issuedIdentifiers.size == nonDeferred) TransactionResult.Completed
                        else TransactionResult.NotCompleted(failureReason ?: REASON_ISSUANCE_FAILED)
                    buildList {
                        add(buildEntry(batchResult, requestedCount = nonDeferred))
                        deferredDocIds.forEach { add(deferredAwaitingEntry(it)) }
                    }
                }
            }
        }

        private fun buildEntry(
            result: TransactionResult,
            requestedCount: Int = requested,
        ): TransactionEntry {
            val party = issuerRegistration.toInteractingParty(fallbackName = issuerName)
            val details = TransactionEntry.CredentialIssuanceDetails(
                credentialNumberRequested = requestedCount,
                credentialNumberIssued = issuedIdentifiers.size,
                credentialIdentifier = issuedIdentifiers.toList(),
                isUserTriggered = userTriggered,
                interactingPartyName = party.name,
                interactingPartyIdentifier = party.identifier,
                interactingPartyType = party.type,
                interactingPartyContact = party.contact
            )
            return if (reissuance) {
                TransactionEntry.CredentialReissuance(id, time, result, details)
            } else {
                TransactionEntry.CredentialIssuance(id, time, result, details)
            }
        }

        /**
         * An "awaiting" row for one deferred credential, keyed by its [DocumentId] so the later
         * resolution updates this row instead of adding a duplicate. Always a
         * [TransactionEntry.CredentialIssuance], since the resolution callback cannot tell it was a
         * re-issuance.
         */
        private fun deferredAwaitingEntry(documentId: DocumentId): TransactionEntry {
            val party = issuerRegistration.toInteractingParty(fallbackName = issuerName)
            return TransactionEntry.CredentialIssuance(
                transactionIdentifier = deferredTxId(documentId),
                time = time,
                transactionResult = TransactionResult.NotCompleted(REASON_ISSUANCE_DEFERRED),
                details = TransactionEntry.CredentialIssuanceDetails(
                    credentialNumberRequested = 1,
                    credentialNumberIssued = 0,
                    credentialIdentifier = emptyList(),
                    isUserTriggered = userTriggered,
                    interactingPartyName = party.name,
                    interactingPartyIdentifier = party.identifier,
                    interactingPartyType = party.type,
                    interactingPartyContact = party.contact
                )
            )
        }
    }

    /**
     * What is already known about an issuance before any [IssueEvent] arrives, used to pre-fill the
     * entry. Fields default to "unknown" and are filled in by the events.
     */
    private class IssuanceSeed(
        val requested: Int = 0,
        val issuerName: MultiLangString? = null,
    )

    companion object {
        private const val DEFAULT_LANG = "en"

        /** Fallback reason when an issuance fails/does not complete without a usable error message. */
        private const val REASON_ISSUANCE_FAILED = "Issuance did not complete"

        /** Reason recorded when the issuer deferred delivery and nothing was issued yet (not a failure). */
        private const val REASON_ISSUANCE_DEFERRED = "Credential issuance deferred — awaiting the credential"

        /**
         * Stable transaction id for a deferred credential's row, so the "awaiting" entry and its later
         * resolution share one row. Keyed by the [DocumentId].
         */
        private fun deferredTxId(documentId: DocumentId): String = "deferred:$documentId"

        /**
         * Builds an [IssuanceSeed] from a resolved [Offer] (the issuer name and requested count are
         * known up front). Best-effort: any failure falls back to an empty seed so logging never
         * disrupts issuance.
         */
        private fun Offer.seedOrEmpty(): IssuanceSeed = runCatching {
            IssuanceSeed(
                requested = offeredDocuments.size,
                issuerName = issuerMetadata.display.firstOrNull()?.let {
                    MultiLangString(lang = it.locale?.toLanguageTag() ?: DEFAULT_LANG, content = it.name)
                },
            )
        }.getOrElse { IssuanceSeed() }

        private fun Document.credentialIdentifier(): String = when (val f = format) {
            is MsoMdocFormat -> f.docType
            is SdJwtVcFormat -> f.vct
        }

        private fun IssuerMetadata.issuerName(): MultiLangString? =
            issuerDisplay?.firstOrNull()?.let {
                MultiLangString(lang = it.locale?.toLanguageTag() ?: DEFAULT_LANG, content = it.name)
            }

        private const val TYPE_PID = "PIDProvider"
        private const val TYPE_QEAA = "QEAAProvider"
        private const val TYPE_PUB_EAA = "PubEEAProvider"
        private const val TYPE_NON_Q_EAA = "NonQEAAProvider"

        private fun RegistrationCertificateResult?.registrationOrNull(): RegistrationCertificate? =
            when (this) {
                is RegistrationCertificateResult.Verified -> registration
                is RegistrationCertificateResult.Failed -> registration
                null -> null
            }

        private fun RegistrationCertificate?.toInteractingParty(
            fallbackName: MultiLangString?
        ): IssuerInteractingParty {
            if (this == null) return IssuerInteractingParty(fallbackName, null, null, null)
            val name = (name ?: legalName
                ?: listOfNotNull(givenName, familyName).joinToString(" ").ifBlank { null })
                ?.let { MultiLangString(lang = DEFAULT_LANG, content = it) }
                ?: fallbackName
            return IssuerInteractingParty(
                name = name,
                identifier = structuredIdentifier(),
                type = interactingPartyType(),
                contact = listOfNotNull(country, infoUri).ifEmpty { null },
            )
        }

        private fun RegistrationCertificate.interactingPartyType(): String? = when {
            IssuerEntitlements.PID in entitlements -> TYPE_PID
            IssuerEntitlements.QEAA in entitlements -> TYPE_QEAA
            IssuerEntitlements.PUB_EAA in entitlements -> TYPE_PUB_EAA
            IssuerEntitlements.NON_Q_EAA in entitlements -> TYPE_NON_Q_EAA
            else -> null
        }
    }
}

private class IssuerInteractingParty(
    val name: MultiLangString?,
    val identifier: QualifiedIdentifier?,
    val type: String?,
    val contact: List<String>?
)
