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
import eu.europa.ec.eudi.iso18013.transfer.response.EU_WRPRC_REQUEST_INFO_KEY
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.iso18013.transfer.response.WrpRegistrationValidator
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.zkp.ZkResponsePolicy
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import kotlinx.coroutines.CancellationException
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.claim.Claim
import org.multipaz.claim.findMatchingClaim
import org.multipaz.cose.Cose
import org.multipaz.cose.toCoseLabel
import org.multipaz.crypto.javaX509Certificates
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.mdoc.request.DocRequest
import org.multipaz.mdoc.zkp.ZkSystemRepository
import org.multipaz.presentment.CredentialMatchSourceIso18013
import org.multipaz.presentment.CredentialQueryResult
import org.multipaz.presentment.CredentialPresentmentSet
import org.multipaz.presentment.CredentialPresentmentSetOption
import org.multipaz.presentment.CredentialPresentmentSetOptionMember
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.request.Iso18013RequesterIdentity
import org.multipaz.request.MdocRequestedClaim
import org.multipaz.request.RequestedClaim
import org.multipaz.request.Requester
import org.multipaz.request.RequesterIdentity
import org.multipaz.trustmanagement.TrustMetadata
import kotlin.collections.component1
import kotlin.collections.component2
import org.multipaz.mdoc.request.DeviceRequest as MultipazDeviceRequest

/**
 * Implementation of [RequestProcessor] for [DeviceRequest] (ISO 18013-5).
 *
 * Builds a [CredentialQueryResult] tree directly from the matched documents,
 * one [CredentialPresentmentSet] per parsed [DocRequest]. Each set has a single option /
 * single member containing all candidate credentials that have **at least one** of the
 * verifier's requested data elements (soft matching). Missing elements are simply omitted
 * from the resulting [CredentialPresentmentSetOptionMemberMatch.claims] map; the disclosure
 * phase will produce an IssuerSigned only for the elements that were actually matched and
 * confirmed by the user, in line with ISO 18013-5 partial-response semantics.
 *
 * @property documentManager the document manager to retrieve the requested documents
 * @property readerAuthPolicy the policy for enforcing reader authentication during response generation.
 *   The trust store used for certificate chain validation is embedded in the policy.
 * @property zkSystemRepository the ZKP system repository
 * @property zkResponsePolicy the ZK response policy to use when ZK proof generation fails
 * @property wrpRegistrationValidator validates the relying party registration certificate carried in
 *   the request's `euWrprc` `requestInfo`; when null, registration is not validated
 */
class DeviceRequestProcessor(
    private val documentManager: DocumentManager,
    private val readerAuthPolicy: ReaderAuthPolicy,
    private var zkSystemRepository: ZkSystemRepository? = null,
    internal val zkResponsePolicy: ZkResponsePolicy = ZkResponsePolicy.Strict,
    private val wrpRegistrationValidator: WrpRegistrationValidator? = null,
) : RequestProcessor {

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

            // Verify every reader signature. A signature that does not verify fails processing with
            // its SignatureVerificationException: no information from the request's reader
            // authentication is used, and there is no fallback to its other signatures.
            parsedRequest.verifyReaderAuthentication(sessionTranscriptDataItem)

            val readerAuthentication = authenticateReader(parsedRequest)
            // The display name and the registration-certificate access chain come from the first
            // trusted identity. With nothing trusted we still report the first identity, so that
            // the access chain remains visible to the registration validator.
            val readerCertChain =
                (readerAuthentication.trustedIdentity ?: readerAuthentication.identities.firstOrNull())
                    ?.certChain?.javaX509Certificates ?: emptyList()

            val requester = Requester(requesterIdentities = readerAuthentication.identities)
            val trustMetadata = if (readerAuthentication.verified) {
                TrustMetadata(displayName = readerCertChain.cn.takeIf { it.isNotBlank() })
            } else null

            val credentialSets = parsedRequest.docRequests.mapNotNull { docRequest ->
                docRequest.toCredentialPresentmentSet(documentManager)
            }

            // Validate the relying party registration certificate carried in the request, when a
            // validator is configured. It is repeated in each ItemsRequest.requestInfo (ETSI TS 119
            // 472-2 clause 5.3.2), so identical copies collapse to one; two different certificates
            // are ambiguous and rejected.
            val wrpRegistration = wrpRegistrationValidator?.let { validator ->
                val certificates = parsedRequest.docRequests
                    .mapNotNull { it.docRequestInfo?.otherInfo?.get(EU_WRPRC_REQUEST_INFO_KEY) }
                    .mapNotNull { runCatching { it.asBstr }.getOrNull() }
                    .distinctBy { it.toList() }
                require(certificates.size <= 1) {
                    "Device Request carries multiple relying party registration certificates"
                }
                validator.validate(
                    registrationCertificate = certificates.firstOrNull(),
                    readerAccessChain = readerCertChain,
                    requestedDocuments = parsedRequest.docRequests.map { it.toRequestedDocument() },
                )
            }

            ProcessedDeviceRequest(
                documentManager = documentManager,
                sessionTranscript = request.sessionTranscriptBytes,
                presentmentData = CredentialQueryResult(credentialSets),
                requester = requester,
                trustMetadata = trustMetadata,
                zkSystemRepository = zkSystemRepository,
                readerAuthPolicy = readerAuthPolicy,
                readerAuthFailed = readerAuthentication.failed,
                zkResponsePolicy = zkResponsePolicy,
                wrpRegistration = wrpRegistration
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            RequestProcessor.ProcessedRequest.Failure(e)
        }
    }

    /**
     * Resolves the reader identities of [request] and whether its reader authentication is
     * verified or failed.
     *
     * - With `readerAuthAll`, each signature covers the whole request, so the request is verified
     *   when at least one of these signatures is trusted. The `readerAuth` of each DocRequest is
     *   then only checked for validity.
     * - Without `readerAuthAll`, each `readerAuth` covers only its own DocRequest, so the request is
     *   verified only when every DocRequest is signed and every signer is trusted. Reader
     *   authentication fails when any signer is not trusted.
     *
     * @param request the parsed request, after all its reader signatures verified
     */
    private fun authenticateReader(request: MultipazDeviceRequest): ReaderAuthentication {
        if (request.readerAuthAll.isNotEmpty()) {
            // Several readerAuthAll signatures are identities of the same reader, typically one per
            // trust framework it is registered in, so one trusted signature is enough. This follows
            // the rationale of OpenID4VP 1.0 Appendix A.3.2.2 for the equivalent JWS JSON
            // Serialization ("multiple Client Identifiers ... different trust frameworks").
            val identities = request.getRequesterIdentities()
            val trustedIdentity = identities.firstOrNull { isTrusted(it) }
            return ReaderAuthentication(
                identities = identities,
                trustedIdentity = trustedIdentity,
                verified = trustedIdentity != null,
                failed = trustedIdentity == null,
            )
        }
        val signerChains = request.docRequests.map { docRequest ->
            docRequest.readerAuth?.let { readerAuth ->
                requireNotNull(
                    readerAuth.protectedHeaders[Cose.COSE_LABEL_X5CHAIN.toCoseLabel]
                        ?: readerAuth.unprotectedHeaders[Cose.COSE_LABEL_X5CHAIN.toCoseLabel]
                ) { "DocRequest readerAuth without x5chain" }.asX509CertChain
            }
        }
        val identities = signerChains.filterNotNull().distinct()
            .map { Iso18013RequesterIdentity(certChain = it) }
        val trustedIdentities = identities.filter { isTrusted(it) }
        val allSignersTrusted = trustedIdentities.size == identities.size
        return ReaderAuthentication(
            identities = identities,
            trustedIdentity = trustedIdentities.firstOrNull(),
            verified = identities.isNotEmpty() && signerChains.all { it != null } && allSignersTrusted,
            failed = !allSignersTrusted,
        )
    }

    private fun isTrusted(identity: RequesterIdentity): Boolean =
        readerAuthPolicy.readerTrustStore
            ?.validateCertificationTrustPath(identity.certChain.javaX509Certificates) == true
}

/**
 * Outcome of reader authentication for one request.
 *
 * @property identities the reader identities, one for each distinct signer
 * @property trustedIdentity the first identity that is trusted, if any
 * @property verified whether the whole request is covered by trusted reader authentication
 * @property failed whether a signer is not trusted
 */
private class ReaderAuthentication(
    val identities: List<RequesterIdentity>,
    val trustedIdentity: RequesterIdentity?,
    val verified: Boolean,
    val failed: Boolean,
)

/**
 * Projects a single ISO 18013-5 [DocRequest] onto the [RequestedDocument] used to check the request
 * against the relying party's registered scope.
 */
internal fun DocRequest.toRequestedDocument(): RequestedDocument =
    RequestedDocument(
        docType = docType,
        nameSpaces = nameSpaces.mapValues { (_, elements) -> elements.keys }
    )

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

        val credClaims = try {
            secureCred.getClaims(documentTypeRepository = null)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            return@mapNotNull null
        }
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