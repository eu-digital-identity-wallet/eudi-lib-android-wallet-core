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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuth
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.openid4vp.PresentationQuery
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.legalName
import eu.europa.ec.eudi.prex.InputDescriptor
import eu.europa.ec.eudi.prex.InputDescriptorId
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.internal.generateMdocGeneratedNonce
import eu.europa.ec.eudi.wallet.internal.getSessionTranscriptBytes
import kotlinx.coroutines.runBlocking

/**
 * Processes OpenID4VP (OpenID for Verifiable Presentations) requests using Presentation Exchange.
 *
 * This processor implements the [RequestProcessor] interface to handle credential presentation
 * requests conforming to the OpenID4VP protocol specification. The processor supports multiple
 * document formats simultaneously, with special handling for:
 *
 * - MSO_MDOC: Mobile Security Object/Mobile Driving License format (ISO 18013-5)
 * - SD-JWT VC: Selective Disclosure JWT Verifiable Credentials format
 *
 * The processor analyzes presentation definition requirements from verifiers, matches them with
 * available credentials in the wallet, and prepares disclosure requests for user approval.
 * It integrates with the wallet's security architecture through reader authentication and
 * trust verification.
 *
 * @param documentManager Provides access to credentials stored in the wallet
 * @property openid4VpX509CertificateTrust Handles verification of relying party X.509 certificates
 */
class OpenId4VpRequestProcessor(
    private val documentManager: DocumentManager,
    var openid4VpX509CertificateTrust: OpenId4VpReaderTrust
) : RequestProcessor {

    // Reuse the DeviceRequestProcessor helper to avoid duplicating document retrieval logic
    private val helper: DeviceRequestProcessor.Helper by lazy {
        DeviceRequestProcessor.Helper(documentManager)
    }

    /**
     * Processes an OpenID4VP request and generates an appropriate response processor.
     *
     * This method determines the request type based on presentation requirements and produces
     * a specialized processor for handling the credential disclosure flow:
     *
     * 1. Validates the request structure and authorization format
     * 2. Extracts presentation definitions and identifies requested document formats
     * 3. For MSO_MDOC-only requests, creates a specialized processor optimized for ISO 18013-5
     * 4. For mixed format requests, creates a generic processor supporting multiple formats
     * 5. Maps requested credential fields to available documents in the wallet
     *
     * @param request The incoming OpenID4VP request to process
     * @return A [RequestProcessor.ProcessedRequest] containing matched documents and response generators
     * or [RequestProcessor.ProcessedRequest.Failure] if processing fails
     */
    override fun process(request: Request): RequestProcessor.ProcessedRequest {
        return try {
            // Validate request structure and type
            require(request is OpenId4VpRequest) { "Request must be an OpenId4VpRequest" }
            require(request.resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization) {
                "Request must have be a OpenId4VPAuthorization"
            }
            val presentationQuery = request.resolvedRequestObject.presentationQuery
            require(presentationQuery is PresentationQuery.ByPresentationDefinition) {
                "Currently only PresentationDefinition is supported"
            }

            val presentationDefinition = presentationQuery.value

            // Extract requested formats from all input descriptors
            val requestedFormats = presentationDefinition.inputDescriptors
                .flatMap { it.format?.jsonObject()?.keys ?: emptySet() }
                .toSet()
            require(requestedFormats.isNotEmpty()) { "No format is requested" }
            val msoMdocNonce: String = generateMdocGeneratedNonce()

            // Optimization: Use specialized processor for MSO_MDOC-only requests
            val requestedOnlyMsoMdoc =
                requestedFormats.size == 1 && requestedFormats.first() == FORMAT_MSO_MDOC

            if (requestedOnlyMsoMdoc) {
                // Process the request as an MSO_MDOC-only request for better performance
                // and ISO 18013-5 compliance
                val requestedDocuments: RequestedDocuments = runBlocking {
                    presentationDefinition.inputDescriptors
                        .map { descriptor ->
                            parseDescriptorForMsoMdocDocument(
                                descriptor,
                                request
                            )
                        }
                        .let { helper.getRequestedDocuments(it) }
                }

                val msoMdocNonce = generateMdocGeneratedNonce()
                // Create session transcript for device engagement according to ISO 18013-5
                val sessionTranscriptBytes =
                    request.resolvedRequestObject.getSessionTranscriptBytes(msoMdocNonce)

                ProcessedMsoMdocOpenId4VpRequest(
                    resolvedRequestObject = request.resolvedRequestObject,
                    processedDeviceRequest = ProcessedDeviceRequest(
                        documentManager = documentManager,
                        requestedDocuments = requestedDocuments,
                        sessionTranscript = sessionTranscriptBytes
                    ),
                    msoMdocNonce = msoMdocNonce
                )
            } else {
                // Handle mixed format requests or SD-JWT VC only requests
                // Track which documents match each input descriptor for response generation
                val inputDescriptorMap: MutableMap<InputDescriptorId, List<DocumentId>> =
                    mutableMapOf()

                val requestedDocuments = RequestedDocuments(
                    presentationDefinition.inputDescriptors
                        // Filter to only process supported formats
                        .filter {
                            it.format?.jsonObject()?.keys?.first() in listOf(
                                FORMAT_MSO_MDOC, FORMAT_SD_JWT_VC
                            )
                        }
                        .flatMap { inputDescriptor ->
                            // Process each input descriptor based on its requested format
                            // Note: Currently only one format per input descriptor is supported
                            when (inputDescriptor.format?.jsonObject()?.keys?.first()) {
                                FORMAT_MSO_MDOC -> runBlocking {
                                    // Process MSO_MDOC format requests
                                    parseDescriptorForMsoMdocDocument(inputDescriptor, request)
                                        .let { helper.getRequestedDocuments(listOf(it)) }
                                        .also { requestedDoc ->
                                            // Track which document IDs match this input descriptor
                                            inputDescriptorMap[inputDescriptor.id] =
                                                requestedDoc.map { it.documentId }.toList()
                                        }
                                }

                                FORMAT_SD_JWT_VC -> runBlocking {
                                    // Process SD-JWT VC format requests
                                    parseDescriptorForSdJwtVcDocument(inputDescriptor, request)
                                        .also { requestedDoc ->
                                            // Track which document IDs match this input descriptor
                                            inputDescriptorMap[inputDescriptor.id] =
                                                requestedDoc.map { it.documentId }.toList()
                                        }
                                }

                                else -> throw IllegalArgumentException("Unsupported format ${inputDescriptor.format}")
                            }
                        }.toList()
                )
                ProcessedGenericOpenId4VpRequest(
                    documentManager = documentManager,
                    resolvedRequestObject = request.resolvedRequestObject,
                    inputDescriptorMap = inputDescriptorMap,
                    requestedDocuments = requestedDocuments,
                    msoMdocNonce = msoMdocNonce
                )
            }
        } catch (e: Throwable) {
            RequestProcessor.ProcessedRequest.Failure(e)
        }
    }

    /**
     * Parses an input descriptor for MSO_MDOC format document requests.
     *
     * This method extracts namespace and element identifier pairs from the input descriptor's
     * constraint field paths, using regular expressions to parse JSONPath syntax. It creates
     * a mapping of namespaces to their requested element identifiers with retention flags.
     *
     * @param descriptor The input descriptor containing document type and path constraints
     * @param request The original OpenID4VP request (used for reader authentication)
     * @return A [DeviceRequestProcessor.RequestedMdocDocument] containing parsed request parameters
     */
    private fun parseDescriptorForMsoMdocDocument(
        descriptor: InputDescriptor,
        request: OpenId4VpRequest,
    ): DeviceRequestProcessor.RequestedMdocDocument {
        // Extract namespace and element identifiers from constraint field paths
        // Example path format: $['driving_privileges']['status']
        val requested = descriptor.constraints.fields()
            .mapNotNull { fields ->
                val path = fields.paths.first().value

                // Parse the JSONPath to extract namespace and element identifier
                // Format is expected to be $['namespace']['element']
                Regex("\\\$\\['(.*?)']\\['(.*?)']").find(path)
                    ?.destructured
                    ?.takeIf { (nameSpace, elementIdentifier) -> nameSpace.isNotBlank() && elementIdentifier.isNotBlank() }
                    ?.let { (nameSpace, elementIdentifier) -> nameSpace to elementIdentifier }

            }
            // Group by namespace and create mapping to element identifiers
            // Each element identifier is associated with a boolean for intentToRetain (default: false)
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, elementIdentifiers) -> elementIdentifiers.associateWith { false } }

        return DeviceRequestProcessor.RequestedMdocDocument(
            docType = descriptor.id.value.trim(),
            requested = requested,
            readerAuthentication = { getReaderAuthResult(request) }
        )
    }

    /**
     * Parses an input descriptor for SD-JWT VC format document requests.
     *
     * This method extracts the Verifiable Credential Type (vct) and requested claims
     * from the input descriptor constraints. It then retrieves matching documents
     * from the wallet and configures the disclosure request with appropriate reader
     * authentication.
     *
     * @param descriptor The input descriptor containing vct and claim path constraints
     * @param request The original OpenID4VP request (used for reader authentication)
     * @return A [RequestedDocuments] collection containing matched documents and claims
     * @throws IllegalArgumentException if vct parameter is missing in the input descriptor
     */
    private suspend fun parseDescriptorForSdJwtVcDocument(
        descriptor: InputDescriptor,
        request: OpenId4VpRequest,
    ): RequestedDocuments {
        // Extract the credential type (vct) from the constraints
        // The vct is required to identify which SD-JWT VCs match this request
        val vct = descriptor.constraints.fields().find { field ->
            field.paths.first().value == "$.vct"
        }?.filter?.jsonObject()?.getValue("const")?.toString()
            ?.removeSurrounding("\"")
            ?: throw IllegalArgumentException("vct not found")

        // Extract requested claims from constraints, excluding the vct field
        // Claims are parsed from JSONPath expressions like $.claim or $.claim.subclaim
        val requestedClaims = descriptor.constraints.fields()
            .filter { it.paths.first().value != "$.vct" }
            .mapNotNull { fieldConstraint ->
                val path = fieldConstraint.paths.first().value
                // Parse JSONPath to extract claim path segments
                // Currently only simple paths are supported (no array indexing or wildcards)
                Regex("""^\$\.(\w+(?:\.\w+)*)$""").matchEntire(path)
                    ?.groupValues?.get(1)?.split(".")?.let {
                        // Create item with path segments and retention flag
                        SdJwtVcItem(it) to (fieldConstraint.intentToRetain ?: false)
                    }
            }.toMap()

        // Retrieve all documents matching the vct and create request objects
        return RequestedDocuments(
            documents = documentManager.getValidIssuedSdJwtVcDocuments(vct)
                .map { doc ->
                    RequestedDocument(
                        documentId = doc.id,
                        requestedItems = requestedClaims,
                        readerAuth = getReaderAuthResult(request)
                    )
                }
        )
    }

    /**
     * Creates a ReaderAuth object based on the certificate trust verification result.
     *
     * This method extracts the certificate chain and trust status from the X.509 certificate
     * verification process, combines it with the relying party's legal name, and constructs
     * a ReaderAuth object for inclusion in the presentation response.
     *
     * @param request The OpenID4VP request containing client (relying party) information
     * @return A [ReaderAuth] object with certificate chain and verification details, or null if unavailable
     */
    private fun getReaderAuthResult(request: OpenId4VpRequest): ReaderAuth? {
        return (openid4VpX509CertificateTrust.result as? ReaderTrustResult.Processed)
            ?.let { (chain, isTrusted) ->
                val readerCommonName =
                    request.resolvedRequestObject.client.legalName() ?: ""
                ReaderAuth(
                    readerAuth = ByteArray(0), // No raw auth bytes in OpenID4VP
                    readerSignIsValid = true,  // Signature verification happens at JWT level
                    readerCertificatedIsTrusted = isTrusted,
                    readerCertificateChain = chain,
                    readerCommonName = readerCommonName
                )
            }
    }
}
