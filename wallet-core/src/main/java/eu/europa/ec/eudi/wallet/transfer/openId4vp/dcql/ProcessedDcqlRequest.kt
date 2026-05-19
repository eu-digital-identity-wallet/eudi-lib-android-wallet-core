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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql

import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VerifiablePresentations
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.internal.getSessionTranscriptBytes
import eu.europa.ec.eudi.wallet.internal.verifiablePresentationForMsoMdoc
import eu.europa.ec.eudi.wallet.internal.verifiablePresentationForSdJwtVc
import eu.europa.ec.eudi.wallet.internal.requireIssuedDocument
import eu.europa.ec.eudi.wallet.transfer.openId4vp.FORMAT_MSO_MDOC
import eu.europa.ec.eudi.wallet.transfer.openId4vp.FORMAT_SD_JWT_VC
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpResponse
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.request.Requester
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.trustmanagement.TrustMetadata

/**
 * Implementation of [RequestProcessor.ProcessedRequest.Success] for DCQL OpenID4VP flows.
 *
 * Holds the [CredentialPresentmentData] tree produced by [DcqlRequestProcessor] together
 * with the verifier's [Requester] and [TrustMetadata]. [generateResponse] takes the
 * user's [CredentialPresentmentSelection] and emits an [OpenId4VpResponse] containing
 * one [VerifiablePresentation] per match, grouped by the originating credential query.
 *
 * @property resolvedRequestObject the parsed OpenID4VP authorization request — used by
 *   the SD-JWT VC presentation builder (client id, nonce) and for session transcript
 *   derivation in MSO mdoc presentations.
 * @property documentManager bridges the credential identifier back to the wallet's
 *   [eu.europa.ec.eudi.wallet.document.IssuedDocument].
 * @property msoMdocNonce nonce used for both MSO mdoc handover binding and JARM
 *   encryption.
 * @property multipleByQueryId the `multiple` flag for each query. Drives
 *   [presentmentSelections]: when `multiple = false`, each candidate credential becomes
 *   its own option; when `multiple = true`, all candidates of the query are grouped into
 *   one option.
 */
class ProcessedDcqlRequest(
    val resolvedRequestObject: ResolvedRequestObject,
    private val documentManager: DocumentManager,
    presentmentData: CredentialPresentmentData,
    requester: Requester,
    trustMetadata: TrustMetadata?,
    val msoMdocNonce: String,
    private val multipleByQueryId: Map<QueryId, Boolean> = emptyMap()
) : RequestProcessor.ProcessedRequest.Success(
    presentmentData = presentmentData,
    requester = requester,
    trustMetadata = trustMetadata
) {

    /**
     * The options the user can choose from. For a query with `multiple = false` (the
     * default), each candidate credential becomes its own option; for `multiple = true`,
     * all candidates of the query are grouped into one option. Falls back to the default
     * behaviour when no per-query flags were supplied.
     */
    override val presentmentSelections: List<CredentialPresentmentSelection> by lazy {
        if (multipleByQueryId.isEmpty()) {
            super.presentmentSelections
        } else {
            buildMultipleAwareSelections(presentmentData, multipleByQueryId)
        }
    }

    /**
     * Generates an [OpenId4VpResponse] with one [VerifiablePresentation] per selected
     * match.
     *
     * Matches are grouped by their originating credential query's id (projected to a
     * [QueryId] for [OpenId4VpResponse.respondedDocuments]). Format selection
     * (`mso_mdoc` vs `dc+sd-jwt`) is driven by the source query's format, so a single
     * selection can mix formats across queries.
     *
     * Per-credential [keyUnlockData] is keyed by `match.credential.identifier`.
     */
    override suspend fun generateResponse(
        selection: CredentialPresentmentSelection,
        keyUnlockData: Map<String, KeyUnlockData>
    ): ResponseResult {
        return try {
            val verifiablePresentationsMap =
                mutableMapOf<QueryId, MutableList<VerifiablePresentation>>()
            val respondedDocumentsMap =
                mutableMapOf<QueryId, MutableList<OpenId4VpResponse.RespondedDocument>>()

            for (match in selection.matches) {
                val source = match.source as? CredentialMatchSourceOpenID4VP ?: continue
                val dcqlQuery = source.credentialQuery
                val queryId = QueryId(dcqlQuery.id)
                val format = dcqlQuery.format

                val vp = when (format) {
                    FORMAT_MSO_MDOC -> verifiablePresentationForMsoMdoc(
                        match = match,
                        documentManager = documentManager,
                        sessionTranscript = resolvedRequestObject.getSessionTranscriptBytes(),
                        keyUnlockData = keyUnlockData[match.credential.identifier]
                    )

                    FORMAT_SD_JWT_VC -> verifiablePresentationForSdJwtVc(
                        resolvedRequestObject = resolvedRequestObject,
                        match = match,
                        documentManager = documentManager,
                        keyUnlockData = keyUnlockData[match.credential.identifier]
                    )

                    else -> throw IllegalArgumentException("Unsupported format: $format")
                }

                val responseFormat = if (format == FORMAT_SD_JWT_VC) FORMAT_SD_JWT_VC else FORMAT_MSO_MDOC
                val issuedDocument = match.credential.requireIssuedDocument(documentManager)

                verifiablePresentationsMap.getOrPut(queryId) { mutableListOf() }.add(vp)
                respondedDocumentsMap.getOrPut(queryId) { mutableListOf() }.add(
                    OpenId4VpResponse.RespondedDocument(
                        documentId = issuedDocument.id,
                        format = responseFormat,
                    )
                )
            }

            val verifiablePresentations = VerifiablePresentations(
                verifiablePresentationsMap.mapValues { it.value.toList() }
            )
            val response = OpenId4VpResponse(
                resolvedRequestObject = resolvedRequestObject,
                vpToken = Consensus.PositiveConsensus(verifiablePresentations),
                msoMdocNonce = msoMdocNonce,
                respondedDocuments = respondedDocumentsMap.mapValues { it.value.toList() }
            )
            ResponseResult.Success(response)
        } catch (e: Exception) {
            ResponseResult.Failure(e)
        }
    }
}