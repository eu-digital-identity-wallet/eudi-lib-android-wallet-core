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

import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.Response
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.wallet.dcapi.DCAPIRequest
import eu.europa.ec.eudi.wallet.dcapi.IsoMdocDCAPIResponse
import eu.europa.ec.eudi.wallet.dcapi.OpenId4VpDCAPIResponse
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimInfo
import eu.europa.ec.eudi.wallet.transactionLogging.model.ClaimPath
import eu.europa.ec.eudi.wallet.transactionLogging.model.MultiLangString
import eu.europa.ec.eudi.wallet.transactionLogging.model.Policy
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionEntry
import eu.europa.ec.eudi.wallet.transactionLogging.model.TransactionResult
import eu.europa.ec.eudi.wallet.registration.structuredIdentifier
import eu.europa.ec.eudi.wallet.transactionLogging.producers.toNoncompletionReason
import eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation.parsing.parseMsoMdoc
import eu.europa.ec.eudi.wallet.transactionLogging.producers.presentation.parsing.parseVp
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpResponse
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.multipaz.presentment.CredentialMatchSource
import org.multipaz.presentment.CredentialMatchSourceIso18013
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.request.JsonRequestedClaim
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.RequestedClaim
import java.time.Instant
import java.util.UUID

/**
 * Builds [TransactionEntry.Presentation] entries from requests and responses. It records only claim
 * identifiers and paths, never raw bytes or attribute values.
 */
class PresentationLogBuilder {
    /** Creates an empty, pending entry (not completed, no reason) with a new id and the current time. */
    fun createEmptyPresentationLog(): TransactionEntry.Presentation = TransactionEntry.Presentation(
        transactionIdentifier = UUID.randomUUID().toString(),
        time = Instant.now(),
        transactionResult = TransactionResult.NotCompleted(),
        listOfClaimsRequested = emptyList(),
        listOfClaimsPresented = emptyList(),
    )

    /**
     * Adds the requested claims (TS10 §3.2) read from the [processedRequest]. Paths only, no values.
     * Unsupported or unprocessable requests mark the entry as not completed.
     *
     * @param processedRequest the processed request, holding the matched claims.
     */
    fun withRequest(
        log: TransactionEntry.Presentation,
        request: Request,
        processedRequest: RequestProcessor.ProcessedRequest,
    ): TransactionEntry.Presentation = when (request) {
        is DeviceRequest,
        is OpenId4VpRequest,
        is DCAPIRequest,
            -> {
            val success = processedRequest.getOrNull()
            if (success == null) {
                // The request itself could not be processed (e.g. unsupported feature, malformed
                // request) — record the underlying failure as the reason of non-completion.
                val failureReason = (processedRequest as? RequestProcessor.ProcessedRequest.Failure)
                    ?.error?.toNoncompletionReason(REASON_REQUEST_FAILED)
                log.copy(
                    transactionResult = TransactionResult.NotCompleted(
                        failureReason ?: REASON_REQUEST_FAILED
                    )
                )
            } else {
                // Take the claims from what the user was shown on the consent screen, de-duplicated
                // per credential (a match repeats across selection combinations).
                val matches = success.presentmentSelections
                    .flatMap { it.matches }
                    .distinctBy { it.offeredKey() }
                if (matches.isEmpty()) {
                    // The wallet holds no credential that can satisfy the request; record a clear reason.
                    log.copy(
                        transactionResult = TransactionResult.NotCompleted(
                            REASON_REQUEST_NOT_SATISFIABLE
                        )
                    )
                } else {
                    log.copy(listOfClaimsRequested = parseRequestedClaims(matches))
                }
            }
        }

        else -> log.copy(
            transactionResult = TransactionResult.NotCompleted("Unsupported request type")
        )
    }

    /**
     * Adds the relying party's identity. When the request carries a registration certificate, its
     * registered details — identifier, name, contact, purpose, privacy policy, registrar, supervisory
     * authority and intermediary — are recorded. Otherwise only the name is recorded, from the
     * request's trust metadata.
     */
    fun withRelyingParty(
        log: TransactionEntry.Presentation,
        processedRequest: RequestProcessor.ProcessedRequest,
    ): TransactionEntry.Presentation {
        val success = processedRequest.getOrNull() ?: return log
        val registration =
            when (val result = success.wrpRegistration as? RegistrationCertificateResult) {
                is RegistrationCertificateResult.Verified -> result.registration
                is RegistrationCertificateResult.Failed -> result.registration
                null -> null
            }
        return if (registration != null) {
            log.withRegistrationCertificate(
                rc = registration,
                fallbackName = success.trustMetadata?.displayName
            )
        } else {
            val name = success.trustMetadata?.displayName ?: "Unidentified Relying Party"
            log.copy(interactingPartyName = MultiLangString(lang = DEFAULT_LANG, content = name))
        }
    }

    /**
     * Copies the relying party's registered details onto the entry. Fields with no value are left
     * empty; the name falls back to [fallbackName] when the certificate carries none.
     */
    private fun TransactionEntry.Presentation.withRegistrationCertificate(
        rc: RegistrationCertificate,
        fallbackName: String?,
    ): TransactionEntry.Presentation {
        val name = rc.name ?: rc.legalName ?: fallbackName ?: "Unidentified Relying Party"
        val dpa = rc.supervisoryAuthority
        return copy(
            interactingPartyName = MultiLangString(lang = DEFAULT_LANG, content = name),
            interactingPartyIdentifier = rc.structuredIdentifier(),
            interactingPartyContact = listOfNotNull(rc.supportUri, rc.infoUri).ifEmpty { null },
            isIntermediary = rc.intermediary != null,
            intermediaryName = rc.intermediary?.name
                ?.let { MultiLangString(lang = DEFAULT_LANG, content = it) },
            registrarURL = rc.registryUri,
            purpose = rc.purpose
                .map { MultiLangString(lang = it.language, content = it.value) }
                .ifEmpty { null },
            privacyPolicy = listOfNotNull(
                rc.privacyPolicyUri?.let { Policy(type = Policy.PRIVACY_POLICY, policyURI = it) },
            ).ifEmpty { null },
            dpaName = dpa?.name?.let { MultiLangString(lang = DEFAULT_LANG, content = it) },
            dpaContact = dpa?.let { listOfNotNull(it.email, it.phone, it.uri) }?.ifEmpty { null },
        )
    }


    /**
     * Adds the presented claims from a [Response] (paths only, no values) and sets the
     * result: completed when [error] is null, otherwise not completed.
     *
     * @throws IllegalArgumentException if the response type is unsupported.
     */
    fun withResponse(
        log: TransactionEntry.Presentation,
        response: Response,
        error: Throwable? = null,
    ): TransactionEntry.Presentation = log.copy(
        listOfClaimsPresented = parsePresentedClaims(response),
        transactionResult = if (error == null) {
            TransactionResult.Completed
        } else {
            TransactionResult.NotCompleted(error.toNoncompletionReason(REASON_RESPONSE_ERROR))
        },
    )

    /**
     * Adds the presented claims (paths only) without deciding the result, leaving it
     * for a later transfer event. Used when the response is dispatched but the outcome is not yet known.
     */
    fun withPresentedClaims(
        log: TransactionEntry.Presentation,
        response: Response,
    ): TransactionEntry.Presentation =
        log.copy(listOfClaimsPresented = parsePresentedClaims(response))

    /** Marks the entry as completed, leaving the already-recorded presented claims untouched. */
    fun withCompleted(
        log: TransactionEntry.Presentation,
    ): TransactionEntry.Presentation =
        log.copy(transactionResult = TransactionResult.Completed)

    /**
     * Parses a [Response] into the presented claims (paths only).
     *
     * @throws IllegalArgumentException if the response type is unsupported.
     */
    private fun parsePresentedClaims(response: Response): List<ClaimInfo> = when (response) {
        is DeviceResponse -> parseMsoMdoc(
            rawResponse = response.deviceResponseBytes,
            sessionTranscript = response.sessionTranscriptBytes,
        )

        is OpenId4VpResponse -> parseVp(
            rawResponse = VPTokenConsensusJson.encodeToString(response.vpToken).toByteArray(),
        )

        is IsoMdocDCAPIResponse -> parseMsoMdoc(
            rawResponse = response.deviceResponseBytes,
            sessionTranscript = null,
        )

        is OpenId4VpDCAPIResponse -> parseVp(
            rawResponse = VPTokenConsensusJson.encodeToString(response.vpToken).toByteArray(),
        )

        else -> throw IllegalArgumentException(
            "Unsupported response type: ${response::class.simpleName}"
        )
    }

    /**
     * Marks the entry as not completed, with an optional reason (TS10 §3.1).
     *
     * @param reason a human-readable reason, or `null` if unknown.
     */
    fun withError(
        log: TransactionEntry.Presentation,
        reason: String? = null,
    ): TransactionEntry.Presentation =
        log.copy(transactionResult = TransactionResult.NotCompleted(reason))

    /**
     * Builds the requested claims (TS10 §3.2): one [ClaimInfo] per match. For each match it reads the
     * claims the wallet can satisfy and that the user was shown, not the full raw request. Paths only,
     * no values. A claim the wallet cannot satisfy produces no match and is left out.
     */
    private fun parseRequestedClaims(
        matches: List<CredentialPresentmentSetOptionMemberMatch>,
    ): List<ClaimInfo> = matches.mapNotNull { match ->
        val credentialId = match.source.credentialIdentifier() ?: return@mapNotNull null
        val paths = match.claims.keys.mapNotNull { it.toRequestedPath() }
        ClaimInfo(credentialIdentifier = credentialId, claims = paths)
    }

    /**
     * The credential identifier (mdoc `docType` / SD-JWT VC `vct`) of a match, or `null` if unknown.
     */
    private fun CredentialMatchSource.credentialIdentifier(): String? = when (this) {
        is CredentialMatchSourceIso18013 -> docRequest.docType
        is CredentialMatchSourceOpenID4VP ->
            credentialQuery.mdocDocType ?: credentialQuery.vctValues?.firstOrNull()
    }

    /** De-duplication key for a match: one entry per held credential per requested query. */
    private fun CredentialPresentmentSetOptionMemberMatch.offeredKey(): String {
        val sourceKey = when (val s = source) {
            is CredentialMatchSourceIso18013 -> s.docRequest.docType
            is CredentialMatchSourceOpenID4VP -> s.credentialQuery.id
        }
        return "${credential.document.identifier}|$sourceKey"
    }

    /**
     * Maps a [RequestedClaim] to its claim path (OpenID4VP §7), paths only:
     * mdoc becomes `[namespace, dataElement]`; SD-JWT VC uses its claim path. Returns `null` if malformed.
     */
    private fun RequestedClaim.toRequestedPath(): ClaimPath? = when (this) {
        is MdocRequestedClaim -> ClaimPath.ofKeys(namespaceName, dataElementName)
        is JsonRequestedClaim -> runCatching { claimPath.toClaimPath() }.getOrNull()
    }

    /**
     * Converts a raw OpenID4VP claim path [JsonArray] into a typed [ClaimPath]. Each element must be
     * a string, a non-negative integer, or `null`, and the array must be non-empty.
     */
    private fun JsonArray.toClaimPath(): ClaimPath {
        require(isNotEmpty()) { "ClaimPath must be non-empty (OpenID4VP §7)" }
        val segments = map { element ->
            when {
                element is JsonNull -> ClaimPath.Segment.Wildcard
                element is JsonPrimitive && element.isString -> ClaimPath.Segment.Key(element.content)
                element is JsonPrimitive -> {
                    val asInt = element.content.toIntOrNull()
                        ?: error("ClaimPath integer segment must fit in Int (got '${element.content}')")
                    ClaimPath.Segment.Index(asInt) // negativity rejected by Segment.Index.init
                }
                else -> error("ClaimPath segment must be string|integer|null (got $element)")
            }
        }
        return ClaimPath(segments)
    }

    companion object {
        private const val DEFAULT_LANG = "en"

        /** Reason recorded when the wallet holds no credential that can satisfy the request. */
        internal const val REASON_REQUEST_NOT_SATISFIABLE =
            "Request could not be satisfied — no matching credential available"

        /** Reason recorded when the processed request is a failure (request could not be processed). */
        internal const val REASON_REQUEST_FAILED = "Request could not be processed"

        /** Fallback reason when sending/parsing the response fails without a usable message. */
        internal const val REASON_RESPONSE_ERROR = "Failed to send the response"
    }
}
