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

import eu.europa.ec.eudi.iso18013.transfer.internal.cn
import eu.europa.ec.eudi.iso18013.transfer.internal.getValidIssuedMsoMdocDocuments
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.zkp.ZkResponsePolicy
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.claim.Claim
import org.multipaz.claim.findMatchingClaim
import org.multipaz.crypto.SignatureVerificationException
import org.multipaz.crypto.X509CertChain
import org.multipaz.crypto.fromJavaX509Certificates
import org.multipaz.crypto.javaX509Certificates
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.mdoc.request.DocRequest
import org.multipaz.mdoc.zkp.ZkSystemRepository
import org.multipaz.presentment.CredentialMatchSourceIso18013
import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSet
import org.multipaz.presentment.CredentialPresentmentSetOption
import org.multipaz.presentment.CredentialPresentmentSetOptionMember
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.RequestedClaim
import org.multipaz.request.Requester
import org.multipaz.trustmanagement.TrustMetadata
import kotlin.collections.component1
import kotlin.collections.component2
import org.multipaz.mdoc.request.DeviceRequest as MultipazDeviceRequest

/**
 * Implementation of [RequestProcessor] for [DeviceRequest] (ISO 18013-5).
 *
 * Builds a [CredentialPresentmentData] tree directly from the matched documents,
 * one [CredentialPresentmentSet] per parsed [DocRequest]. Each set has a single option /
 * single member containing all candidate credentials that have **at least one** of the
 * verifier's requested data elements (soft matching). Missing elements are simply omitted
 * from the resulting [CredentialPresentmentSetOptionMemberMatch.claims] map; the disclosure
 * phase will produce an IssuerSigned only for the elements that were actually matched and
 * confirmed by the user, in line with ISO 18013-5 partial-response semantics.
 *
 * @property documentManager the document manager to retrieve the requested documents
 * @property readerTrustStore the reader trust store to perform reader authentication
 * @property readerAuthPolicy the policy for enforcing reader authentication during response generation
 * @property zkSystemRepository the ZKP system repository
 * @property zkResponsePolicy the ZK response policy to use when ZK proof generation fails
 */
class DeviceRequestProcessor(
    private val documentManager: DocumentManager,
    override var readerTrustStore: ReaderTrustStore? = null,
    private val readerAuthPolicy: ReaderAuthPolicy = ReaderAuthPolicy.EnforceIfPresent,
    private var zkSystemRepository: ZkSystemRepository? = null,
    internal val zkResponsePolicy: ZkResponsePolicy = ZkResponsePolicy.FallbackToFullDisclosure,
) : RequestProcessor, ReaderTrustStoreAware {

    /**
     * Process the [DeviceRequest] and return [ProcessedDeviceRequest] (success) or
     * [RequestProcessor.ProcessedRequest.Failure] (parsing/matching error).
     */
    override suspend fun process(request: Request): RequestProcessor.ProcessedRequest {
        return try {
            require(request is DeviceRequest) { "Request must be a DeviceRequest" }

            val deviceRequestDataItem: DataItem = Cbor.decode(request.deviceRequestBytes)
            val sessionTranscriptDataItem: DataItem = Cbor.decode(request.sessionTranscriptBytes)
            val parsedRequest = MultipazDeviceRequest.fromDataItem(deviceRequestDataItem)

            // Verify reader signature.
            val signatureValid = try {
                parsedRequest.verifyReaderAuthentication(sessionTranscriptDataItem)
                true
            } catch (_: SignatureVerificationException) {
                false
            }

            // Validate the reader cert chain against the trust store, if any.
            val readerCertChain = parsedRequest.getRequester()?.javaX509Certificates ?: emptyList()
            val isTrusted = readerCertChain.isNotEmpty() &&
                    readerTrustStore?.validateCertificationTrustPath(readerCertChain) == true
                    && signatureValid

            val requester = Requester(
                certChain = if (readerCertChain.isNotEmpty()) {
                    X509CertChain.fromJavaX509Certificates(readerCertChain)
                } else null
            )
            val trustMetadata = if (isTrusted) {
                TrustMetadata(displayName = readerCertChain.cn.takeIf { it.isNotBlank() })
            } else null

            val credentialSets = parsedRequest.docRequests.mapNotNull { docRequest ->
                docRequest.toCredentialPresentmentSet(documentManager)
            }

            ProcessedDeviceRequest(
                documentManager = documentManager,
                sessionTranscript = request.sessionTranscriptBytes,
                presentmentData = CredentialPresentmentData(credentialSets),
                requester = requester,
                trustMetadata = trustMetadata,
                zkSystemRepository = zkSystemRepository,
                readerAuthPolicy = readerAuthPolicy,
                zkResponsePolicy = zkResponsePolicy
            )
        } catch (e: Throwable) {
            RequestProcessor.ProcessedRequest.Failure(e)
        }
    }
}

/**
 * Build a [CredentialPresentmentSet] from a single ISO 18013-5 [DocRequest] against the
 * wallet's [documentManager], using soft matching: a candidate credential is included if
 * it has **at least one** of the verifier's requested data elements. Missing elements are
 * simply omitted from [CredentialPresentmentSetOptionMemberMatch.claims]; the disclosure
 * phase only signs over what the wallet can actually disclose (ISO 18013-5 partial-response
 * semantics). Returns `null` if no candidate credential has any matching element.
 *
 * @param documentManager source of candidate wallet documents — only documents whose
 *   `format` matches `docType` are considered.
 */
internal suspend fun DocRequest.toCredentialPresentmentSet(
    documentManager: DocumentManager,
): CredentialPresentmentSet? {
    val docType = docType
    val requestedClaims: List<MdocRequestedClaim> = nameSpaces
        .flatMap { (namespace, elements) ->
            elements.map { (elementName, intentToRetain) ->
                MdocRequestedClaim(
                    docType = docType,
                    namespaceName = namespace,
                    dataElementName = elementName,
                    intentToRetain = intentToRetain
                )
            }
        }

    val candidates: List<IssuedDocument> =
        documentManager.getValidIssuedMsoMdocDocuments(docType)
    val matches = candidates.mapNotNull { issuedDoc ->
        val secureCred = issuedDoc.findCredential() ?: return@mapNotNull null
        if (secureCred !is MdocCredential) return@mapNotNull null

        val credClaims = runCatching { secureCred.getClaims(documentTypeRepository = null) }
            .getOrElse { return@mapNotNull null }
        val matchedClaims: MutableMap<RequestedClaim, Claim> = mutableMapOf()
        for (req in requestedClaims) {
            credClaims.findMatchingClaim(req)?.let { matchedClaims[req] = it }
        }
        // Skip the credential only if it has none of the requested elements.
        if (matchedClaims.isEmpty()) return@mapNotNull null

        CredentialPresentmentSetOptionMemberMatch(
            credential = secureCred,
            claims = matchedClaims,
            source = CredentialMatchSourceIso18013(docRequest = this),
            transactionData = emptyList()
        )
    }

    if (matches.isEmpty()) return null

    return CredentialPresentmentSet(
        optional = false,
        options = listOf(
            CredentialPresentmentSetOption(
                members = listOf(
                    CredentialPresentmentSetOptionMember(matches = matches)
                )
            )
        )
    )
}