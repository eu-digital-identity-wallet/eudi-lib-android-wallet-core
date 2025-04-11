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

import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.prex.DescriptorMap
import eu.europa.ec.eudi.prex.PresentationSubmission
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.recreateClaimsAndDisclosuresPerClaim
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.vc.SelectPath.Default.select
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.metadata.DocumentMetaData
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedClaim
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedDocument
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

typealias VpAndMetadata = Pair<VerifiablePresentation.Generic, String?>

/**
 * Function to parse the Verifiable Presentation (VP) response.
 * It takes the raw response as a byte array and metadata as a list of strings.
 * It returns a list of PresentedDocument objects.
 *
 * @param rawResponse the raw response to parse
 * @param metadata optional metadata associated with the documents
 * @return a list of PresentedDocument objects
 */
fun parseVp(
    rawResponse: ByteArray,
    metadata: List<String?>?
): List<PresentedDocument> {

    val parsedResponse = parseRawResponse(rawResponse) ?: return emptyList()

    val (verifiablePresentations, presentationSubmission) = parsedResponse

    if (presentationSubmission.descriptorMaps.size == 1) {
        // this means that the response either contains on mso_mdoc response
        // or one sd-jwt-vc document

        val descriptorMap = presentationSubmission.descriptorMaps.first()
        val verifiablePresentation = verifiablePresentations.first()
        when (descriptorMap.format) {
            "mso_mdoc" -> {
                // this means that the response contains one mso_mdoc response
                // we need to parse it and eagerly return the result
                return parseMsoMdocFromVp(vp = verifiablePresentation, metadata = metadata)
            }

            "vc+sd-jwt" -> {
                // if we cannot get sd-jwt for the one and only verifiable presentation
                // we need to return empty list since there is nothing else to parse
                return parseVcSdJwt(
                    vp = verifiablePresentation,
                    // since we have only one document metadata should contain only one element
                    metadata = metadata?.firstOrNull()
                )
                    ?.let { listOf(it) }
                    ?: emptyList()
            }

            else -> {
                // Not supported format
                // eagerly return empty list
                return emptyList()
            }
        }

    } else {
        // this means that the response contains more than one document
        // we need to separate mso_mdoc and sd-jwt-vc documents
        // and parse them separately
        // at the end we need to merge the results

        // first we get the indices for each document from descriptor maps
        val indicesByFormat = presentationSubmission.getResponseIndicesByFormat()

        val verifiablePresentationsAndMetadataByFormat: Map<String, List<VpAndMetadata>> =
            indicesByFormat.mapValues { (_, indices) ->
                indices.map { Pair(verifiablePresentations[it], metadata?.getOrNull(it)) }
            }

        val presentedDocuments =
            verifiablePresentationsAndMetadataByFormat.flatMap { (format, vpAndMetadata) ->
                when (format) {
                    "mso_mdoc" -> vpAndMetadata.flatMap { (vp, metadata) ->
                        parseMsoMdocFromVp(vp, listOf(metadata))
                    }

                    "vc+sd-jwt" -> vpAndMetadata.map { (vp, metadata) ->
                        parseVcSdJwt(vp, metadata)
                    }

                    else -> emptyList()
                }
            }.filterNotNull()

        return presentedDocuments
    }
}


/**
 * Function to parse the raw response of Vp.
 * It takes the raw response as a byte array and returns a pair of verifiable presentations and presentation submission.
 * @param rawResponse the raw response to parse
 * @return a pair of verifiable presentations and presentation submission or null if parsing fails
 */
fun parseRawResponse(rawResponse: ByteArray): Pair<List<VerifiablePresentation.Generic>, PresentationSubmission>? {
    val decodedResponse = Json.decodeFromString<JsonObject>(String(rawResponse))
    // get the verifiable presentations from the decoded response or return null
    val verifiablePresentations: List<VerifiablePresentation.Generic> =
        decodedResponse["verifiable_presentations"]
            ?.jsonArray
            ?.map {
                VerifiablePresentation.Generic(value = it.jsonPrimitive.content)
            } ?: return null
    // get the presentation submission from the decoded response or return null
    val presentationSubmission: PresentationSubmission =
        decodedResponse["presentation_submission"]
            ?.jsonObject
            ?.let {
                Json.decodeFromJsonElement(it)
            } ?: return null

    return Pair(verifiablePresentations, presentationSubmission)
}

/**
 * Parses an mso_mdoc document from a Verifiable Presentation.
 * This function extracts the mso_mdoc data from the verifiable presentation,
 * decodes it from Base64, and then passes it to the parseMsoMdoc function.
 *
 * @param vp The generic Verifiable Presentation containing the mso_mdoc document.
 * @param metadata Optional list of metadata strings associated with the documents.
 * @return A list of PresentedDocument objects parsed from the mso_mdoc data.
 */
fun parseMsoMdocFromVp(
    vp: VerifiablePresentation.Generic,
    metadata: List<String?>?
): List<PresentedDocument> {

    // but first we need to decode the verifiable presentation
    val msoMdocResponse = Base64.getUrlDecoder().decode(vp.value)

    return parseMsoMdoc(
        rawResponse = msoMdocResponse,
        sessionTranscript = null,
        metadata = metadata
    )
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
    metadata: String?
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
        metadata = metadata?.let { DocumentMetaData.fromJson(it).getOrNull() }
    )

    return presentedDocument
}

/**
 * This function is used to remove the key binding from the sd-jwt
 * due to limitations of the sd-jwt library and [eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.verify] method.
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
            it.value.toList().map(Any::toString) to jsonObject.select(it).getOrNull()
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
    metadata: DocumentMetaData?
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
 * Function to get the index from the path of the descriptor map
 * It takes the descriptor map to parse the path
 * and returns the index as an integer
 *
 * If the path is "$" it returns 0
 *
 * @param descriptorMap the descriptor map to parse
 * @return the index as an integer
 */
fun getIndexFormPath(
    descriptorMap: DescriptorMap,
): Int {
    return if (descriptorMap.path.value == "$") {
        0
    } else {
        val regex = Regex("\\[(\\d+)]")
        // parse $[(\d+)] with regex and extract the number
        val matchResult = regex.find(descriptorMap.path.value)
        matchResult?.groups?.get(1)?.value?.toInt() ?: 0
    }
}

/**
 * Function to get the indices of the descriptor maps by format
 * It takes the presentation submission and returns a map of format to list of indices
 * @receiver presentationSubmission the presentation submission to parse
 * @return the map of format to list of indices
 */
fun PresentationSubmission.getResponseIndicesByFormat(): Map<String, List<Int>> {
    return descriptorMaps.groupBy { it.format }.mapValues { (_, maps) ->
        maps.map(::getIndexFormPath)
    }
}

/**
 * Function to find the claim metadata from the path and metadata
 * It takes the path as a list of strings and metadata and iterates [DocumentMetaData.claims] to find
 * the claim metadata
 *
 * @param path the path to parse
 * @param metadata the metadata to parse
 * @return the claim metadata as a [DocumentMetaData.Claim] object
 */
fun findClaimMetadataForSdJwtVc(
    path: List<String>,
    metadata: DocumentMetaData?
): DocumentMetaData.Claim? {
    return metadata?.claims?.find {
        it.path.size == path.size && it.path.zip(path).all { (a, b) -> a == b }
    }
}

