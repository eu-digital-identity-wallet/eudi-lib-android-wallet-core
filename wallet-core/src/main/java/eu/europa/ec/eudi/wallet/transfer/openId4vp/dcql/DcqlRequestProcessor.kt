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

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreAware
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuth
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.openid4vp.Format
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.openid4vp.dcql.ClaimsQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.fold
import eu.europa.ec.eudi.openid4vp.dcql.metaMsoMdoc
import eu.europa.ec.eudi.openid4vp.dcql.metaSdJwtVc
import eu.europa.ec.eudi.openid4vp.legalName
import eu.europa.ec.eudi.wallet.document.format.DocumentClaim
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.generateJarmNonce
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderTrust
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderTrustImpl
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ReaderTrustResult
import eu.europa.ec.eudi.wallet.transfer.openId4vp.SdJwtVcItem
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Processes OpenID4VP requests using DCQL (Digital Credentials Query Language).
 *
 * This processor validates and processes credential presentation requests using the DCQL format.
 * It matches credential requirements against locally available documents in the wallet, handling
 * multiple document formats:
 *
 * - MSO mdoc (ISO 18013-5 mobile driving license format)
 * - SD-JWT VC (Selective Disclosure JSON Web Token Verifiable Credentials)
 *
 * The processor verifies the request validity, extracts the required credentials, and matches
 * them with documents in the wallet that satisfy the requirements.
 *
 * @property documentManager Provides access to documents stored in the wallet
 * @property openid4VpX509CertificateTrust Verifies trust in the reader's certificate for secure exchange
 */
class DcqlRequestProcessor(
    private val documentManager: DocumentManager,
    var openid4VpX509CertificateTrust: OpenId4VpReaderTrust,
) : RequestProcessor, ReaderTrustStoreAware {

    /**
     * The trust store used for verifying reader certificates.
     */
    override var readerTrustStore: ReaderTrustStore?
        get() = openid4VpX509CertificateTrust.readerTrustStore
        set(value) {
            openid4VpX509CertificateTrust.readerTrustStore = value
        }

    /**
     * Processes an OpenID4VP request containing DCQL queries.
     *
     * This method performs the following steps:
     * 1. Validates the request is a properly formatted OpenID4VP request with DCQL
     * 2. Extracts reader authentication information
     * 3. Processes each credential request in the query
     * 4. Identifies matching documents in the wallet for each request
     * 5. Maps requested claims to the document items
     *
     * Important! Currently credentials_sets and claim_sets are not supported.
     *
     * @param request The incoming presentation request
     * @return [ProcessedDcqlRequest] containing matched documents and requested items
     */
    override fun process(request: Request): RequestProcessor.ProcessedRequest {
        try {
            // Validate request type and structure
            require(request is OpenId4VpRequest) { "Request must be an OpenId4VpRequest" }

            // Temporarily reject all requests with transaction data (not yet supported)
            val requestTransactionData = request.resolvedRequestObject.transactionData
            if (!requestTransactionData.isNullOrEmpty()) {
                return RequestProcessor.ProcessedRequest.Failure(
                    IllegalArgumentException("Transaction data is not supported")
                )
            }

            val dcql = request.resolvedRequestObject.query
            val credentials = dcql.credentials
            val credentialSets = dcql.credentialSets

            val readerCommonName = request.resolvedRequestObject.client.legalName() ?: ""
            // Extract reader authentication/trust result if available
            // This creates a ReaderAuth object from the trust result to include with requested documents
            val readerAuth = (openid4VpX509CertificateTrust.result as? ReaderTrustResult.Processed)
                ?.let { (chain, isTrusted) ->
                    ReaderAuth(
                        readerAuth = ByteArray(0),
                        readerSignIsValid = true,
                        readerCertificatedIsTrusted = isTrusted,
                        readerCertificateChain = chain,
                        readerCommonName = readerCommonName
                    )
                }

            // Process each credential in the query and match to available documents
            val potentialMatchesMap = credentials.value
                .associate { query ->
                    when (val format = query.format) {
                        Format.MsoMdoc -> {
                            // Handle MSO mdoc format credentials
                            val docTypeValue = query.metaMsoMdoc?.doctypeValue
                            requireNotNull(docTypeValue) {
                                "DocType is missing for query with id ${query.id}"
                            }
                            val docType = docTypeValue.value
                            // Find all documents that match the requested docType
                            val requestedDocuments =
                                getRequestedMsoMdocDocuments(docType, query, readerAuth)

                            query.id to RequestedDocumentsByFormat(
                                format = format.value,
                                requestedDocuments = requestedDocuments
                            )
                        }

                        Format.SdJwtVc -> {
                            // Handle SD-JWT VC format credentials
                            val vctValues = query.metaSdJwtVc!!.vctValues
                            require(vctValues.isNotEmpty()) {
                                "VctValues are missing or is empty for query with id ${query.id}"
                            }
                            val requestedDocuments =
                                getSdJwtVcRequestedDocuments(query, vctValues, readerAuth)

                            query.id to RequestedDocumentsByFormat(
                                format = format.value,
                                requestedDocuments = requestedDocuments
                            )
                        }

                        else -> throw IllegalArgumentException("Not supported format ${format.value}")
                    }
                }

            // Get the IDs of credentials that were actually found in the wallet.
            val availableWalletCredentialIds = potentialMatchesMap
                .filterValues { it.requestedDocuments.isNotEmpty() }
                .keys

            // Instantiate the processor and use it to determine the final set of credential IDs
            // that satisfy the credential_sets rules.
            val processor = CredentialSetsMatcher()
            val processedCredentials = processor.determineRequestedDocuments(
                credentials = credentials,
                credentialSets = credentialSets,
                availableWalletCredentialIds = availableWalletCredentialIds
            )

            // Filter the potential matches to create the final selection of documents for presentation.
            // This step executes the decision made by the DcqlProcessor. If the processor returned an empty map (because a
            // 'required' credential set could not be satisfied), this filter will also produce an empty
            // map, ensuring no documents are returned.
            val queryRequestedDocumentsMap = potentialMatchesMap
                .filterKeys { it in processedCredentials.keys }

            // Create and return the processed request with all matched documents and a generated nonce
            return ProcessedDcqlRequest(
                resolvedRequestObject = request.resolvedRequestObject,
                documentManager = documentManager,
                queryMap = queryRequestedDocumentsMap,
                msoMdocNonce = generateJarmNonce() // Generate a random nonce
            )
        } catch (e: Throwable) {
            return RequestProcessor.ProcessedRequest.Failure(e)
        }
    }

    /**
     * Processes SD-JWT VC format credential requests and finds matching documents.
     *
     * This method takes a DCQL credential query containing SD-JWT VC format requirements and:
     * 1. Extracts requested claims and their retention flags from the query
     * 2. Finds all wallet documents matching any of the provided VCT (Verifiable Credential Type) values
     * 3. For each matching document, maps either the specific requested claims or all available claims
     *    if none were explicitly requested
     *
     * When no claims are specified in the query, the method automatically includes all available
     * claims from the matched documents by traversing their claim hierarchy.
     *
     * @param query The credential query containing SD-JWT VC format requirements and requested claims
     * @param vctValues List of Verifiable Credential Type values to match against wallet documents
     * @param readerAuth Optional reader authentication information to include with the documents
     * @return [RequestedDocuments] collection containing all matching documents with their claims
     */
    private fun getSdJwtVcRequestedDocuments(
        query: CredentialQuery,
        vctValues: List<String>,
        readerAuth: ReaderAuth?,
    ): RequestedDocuments {
        // Map requested claims to SdJwtVcItems, preserving the typed ClaimPath
        val requestedItems = query.claims?.associate { claim ->
            SdJwtVcItem(path = claim.path) to (claim.intentToRetain == true)
        }

        // Find all documents that match any of the requested vctValues
        val allDocuments = runBlocking {
            vctValues.flatMap {
                findDocumentsByFormat(SdJwtVcFormat(it))
            }
        }

        // Filter documents by values constraints if any claims have values specified
        val documents = if (query.claims?.any { it.values != null } == true) {
            allDocuments.filter { document ->
                query.claims!!.all { claim ->
                    matchesClaimValues(document.data.claims, claim, isMsoMdoc = false)
                }
            }
        } else {
            allDocuments
        }

        val requestedDocuments = RequestedDocuments(documents.map { document ->
            RequestedDocument(
                documentId = document.id,
                // If no claims are specified, use all available claims in the document
                requestedItems = requestedItems ?: (getAllClaimPathsFrom(
                    claims = document.data.claims.filterIsInstance<SdJwtVcClaim>(),
                    rootPath = emptyList()
                ).associate { path ->
                    SdJwtVcItem(
                        ClaimPath(path.map { ClaimPathElement.Claim(it) })
                    ) to false
                }),
                readerAuth = readerAuth
            )
        })
        return requestedDocuments
    }

    /**
     * Processes MSO_MDOC format credential requests and finds matching documents.
     *
     * This method takes a DCQL credential query containing MSO_MDOC format requirements and:
     * 1. Finds all wallet documents matching the requested document type
     * 2. Extracts requested namespace and element identifier pairs from the query
     * 3. For each matching document, maps either the specific requested claims or all available claims
     *    if none were explicitly requested
     *
     * When no claims are specified in the query, the method automatically includes all
     * MsoMdocClaim elements from the matched documents.
     *
     * @param docType The document type identifier to match (e.g., "org.iso.18013.5.1.mDL")
     * @param query The credential query containing requested claim paths
     * @param readerAuth Optional reader authentication information to include with the documents
     * @return [RequestedDocuments] collection containing all matching documents with their claims
     */
    private fun getRequestedMsoMdocDocuments(
        docType: String,
        query: CredentialQuery,
        readerAuth: ReaderAuth?,
    ): RequestedDocuments {
        val documents =
            runBlocking { findDocumentsByFormat(format = MsoMdocFormat(docType)) }

        // Map requested claims to MsoMdocItems
        // MsoMdoc DCQL paths have at least 2 Claim elements: [namespace, elementIdentifier, ...]
        // Additional path elements (e.g., null for array traversal) are used for values matching.
        val requestedItems = query.claims?.associate { claim ->
            val namespace = (claim.path.value[0] as ClaimPathElement.Claim).name
            val elementIdentifier = (claim.path.value[1] as ClaimPathElement.Claim).name
            MsoMdocItem(
                namespace = namespace,
                elementIdentifier = elementIdentifier
            ) to (claim.intentToRetain == true)
        }

        // Filter documents by values constraints if any claims have values specified
        val filteredDocuments = if (query.claims?.any { it.values != null } == true) {
            documents.filter { document ->
                query.claims!!.all { claim ->
                    matchesClaimValues(document.data.claims, claim, isMsoMdoc = true)
                }
            }
        } else {
            documents
        }

        val requestedDocuments = RequestedDocuments(filteredDocuments.map { document ->
            RequestedDocument(
                documentId = document.id,
                requestedItems = requestedItems ?: document.data.claims
                    .filterIsInstance<MsoMdocClaim>()
                    .associate {
                        MsoMdocItem(
                            namespace = it.nameSpace,
                            elementIdentifier = it.identifier
                        ) to false
                    },
                readerAuth = readerAuth
            )
        })
        return requestedDocuments
    }

    /**
     * Finds all issued documents matching the specified document format.
     *
     * @param format The document format to match (e.g., MsoMdocFormat, SdJwtVcFormat)
     * @return List of [IssuedDocument]s matching the format and containing valid credentials
     */
    private suspend fun findDocumentsByFormat(format: DocumentFormat): List<IssuedDocument> {
        return documentManager.getDocuments()
            .filter { it.format == format }
            .filterIsInstance<IssuedDocument>()
            .filter { it.findCredential() != null }
    }

    /**
     * Returns all claim paths from a hierarchy of SdJwtVcClaims, starting from a given root path.
     *
     * This is used to determine all available claims in an SD-JWT VC document when no specific
     * claims are requested, allowing the presentation of all claims.
     *
     * @param claims The list of SdJwtVcClaim objects to search through
     * @param rootPath The path to the root claim from which to find children (empty list for root)
     * @return List of paths (where each path is a list of identifiers) to all child claims, excluding the rootPath itself
     */
    private fun getAllClaimPathsFrom(
        claims: List<SdJwtVcClaim>,
        rootPath: List<String>,
    ): List<List<String>> {
        // If the path is empty, return all paths from the root
        if (rootPath.isEmpty()) {
            return collectAllPaths(claims, emptyList())
        }

        // Find the claim at the rootPath
        val rootClaim = findClaimAtPath(claims, rootPath)

        // If the root claim is not found, return an empty list
        return rootClaim?.let {
            // Get all paths including the rootPath
            val allPaths = collectAllPaths(listOf(it), rootPath.dropLast(1))
            // Filter out the rootPath itself
            allPaths.filterNot { path -> path == rootPath }
        } ?: emptyList()
    }

    /**
     * Recursively collects all paths from the given SdJwtVcClaims.
     *
     * This traverses the hierarchical structure of SD-JWT VC claims to build complete paths
     * to each claim in the structure.
     *
     * @param claims The SdJwtVcClaim objects to process
     * @param basePath The base path prefix for each claim
     * @return List of all complete claim paths
     */
    private fun collectAllPaths(
        claims: List<SdJwtVcClaim>,
        basePath: List<String>,
    ): List<List<String>> {
        val result = mutableListOf<List<String>>()

        for (claim in claims) {
            val currentPath = basePath + claim.identifier
            result.add(currentPath)

            // Recursively process child claims if they exist
            if (claim.children.isNotEmpty()) {
                result.addAll(collectAllPaths(claim.children, currentPath))
            }
        }

        return result
    }

    /**
     * Finds a specific SdJwtVcClaim at the specified path in a claim hierarchy.
     *
     * @param claims The list of SdJwtVcClaim objects to search through
     * @param path The path to the target claim
     * @return The SdJwtVcClaim at the path, or null if not found
     */
    private fun findClaimAtPath(claims: List<SdJwtVcClaim>, path: List<String>): SdJwtVcClaim? {
        if (path.isEmpty()) return null

        var currentClaims = claims
        var targetClaim: SdJwtVcClaim? = null

        // Traverse the claim hierarchy following the path segments
        for (i in path.indices) {
            val segment = path[i]
            targetClaim = currentClaims.find { it.identifier == segment }

            if (targetClaim == null) {
                return null
            }

            if (i < path.size - 1) {
                currentClaims = targetClaim.children
            }
        }

        return targetClaim
    }

    /**
     * Checks if a document's claims satisfy a DCQL claim query's values constraint.
     *
     * If the claim query has no values constraint, returns true (no filtering needed).
     * Otherwise, resolves the claim value at the given path and checks if it matches
     * any of the requested values.
     *
     * For paths containing [ClaimPathElement.AllArrayElements] (null), all array elements
     * are checked — if ANY element matches ANY requested value, the constraint is satisfied.
     *
     * @param claims The document's claims
     * @param claimQuery The DCQL claim query with path and optional values
     * @param isMsoMdoc Whether the document is MsoMdoc format
     * @return true if the values constraint is satisfied or absent
     */
    private fun matchesClaimValues(
        claims: List<DocumentClaim>,
        claimQuery: ClaimsQuery,
        isMsoMdoc: Boolean,
    ): Boolean {
        val requestedValues = claimQuery.values ?: return true // No values constraint

        // Resolve the claim value at the path
        val claimValue = if (isMsoMdoc) {
            val namespace = (claimQuery.path.value[0] as ClaimPathElement.Claim).name
            val elementId = (claimQuery.path.value[1] as ClaimPathElement.Claim).name
            val claim = claims.filterIsInstance<MsoMdocClaim>()
                .find { it.nameSpace == namespace && it.identifier == elementId }
                ?: return false
            // If path has more than 2 elements, navigate deeper into the value
            val remainingPath = claimQuery.path.value.drop(2)
            if (remainingPath.isEmpty()) claim.value else resolveValueAtPath(claim.value, remainingPath)
        } else {
            // SD-JWT VC: navigate the claim hierarchy
            val sdJwtClaims = claims.filterIsInstance<SdJwtVcClaim>()
            resolveValueFromSdJwtClaims(sdJwtClaims, claimQuery.path.value)
        }

        // Check if the resolved value matches any of the requested values
        return valueMatchesAny(claimValue, requestedValues)
    }

    /**
     * Resolves a value by navigating through an object/array structure following path elements.
     *
     * @param value The current value to navigate into
     * @param path The remaining path elements to follow
     * @return The resolved value(s), or a List of values when a wildcard expands to multiple results
     */
    private fun resolveValueAtPath(value: Any?, path: List<ClaimPathElement>): Any? {
        if (path.isEmpty() || value == null) return value

        val head = path.first()
        val tail = path.drop(1)

        return head.fold(
            ifClaim = { name ->
                when (value) {
                    is Map<*, *> -> resolveValueAtPath(value[name], tail)
                    else -> null
                }
            },
            ifArrayElement = { index ->
                when (value) {
                    is List<*> -> if (index < value.size) resolveValueAtPath(value[index], tail) else null
                    else -> null
                }
            },
            ifAllArrayElements = {
                when (value) {
                    is List<*> -> value.mapNotNull { element -> resolveValueAtPath(element, tail) }
                    else -> null
                }
            }
        )
    }

    /**
     * Resolves a value from SD-JWT VC claims following the typed ClaimPath.
     * Navigates the SdJwtVcClaim hierarchy using Claim elements, then switches
     * to value-based navigation for ArrayElement/AllArrayElements.
     */
    private fun resolveValueFromSdJwtClaims(
        claims: List<SdJwtVcClaim>,
        path: List<ClaimPathElement>,
    ): Any? {
        if (path.isEmpty()) return null

        val head = path.first()
        val tail = path.drop(1)

        return head.fold(
            ifClaim = { name ->
                val claim = claims.find { it.identifier == name } ?: return null
                if (tail.isEmpty()) {
                    claim.value
                } else if (claim.children.isNotEmpty() && tail.first() is ClaimPathElement.Claim) {
                    // Continue navigating the claim hierarchy
                    resolveValueFromSdJwtClaims(claim.children, tail)
                } else {
                    // Switch to value-based navigation (for array/wildcard elements)
                    resolveValueAtPath(claim.value, tail)
                }
            },
            ifArrayElement = { _ -> null }, // Array element cannot be first
            ifAllArrayElements = { null }   // Wildcard cannot be first
        )
    }

    /**
     * Checks if a resolved value matches any of the requested JSON values.
     *
     * Handles:
     * - Direct primitive comparison (String, Number, Boolean)
     * - List of values from wildcard expansion (any element matching is sufficient)
     */
    private fun valueMatchesAny(resolvedValue: Any?, requestedValues: JsonArray): Boolean {
        if (resolvedValue == null) return false

        // If the resolved value is a list (from AllArrayElements expansion),
        // check if ANY element matches ANY requested value
        if (resolvedValue is List<*>) {
            return resolvedValue.any { element ->
                element != null && valueMatchesAny(element, requestedValues)
            }
        }

        // Compare the primitive value against each requested value
        return requestedValues.any { jsonValue ->
            if (jsonValue !is JsonPrimitive) return@any false
            when (resolvedValue) {
                is String -> jsonValue.isString && jsonValue.content == resolvedValue
                is Int -> jsonValue.intOrNull == resolvedValue
                is Long -> jsonValue.longOrNull == resolvedValue
                is Double -> jsonValue.doubleOrNull == resolvedValue
                is Float -> jsonValue.doubleOrNull?.toFloat() == resolvedValue
                is Boolean -> jsonValue.booleanOrNull == resolvedValue
                is Number -> jsonValue.doubleOrNull == resolvedValue.toDouble()
                else -> resolvedValue.toString() == jsonValue.content
            }
        }
    }

    companion object {
        operator fun invoke(
            documentManager: DocumentManager,
            readerTrustStore: ReaderTrustStore?,
        ): DcqlRequestProcessor {
            val openId4VpReaderTrust = OpenId4VpReaderTrustImpl(
                readerTrustStore = readerTrustStore
            )
            return DcqlRequestProcessor(
                documentManager = documentManager,
                openid4VpX509CertificateTrust = openId4VpReaderTrust,
            )
        }
    }
}
