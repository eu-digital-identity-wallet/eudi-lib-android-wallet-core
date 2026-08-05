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

package eu.europa.ec.eudi.iso18013.transfer.response.device

import eu.europa.ec.eudi.iso18013.transfer.internal.DocumentResponseGenerator.generateDocumentResponse
import eu.europa.ec.eudi.iso18013.transfer.internal.DocumentResponseGenerator.generateDocumentResponseWithoutConsuming
import eu.europa.ec.eudi.iso18013.transfer.internal.assertAgeOverRequestLimitForIso18013
import eu.europa.ec.eudi.iso18013.transfer.internal.flattenToSingleSelection
import eu.europa.ec.eudi.iso18013.transfer.internal.requireIssuedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.zkp.MatchedZkSystem
import eu.europa.ec.eudi.iso18013.transfer.zkp.ZkResponsePolicy
import eu.europa.ec.eudi.iso18013.transfer.zkp.matchZkSystem
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import kotlinx.coroutines.CancellationException
import org.multipaz.cbor.Cbor
import org.multipaz.mdoc.response.DeviceResponseGenerator
import org.multipaz.mdoc.response.MdocDocument
import org.multipaz.mdoc.zkp.ZkSystemRepository
import org.multipaz.presentment.CredentialMatchSourceIso18013
import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.Requester
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.trustmanagement.TrustMetadata
import org.multipaz.util.Constants

/**
 * Implementation of [RequestProcessor.ProcessedRequest.Success] for ISO 18013-5 device requests.
 *
 * Holds the parsed request state — the [CredentialPresentmentData] tree plus the
 * resolved [Requester] and optional [TrustMetadata] — and produces a signed `DeviceResponse`
 * for the user-confirmed [CredentialPresentmentSelection] via [generateResponse].
 *
 * `trustMetadata != null` means that the requester is trusted.
 *
 * @property documentManager document manager used to resolve the [IssuedDocument] for
 *   each selected match (see [requireIssuedDocument]).
 * @property sessionTranscript the session transcript bytes from the engagement; threaded
 *   through to per-document signing and returned in the final [DeviceResponse].
 * @property readerAuthPolicy decides whether [generateResponse] short-circuits to an empty
 *   `STATUS_GENERAL_ERROR` response when the reader isn't trust-verified. See
 *   [ReaderAuthPolicy] for the individual modes.
 * @property zkSystemRepository optional ZKP system repository — when set, the response
 *   generation will attempt to produce a ZK proof for any [CredentialMatchSourceIso18013]
 *   carrying a `zkRequest` on its [docRequest][CredentialMatchSourceIso18013.docRequest].
 * @property zkResponsePolicy what to do if ZK proof generation fails after a compatible
 *   system was matched — strict failure vs. fallback to a regular full-disclosure document.
 */
class ProcessedDeviceRequest(
    private val documentManager: DocumentManager,
    private val sessionTranscript: ByteArray,
    presentmentData: CredentialPresentmentData,
    requester: Requester,
    trustMetadata: TrustMetadata?,
    private val readerAuthPolicy: ReaderAuthPolicy = ReaderAuthPolicy.EnforceIfPresent,
    private val zkSystemRepository: ZkSystemRepository? = null,
    private val zkResponsePolicy: ZkResponsePolicy = ZkResponsePolicy.Strict
) : RequestProcessor.ProcessedRequest.Success(
    presentmentData = presentmentData,
    requester = requester,
    trustMetadata = trustMetadata
) {

    /**
     * One option containing every credential the wallet has for the request. The consent
     * UI shows them on a single screen; the user can select which credentials to share
     * before [generateResponse] is called.
     */
    override val presentmentSelections: List<CredentialPresentmentSelection> by lazy {
        listOf(presentmentData.flattenToSingleSelection())
    }

    /**
     * Generate the device response for the user-confirmed [selection].
     *
     * Behaviour:
     *  - The configured [readerAuthPolicy] is evaluated first. If it dictates that the
     *    response must be skipped (e.g. [ReaderAuthPolicy.AlwaysRequire] with no verified
     *    reader trust), an empty [DeviceResponse] with `STATUS_GENERAL_ERROR` is returned
     *    and no documents are signed.
     *  - Otherwise, each match in [selection] is processed:
     *    - the source must be [CredentialMatchSourceIso18013]; other sources are skipped silently;
     *    - the `Credential` is resolved back to its [IssuedDocument];
     *    - per-credential unlock data is looked up in [keyUnlockData] keyed by
     *      `match.credential.identifier`;
     *    - if the verifier requested a ZK proof and a compatible system spec is available,
     *      a ZK document is added — falling back to a regular response according to
     *      [zkResponsePolicy] when proof generation fails.
     *
     * @param selection the user-confirmed [CredentialPresentmentSelection]; each match's
     *   `claims` map is expected to already reflect the user's disclosure choice.
     * @param keyUnlockData per-credential unlock data, keyed by `match.credential.identifier`.
     *   Empty when no credential keys require unlocking.
     * @return a [ResponseResult.Success] wrapping the signed [DeviceResponse], or
     *   [ResponseResult.Failure] if any per-match generation throws.
     */
    override suspend fun generateResponse(
        selection: CredentialPresentmentSelection,
        keyUnlockData: Map<String, KeyUnlockData>
    ): ResponseResult {
        return try {

            val isReaderTrustVerified = trustMetadata != null
            val readerAuthPresent = requester.certChain != null
            val skipAllByPolicy = when (readerAuthPolicy) {
                ReaderAuthPolicy.DoNotEnforce -> false
                ReaderAuthPolicy.EnforceIfPresent -> readerAuthPresent && !isReaderTrustVerified
                ReaderAuthPolicy.AlwaysRequire -> !isReaderTrustVerified
            }

            if (skipAllByPolicy) {
                return ResponseResult.Success(
                    DeviceResponse(
                        deviceResponseBytes = DeviceResponseGenerator(Constants.DEVICE_RESPONSE_STATUS_GENERAL_ERROR)
                            .generate(),
                        sessionTranscriptBytes = sessionTranscript,
                        documentIds = emptyList()
                    )
                )
            }

            val deviceResponseGenerator =
                DeviceResponseGenerator(Constants.DEVICE_RESPONSE_STATUS_OK)
            val documentIds = mutableListOf<DocumentId>()

            for (match in selection.matches) {
                val source = match.source as? CredentialMatchSourceIso18013 ?: continue
                val docRequest = source.docRequest

                val issuedDocument = match.credential.requireIssuedDocument(documentManager)
                issuedDocument.assertAgeOverRequestLimitForIso18013(match.claims.keys)

                val elements = match.claims.keys
                    .filterIsInstance<MdocRequestedClaim>()
                    .groupBy { it.namespaceName }
                    .mapValues { (_, claims) -> claims.map { it.dataElementName } }

                val matchedZkSystem = matchZkSystem(
                    zkSystemRepository = zkSystemRepository,
                    docRequest = docRequest,
                    disclosedClaims = match.claims.keys
                )

                if (matchedZkSystem == null) {
                    addDocumentResponse(
                        issuedDocument = issuedDocument,
                        elements = elements,
                        keyUnlockData = keyUnlockData[match.credential.identifier],
                        deviceResponseGenerator = deviceResponseGenerator
                    )
                } else {
                    addZkDocumentResponse(
                        issuedDocument = issuedDocument,
                        elements = elements,
                        keyUnlockData = keyUnlockData[match.credential.identifier],
                        matchedZkSystem = matchedZkSystem,
                        deviceResponseGenerator = deviceResponseGenerator
                    )
                }
                documentIds.add(issuedDocument.id)
            }

            ResponseResult.Success(
                DeviceResponse(
                    deviceResponseBytes = deviceResponseGenerator.generate(),
                    sessionTranscriptBytes = sessionTranscript,
                    documentIds = documentIds
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResponseResult.Failure(e)
        }
    }

    private suspend fun addDocumentResponse(
        issuedDocument: IssuedDocument,
        elements: Map<String, List<String>>,
        keyUnlockData: KeyUnlockData?,
        deviceResponseGenerator: DeviceResponseGenerator
    ) {
        val encodedDocument = issuedDocument.generateDocumentResponse(
            transcript = sessionTranscript,
            elements = elements,
            keyUnlockData = keyUnlockData
        ).getOrThrow()
        deviceResponseGenerator.addDocument(encodedDocument)
    }

    /**
     * Generate a ZK proof using the credential without consuming it (proofs don't reveal the
     * device key, so they shouldn't count against credential usage limits).
     *
     * On proof failure behaviour follows [zkResponsePolicy]:
     *  - [ZkResponsePolicy.Strict]: rethrows, becoming a [ResponseResult.Failure];
     *  - [ZkResponsePolicy.FallbackToFullDisclosure]: falls back to [addDocumentResponse].
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    private suspend fun addZkDocumentResponse(
        issuedDocument: IssuedDocument,
        elements: Map<String, List<String>>,
        keyUnlockData: KeyUnlockData?,
        matchedZkSystem: MatchedZkSystem,
        deviceResponseGenerator: DeviceResponseGenerator
    ) {
        val encodedDocument = issuedDocument.generateDocumentResponseWithoutConsuming(
            transcript = sessionTranscript,
            elements = elements,
            keyUnlockData = keyUnlockData
        ).getOrThrow()

        val zkResult = runCatching {
            matchedZkSystem.system.generateProof(
                zkSystemSpec = matchedZkSystem.spec,
                document = MdocDocument.fromDataItem(Cbor.decode(encodedDocument)),
                sessionTranscript = Cbor.decode(sessionTranscript)
            )
        }

        if (zkResult.isSuccess) {
            deviceResponseGenerator.addZkDocument(zkResult.getOrThrow())
        } else when (zkResponsePolicy) {
            ZkResponsePolicy.Strict -> throw zkResult.exceptionOrNull()!!
            ZkResponsePolicy.FallbackToFullDisclosure ->
                addDocumentResponse(
                    issuedDocument = issuedDocument,
                    elements = elements,
                    keyUnlockData = keyUnlockData,
                    deviceResponseGenerator = deviceResponseGenerator
                )
        }
    }
}