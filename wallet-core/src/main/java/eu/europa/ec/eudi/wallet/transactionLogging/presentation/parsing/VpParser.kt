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

package eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing

import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.recreateClaimsAndDisclosuresPerClaim
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.vc.SelectPath.Default.query
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedClaim
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedDocument
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.VPTokenConsensusJson
import eu.europa.ec.eudi.wallet.transfer.openId4vp.FORMAT_MSO_MDOC
import eu.europa.ec.eudi.wallet.transfer.openId4vp.FORMAT_SD_JWT_VC
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

/**
 * Function to parse the Verifiable Presentation (VP) response.
 * It takes the raw response as a byte array and metadata as a list of strings.
 * It returns a list of PresentedDocument objects.
 *
 * @param rawResponse the raw response to parse
 * @param metadata metadata associated with the documents
 * @return a list of PresentedDocument objects
 */
fun parseVp(
    rawResponse: ByteArray,
    metadata: List<String>,
): List<PresentedDocument> {
    val parsedMetadata = metadata.map { TransactionLog.Metadata.fromJson(it) }
    val vpToken =
        VPTokenConsensusJson.decodeFromString<Consensus.PositiveConsensus>(
            String(rawResponse)
        )

    val presentedDocuments = vpToken.verifiablePresentations.value
        .mapKeys { it.key.value }
        .flatMap { (queryId, vps) ->
            // metadata for queryId
            val queryMetadata = parsedMetadata
                .filter { md -> md.queryId == queryId }
                .sortedBy { it.index }

            // Support only VerifiablePresentation.Generic for now
            vps.filterIsInstance<VerifiablePresentation.Generic>()
                .mapIndexedNotNull { index, vp ->
                    // metadata for queryId and current index of document
                    queryMetadata.getOrNull(index)
                        ?.let { vpMetadata ->
                            when (vpMetadata.format) {
                                FORMAT_MSO_MDOC -> parseMsoMdocFromVp(vp, vpMetadata)
                                FORMAT_SD_JWT_VC -> parseVcSdJwt(vp, vpMetadata)
                                else -> null
                            }
                        }
                }

        }
    return presentedDocuments
}


/**
 * Parses an mso_mdoc document from a Verifiable Presentation.
 * This function extracts the mso_mdoc data from the verifiable presentation,
 * decodes it from Base64, and then passes it to the parseMsoMdoc function.
 *
 * @param vp The generic Verifiable Presentation containing the mso_mdoc document.
 * @param metadata List of metadata strings associated with the documents.
 * @return A PresentedDocument objects parsed from the mso_mdoc data.
 */
fun parseMsoMdocFromVp(
    vp: VerifiablePresentation.Generic,
    metadata: TransactionLog.Metadata,
): PresentedDocument? {

    // but first we need to decode the verifiable presentation
    val msoMdocResponse = Base64.getUrlDecoder().decode(vp.value)

    return parseMsoMdoc(
        rawResponse = msoMdocResponse,
        sessionTranscript = null,
        metadata = listOf(metadata.toJson())
    ).firstOrNull()
}

/**
 * Parses an SD-JWT Verifiable Credential from a Verifiable Presentation.
 * This function extracts the SD-JWT data from the verifiable presentation,
 * processes it to remove key binding due to library limitations, and
 * converts it into a PresentedDocument object with all the relevant claims.
 *
 * @param vp The generic Verifiable Presentation containing the SD-JWT VC.
 * @param metadata Optional metadata string associated with the document in JSON format.
 * @return A PresentedDocument object if parsing is successful, or null if parsing fails.
 */
fun parseVcSdJwt(
    vp: VerifiablePresentation.Generic,
    metadata: TransactionLog.Metadata,
): PresentedDocument? {
    // get the sd-jwt from the verifiable presentation but before that
    // we need to remove the key binding from it due to limitations of the
    // sd-jwt library and [eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.verify] method.
    // if we cannot get sd-jwt for the one and only verifiable presentation
    // we need to return empty list since there is nothing else to parse
    val sdJwt: SdJwt<JwtAndClaims> = getSdJwt(
        vp.valueWithoutKeyBinding
    ) ?: return null

    val presentedDocument = getPresentedDocumentsFromClaims(
        claims = sdJwt.claims,
        metadata = metadata.issuerMetadata?.let { IssuerMetadata.fromJson(it).getOrNull() }
    )

    return presentedDocument
}

/**
 * This function is used to remove the key binding from the sd-jwt
 * due to limitations of the sd-jwt library and [DefaultSdJwtOps.verify] method.
 */
val VerifiablePresentation.Generic.valueWithoutKeyBinding: String
    get() = value
        .split("~")
        .dropLast(1)
        .joinToString("~")
        .plus("~")

/**
 * Function to parse the sd-jwt string and return the SdJwt object
 * @param sdJwt the sd-jwt string to parse
 * @return the SdJwt object or null if the parsing fails
 */
fun getSdJwt(sdJwt: String): SdJwt<JwtAndClaims>? {
    return runBlocking {
        with(DefaultSdJwtOps) {
            verify(NoSignatureValidation, sdJwt).getOrNull()
        }
    }
}

/**
 * This extension property is used to get the claims from the sd-jwt object and get
 * a map of claims with the path as the key and the value as the value
 * It uses the [DefaultSdJwtOps.recreateClaimsAndDisclosuresPerClaim] method
 * and then selects the claims from the json object using the path
 */
val SdJwt<JwtAndClaims>.claims: Map<List<String>, JsonElement?>
    get() {
        val (jsonObject, claimPath) = with(DefaultSdJwtOps) {
            this@claims.recreateClaimsAndDisclosuresPerClaim()
        }
        return claimPath.keys.associate {
            it.value.toList().map(Any::toString) to jsonObject.query(it).getOrNull()?.toJsonElement()
        }
    }

/**
 * Function to get the presented documents from the claims
 * It takes the claims as a map of path to value and metadata
 * and returns a PresentedDocument object
 * @param claims the claims to parse
 * @param metadata the metadata to parse
 * @return the PresentedDocument object
 */
fun getPresentedDocumentsFromClaims(
    claims: Map<List<String>, JsonElement?>,
    metadata: IssuerMetadata?,
): PresentedDocument {
    // accumulate claims
    val presentedClaims = mutableListOf<PresentedClaim>()
    // hold vct if present
    var vct: String? = null
    // sort claims by descending order of path size. This is to ensure that leaves are processed first
    // for nested claims and we get a flatten list of claims
    // if a claim is already present in the list and we are processing a parent claim, we skip it
    // avoid having both parent and child claims in the list
    claims.toList().sortedByDescending { it.first.size }.forEach { (path, value) ->
        // check if claim or a child is already present in the list
        if (presentedClaims.none { it.path.take(path.size) == path }) {
            presentedClaims.add(
                PresentedClaim(
                    path = path,
                    value = value?.jsonPrimitive?.content,
                    rawValue = value.toString(),
                    metadata = findClaimMetadataForSdJwtVc(path, metadata),
                )
            )
        }
        // check if vct is present in the claim path
        // and if so, set it to the vct variable
        if (vct == null && path.first() == "vct") {
            vct = value?.jsonPrimitive?.content
        }
    }
    val presentedDocument = PresentedDocument(
        format = SdJwtVcFormat(vct = vct ?: ""),
        claims = presentedClaims,
        metadata = metadata
    )

    return presentedDocument
}


/**
 * Function to find the claim metadata from the path and metadata
 * It takes the path as a list of strings and metadata and iterates [IssuerMetadata.claims] to find
 * the claim metadata
 *
 * @param path the path to parse
 * @param metadata the metadata to parse
 * @return the claim metadata as a [IssuerMetadata.Claim] object
 */
fun findClaimMetadataForSdJwtVc(
    path: List<String>,
    metadata: IssuerMetadata?,
): IssuerMetadata.Claim? {
    return metadata?.claims?.find {
        it.path.size == path.size && it.path.zip(path).all { (a, b) -> a == b }
    }
}