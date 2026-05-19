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
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.openid4vp.Format
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.openid4vp.dcql.metaMsoMdoc
import eu.europa.ec.eudi.openid4vp.dcql.metaSdJwtVc
import eu.europa.ec.eudi.openid4vp.legalName
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.generateJarmNonce
import eu.europa.ec.eudi.wallet.internal.toRequesterAndTrust
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderTrust
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderTrustImpl
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ReaderTrustResult
import kotlinx.io.bytestring.decodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.multipaz.claim.Claim
import org.multipaz.claim.JsonClaim
import org.multipaz.claim.findMatchingClaim
import org.multipaz.openid.dcql.DcqlCredentialQuery
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialPresentmentData
import org.multipaz.presentment.CredentialPresentmentSetOptionMemberMatch
import org.multipaz.request.JsonRequestedClaim
import org.multipaz.request.Requester
import org.multipaz.request.RequestedClaim
import org.multipaz.sdjwt.SdJwt
import org.multipaz.sdjwt.credential.SdJwtVcCredential

/**
 * Processes OpenID4VP requests that use DCQL (Digital Credentials Query Language).
 *
 * For each [CredentialQuery] the processor:
 *  - finds candidate [IssuedDocument]s matching the requested format and type;
 *  - resolves the claims to disclose per the query's `claims` and `claim_sets` rules
 *    (DCQL §6.4.1 first-match semantics over `claim_sets`);
 *  - hands the resulting matches to [CredentialSetsMatcher] which applies
 *    `credential_sets` rules and produces the [CredentialPresentmentData] tree consumed
 *    by [ProcessedDcqlRequest] for the presentation step.
 *
 * @property documentManager Provides access to documents stored in the wallet.
 * @property openid4VpX509CertificateTrust Verifies trust in the reader's certificate.
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

    private val credentialSetsMatcher = CredentialSetsMatcher()

    override suspend fun process(request: Request): RequestProcessor.ProcessedRequest {
        return try {
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

            // Resolve trust verdict and build the Requester / TrustMetadata for the Success
            // payload. The legalName goes into TrustMetadata.displayName when the cert chain
            // validated against the configured ReaderTrustStore.
            val legalName = request.resolvedRequestObject.client.legalName()
            val trustResult = openid4VpX509CertificateTrust.result
            val (requester, trustMetadata) = when (trustResult) {
                is ReaderTrustResult.Processed -> trustResult.toRequesterAndTrust(legalName = legalName)
                ReaderTrustResult.Pending -> Requester(certChain = null) to null
            }

            // Find candidate matches for each credential query.
            val matchesByQueryId: Map<QueryId, List<CredentialPresentmentSetOptionMemberMatch>> =
                credentials.value.associate { query ->
                    query.id to findMatchesForQuery(query)
                }

            // Each query's `multiple` flag, forwarded to [ProcessedDcqlRequest]
            val multipleByQueryId: Map<QueryId, Boolean> = credentials.value
                .associate { query -> query.id to query.multipleOrDefault }

            // Apply credential_sets rules to produce the presentment tree.
            val sets = credentialSetsMatcher.toCredentialPresentmentSets(
                credentials = credentials,
                credentialSets = credentialSets,
                matchesByQueryId = matchesByQueryId,
            )

            ProcessedDcqlRequest(
                resolvedRequestObject = request.resolvedRequestObject,
                documentManager = documentManager,
                presentmentData = CredentialPresentmentData(sets),
                requester = requester,
                trustMetadata = trustMetadata,
                msoMdocNonce = generateJarmNonce(),
                multipleByQueryId = multipleByQueryId
            )
        } catch (e: Throwable) {
            RequestProcessor.ProcessedRequest.Failure(e)
        }
    }

    /**
     * Produces candidate [CredentialPresentmentSetOptionMemberMatch]es for a single
     * [CredentialQuery]. A document is included only when its credential satisfies the
     * verifier's claim requirements, with [resolveClaimsToDisclose] applying
     * `claim_sets` first-match semantics (DCQL §6.4.1).
     */
    private suspend fun findMatchesForQuery(
        query: CredentialQuery
    ): List<CredentialPresentmentSetOptionMemberMatch> {
        val dcqlQuery: DcqlCredentialQuery = query.toDcqlCredentialQuery()
        val candidates: List<IssuedDocument> = candidateDocumentsForQuery(query)

        return candidates.mapNotNull { issuedDoc ->
            val secureCred = issuedDoc.findCredential() ?: return@mapNotNull null

            // When the verifier requires a cryptographic holder binding proof, an
            // SD-JWT credential without a `cnf` claim cannot satisfy the request —
            // it has no key to sign the KB-JWT with. Skip such credentials so they
            // do not appear in the consent UI. mdoc credentials are not filtered;
            // they always carry a device key.
            if (query.requireCryptographicHolderBindingOrDefault) {
                when (query.format) {
                    Format.SdJwtVc -> {
                        if ((secureCred as? SdJwtVcCredential)?.hasCnfClaim() != true) {
                            return@mapNotNull null
                        }
                    }
                }
            }

            val credClaims: List<Claim> = runCatching {
                secureCred.getClaims(documentTypeRepository = null)
            }.getOrElse { return@mapNotNull null }

            // Resolve which subset of the verifier's `claims` this credential must
            // disclose, honouring `claim_sets` first-match semantics (§6.4.1).
            val matchedClaims: Map<RequestedClaim, Claim> =
                resolveClaimsToDisclose(dcqlQuery, credClaims) ?: return@mapNotNull null

            CredentialPresentmentSetOptionMemberMatch(
                credential = secureCred,
                claims = matchedClaims,
                source = CredentialMatchSourceOpenID4VP(credentialQuery = dcqlQuery),
                transactionData = emptyList(),
            )
        }
    }

    /**
     * Returns `true` when the issuer-signed JWT carries a `cnf` claim — the
     * prerequisite for producing a Key Binding JWT at presentation time. Returns
     * `false` when the issuer-signed JWT cannot be parsed.
     */
    private suspend fun SdJwtVcCredential.hasCnfClaim(): Boolean {
        return runCatching {
            SdJwt.fromCompactSerialization(this.issuerProvidedData.decodeToString())
                .kbKey != null
        }.getOrElse { false }
    }

    /**
     * Returns the claims a credential must disclose for [dcqlQuery], or `null` when
     * the credential cannot satisfy the query.
     *
     *  - With `claim_sets` (DCQL §6.4.1): iterate sets **in order** and return the first
     *    whose every referenced claim resolves against the credential. The returned map
     *    contains exactly that set's claims — nothing more.
     *
     *  - Without `claim_sets`: require every entry of `claims` to resolve. The returned
     *    map contains all of them.
     *
     * Per-claim resolution is delegated to [matchClaim].
     */
    private fun resolveClaimsToDisclose(
        dcqlQuery: DcqlCredentialQuery,
        credClaims: List<Claim>,
    ): Map<RequestedClaim, Claim>? {
        if (dcqlQuery.claimSets.isEmpty()) {
            // No alternatives — every claim must be present.
            val all = mutableMapOf<RequestedClaim, Claim>()
            for (req in dcqlQuery.claims) {
                val matched = matchClaim(req, credClaims) ?: return null
                all[req] = matched
            }
            return all
        }

        // First-match over claim_sets. A set is satisfied iff every claimId it references
        // resolves to a [RequestedClaim] that has a value in this credential. The id →
        // claim lookup is rebuilt locally because [DcqlCredentialQuery.claimIdToClaim] is
        // not visible from this module; [DcqlQueryAdapter] populates the same map under
        // the hood.
        val claimIdLookup: Map<String, RequestedClaim> = dcqlQuery.claims
            .mapNotNull { rc -> rc.id?.let { id -> id to rc } }
            .toMap()

        for (claimSet in dcqlQuery.claimSets) {
            val resolved = mutableMapOf<RequestedClaim, Claim>()
            var satisfied = true
            for (claimId in claimSet.claimIdentifiers) {
                val requested = claimIdLookup[claimId]
                if (requested == null) {
                    satisfied = false
                    break
                }
                val matched = matchClaim(requested, credClaims)
                if (matched == null) {
                    satisfied = false
                    break
                }
                resolved[requested] = matched
            }
            if (satisfied) return resolved
        }
        return null
    }

    /**
     * Resolves a single [RequestedClaim] against the credential's claims.
     *
     * The primary path delegates to [Claim.findMatchingClaim], which covers top-level
     * paths, nested object paths and explicit array-index paths. Falls back to
     * [matchClaimViaSpecCorrectNullWildcard] when the primary path returns `null` for
     * an SD-JWT VC request that combines a `null` wildcard in the claim path with a
     * `values` filter — that combination requires per-element evaluation of the
     * wildcard per OpenID4VP §7.1.
     */
    private fun matchClaim(req: RequestedClaim, credClaims: List<Claim>): Claim? {
        val direct = credClaims.findMatchingClaim(req)
        if (direct != null) return direct
        return matchClaimViaSpecCorrectNullWildcard(req, credClaims)
    }

    /**
     * Resolves a [JsonRequestedClaim] whose path contains a `null` wildcard and whose
     * request carries a `values` filter, per OpenID4VP §7.1 element-by-element semantics.
     *
     * Returns the credential's top-level claim (the one whose `claimPath[0]` matches the
     * request's `claimPath[0]`) when at least one resolved value passes the values
     * filter; otherwise `null`. Downstream code consumes only the request's path for
     * disclosure-path generation — the matched [Claim] value itself is not used — so
     * returning the root claim is sufficient to signal "satisfied".
     */
    private fun matchClaimViaSpecCorrectNullWildcard(
        req: RequestedClaim,
        credClaims: List<Claim>,
    ): Claim? {
        if (req !is JsonRequestedClaim) return null
        val values = req.values ?: return null
        val hasNullWildcard = req.claimPath.any { it is JsonNull }
        if (!hasNullWildcard) return null

        val topName = (req.claimPath.firstOrNull() as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?: return null
        val rootClaim = credClaims.filterIsInstance<JsonClaim>().firstOrNull { cc ->
            (cc.claimPath.firstOrNull() as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content == topName
        } ?: return null

        // Spec §7.1: a null wildcard expands the array into its elements; the result is a
        // set of values. Spec §6.4.1: claim is satisfied when at least one resulting value
        // matches any element of the `values` array.
        val resolved = descendClaimPath(rootClaim.value, req.claimPath.drop(1))
        return if (resolved.any { values.contains(it) }) rootClaim else null
    }

    /**
     * Walks a SD-JWT VC claim path per OpenID4VP §7.1 semantics, returning the **set** of
     * resolved values (each `null` element in the path expands into an iteration over
     * every element of the current array).
     *
     *  - String element → object key lookup (descends into array's objects when the
     *    current node is an array).
     *  - Integer element → array index access.
     *  - `null` element → "evaluate all elements" (the wildcard).
     *
     * Returns an empty list when the path cannot be resolved against the structure (e.g.
     * indexing a non-array, descending into a primitive). Empty list ↔ no match.
     */
    private fun descendClaimPath(
        start: JsonElement,
        remainingPath: List<JsonElement>,
    ): List<JsonElement> {
        if (remainingPath.isEmpty()) return listOf(start)
        val head = remainingPath.first()
        val tail = remainingPath.drop(1)
        return when {
            head is JsonNull -> {
                if (start is JsonArray) start.flatMap { descendClaimPath(it, tail) }
                else emptyList()
            }
            head is JsonPrimitive && head.isString -> when (start) {
                is JsonObject -> start[head.content]
                    ?.let { descendClaimPath(it, tail) }
                    ?: emptyList()
                is JsonArray -> start.flatMap { element ->
                    (element as? JsonObject)?.get(head.content)
                        ?.let { descendClaimPath(it, tail) }
                        ?: emptyList()
                }
                else -> emptyList()
            }
            head is JsonPrimitive && !head.isString -> {
                val index = head.content.toIntOrNull() ?: return emptyList()
                if (start is JsonArray && index in start.indices) {
                    descendClaimPath(start[index], tail)
                } else emptyList()
            }
            else -> emptyList()
        }
    }

    /**
     * Resolves the wallet's candidate [IssuedDocument]s for a [CredentialQuery]:
     *  - `mso_mdoc`: documents whose [MsoMdocFormat.docType] equals the query's
     *    `doctype_value`;
     *  - `dc+sd-jwt`: documents whose [SdJwtVcFormat.vct] is in the query's `vct_values`
     *    (exact match).
     *
     * Throws on unsupported formats.
     */
    private suspend fun candidateDocumentsForQuery(query: CredentialQuery): List<IssuedDocument> {
        return when (query.format) {
            Format.MsoMdoc -> {
                val docType = requireNotNull(query.metaMsoMdoc?.doctypeValue?.value) {
                    "DocType is missing for query with id ${query.id}"
                }
                findDocumentsByFormat(MsoMdocFormat(docType))
            }
            Format.SdJwtVc -> {
                val vctValues = query.metaSdJwtVc?.vctValues.orEmpty()
                require(vctValues.isNotEmpty()) {
                    "VctValues are missing or is empty for query with id ${query.id}"
                }
                vctValues.flatMap { findDocumentsByFormat(SdJwtVcFormat(it)) }
            }
            else -> throw IllegalArgumentException("Not supported format ${query.format.value}")
        }
    }

    private suspend fun findDocumentsByFormat(format: DocumentFormat): List<IssuedDocument> {
        return documentManager.getDocuments()
            .filter { it.format == format }
            .filterIsInstance<IssuedDocument>()
            .filter { it.findCredential() != null }
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
