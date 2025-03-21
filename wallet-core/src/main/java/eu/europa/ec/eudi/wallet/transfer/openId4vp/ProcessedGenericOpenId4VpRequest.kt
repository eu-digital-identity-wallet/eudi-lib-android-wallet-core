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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import com.android.identity.crypto.Algorithm
import com.android.identity.securearea.KeyUnlockData
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.AsymmetricJWK
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.PresentationQuery
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VerifierId
import eu.europa.ec.eudi.openid4vp.VpContent
import eu.europa.ec.eudi.prex.DescriptorMap
import eu.europa.ec.eudi.prex.Id
import eu.europa.ec.eudi.prex.InputDescriptorId
import eu.europa.ec.eudi.prex.JsonPath
import eu.europa.ec.eudi.prex.PresentationSubmission
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.present
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.serialize
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.serializeWithKeyBinding
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.NimbusSdJwtOps
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import eu.europa.ec.eudi.sdjwt.vc.ClaimPathElement
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.getSessionTranscriptBytes
import eu.europa.ec.eudi.wallet.issue.openid4vci.toJoseEncoded
import kotlinx.coroutines.runBlocking
import java.util.Base64
import java.util.Date
import java.util.UUID

class ProcessedGenericOpenId4VpRequest(
    private val documentManager: DocumentManager,
    private val resolvedRequestObject: ResolvedRequestObject,
    private val inputDescriptorMap: Map<InputDescriptorId, List<DocumentId>>,
    requestedDocuments: RequestedDocuments,
    val msoMdocNonce: String,
) : RequestProcessor.ProcessedRequest.Success(requestedDocuments) {

    override fun generateResponse(
        disclosedDocuments: DisclosedDocuments,
        signatureAlgorithm: Algorithm?,
    ): ResponseResult {
        return try {
            require(resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization)
            val presentationQuery = resolvedRequestObject.presentationQuery
            require(presentationQuery is PresentationQuery.ByPresentationDefinition) {
                "Currently only PresentationDefinition is supported"
            }

            val signatureAlgorithm = signatureAlgorithm ?: Algorithm.ES256
            val presentationDefinition = presentationQuery.value

            val verifiablePresentations = disclosedDocuments
                .filter { it.disclosedItems.isNotEmpty() } // remove empty disclosed documents
                .map { disclosedDocument ->
                    val document = documentManager.getValidIssuedDocumentById(
                        documentId = disclosedDocument.documentId
                    )
                    val verifiablePresentation = when (document.format) {
                        is SdJwtVcFormat -> verifiablePresentationForSdJwtVc(
                            document = document,
                            disclosedDocument = disclosedDocument,
                            signatureAlgorithm = signatureAlgorithm,
                        )

                        is MsoMdocFormat -> verifiablePresentationForMsoMdoc(
                            sessionTranscript = resolvedRequestObject
                                .getSessionTranscriptBytes(msoMdocNonce),
                            disclosedDocument = disclosedDocument,
                            requestedDocuments = requestedDocuments,
                            signatureAlgorithm = signatureAlgorithm
                        )
                    }
                    Pair(document, verifiablePresentation)
                }

            val descriptorMaps = constructDescriptorsMap(
                inputDescriptorMap = inputDescriptorMap,
                verifiablePresentations = verifiablePresentations
            )

            val presentationSubmission = PresentationSubmission(
                id = Id(UUID.randomUUID().toString()),
                definitionId = presentationDefinition.id,
                descriptorMaps = descriptorMaps,
            )
            val vpContent = VpContent.PresentationExchange(
                verifiablePresentations = verifiablePresentations.map { it.second }.toList(),
                presentationSubmission = presentationSubmission,
            )
            val consensus = Consensus.PositiveConsensus.VPTokenConsensus(vpContent)

            ResponseResult.Success(
                OpenId4VpResponse.GenericResponse(
                    resolvedRequestObject = resolvedRequestObject,
                    consensus = consensus,
                    msoMdocNonce = msoMdocNonce,
                    response = verifiablePresentations
                        .map { it.second.toString() }
                        .toList()
                )
            )
        } catch (e: Throwable) {
            ResponseResult.Failure(e)
        }
    }

    private fun verifiablePresentationForMsoMdoc(
        disclosedDocument: DisclosedDocument,
        requestedDocuments: RequestedDocuments,
        sessionTranscript: ByteArray,
        signatureAlgorithm: Algorithm,
    ): VerifiablePresentation.Generic {
        val deviceResponse = ProcessedDeviceRequest(
            documentManager = documentManager,
            sessionTranscript = sessionTranscript,
            requestedDocuments = RequestedDocuments(requestedDocuments.filter { it.documentId == disclosedDocument.documentId })
        ).generateResponse(
            disclosedDocuments = DisclosedDocuments(disclosedDocument),
            signatureAlgorithm = signatureAlgorithm
        ).getOrThrow() as DeviceResponse

        return VerifiablePresentation.Generic(
            value = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(deviceResponse.deviceResponseBytes)
        )
    }

    private fun verifiablePresentationForSdJwtVc(
        document: IssuedDocument,
        disclosedDocument: DisclosedDocument,
        signatureAlgorithm: Algorithm,
    ): VerifiablePresentation.Generic {
        val unverifiedSdJwt = String(document.issuerProvidedData)
        val issuedSdJwt = DefaultSdJwtOps
            .unverifiedIssuanceFrom(unverifiedSdJwt)
            .getOrThrow()

        val query = disclosedDocument.disclosedItems
            .filterIsInstance<SdJwtVcItem>()
            .map { item ->
                ClaimPath(
                    value = item.path.map { ClaimPathElement.Claim(it) }
                )
            }.toSet()

        if (query.isEmpty()) {
            throw IllegalArgumentException("No claims to disclose")
        }

        val presentation = issuedSdJwt.present(query)
            ?: throw IllegalArgumentException("Failed to create SD JWT VC presentation")

        val containsCnf = issuedSdJwt.jwt.second["cnf"] != null

        val serialized = if (containsCnf) {
            presentation.serializeWithKeyBinding(
                document = document,
                keyUnlockData = disclosedDocument.keyUnlockData,
                clientId = resolvedRequestObject.client.id,
                nonce = resolvedRequestObject.nonce,
                signatureAlgorithm = signatureAlgorithm,
                issueDate = Date()
            )
        } else {
            presentation.serialize()
        }

        return VerifiablePresentation.Generic(serialized)
    }


    private fun SdJwt<JwtAndClaims>.serializeWithKeyBinding(
        document: IssuedDocument,
        keyUnlockData: KeyUnlockData?,
        clientId: VerifierId,
        nonce: String,
        signatureAlgorithm: Algorithm,
        issueDate: Date,
    ): String {
        return runBlocking {
            val algorithm = JWSAlgorithm.parse((signatureAlgorithm).jwseAlgorithmIdentifier)
            val buildKbJwt = NimbusSdJwtOps.kbJwtIssuer(
                signer = object : JWSSigner {
                    override fun getJCAContext(): JCAContext = JCAContext()
                    override fun supportedJWSAlgorithms(): Set<JWSAlgorithm> = setOf(algorithm)
                    override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
                        val signature =
                            document.sign(signingInput, signatureAlgorithm, keyUnlockData)
                                .getOrThrow()
                        return Base64URL.encode(signature.toJoseEncoded(algorithm))
                    }
                },
                signAlgorithm = algorithm,
                publicKey = JWK.parseFromPEMEncodedObjects(document.keyInfo.publicKey.toPem()) as AsymmetricJWK
            ) {
                audience(clientId.clientId)
                claim("nonce", nonce)
                issueTime(issueDate)
            }
            serializeWithKeyBinding(buildKbJwt).getOrThrow()
        }
    }
}

internal fun constructDescriptorsMap(
    inputDescriptorMap: Map<InputDescriptorId, List<DocumentId>>,
    verifiablePresentations: List<Pair<IssuedDocument, VerifiablePresentation.Generic>>,
): List<DescriptorMap> {

    val descriptorMaps = verifiablePresentations.mapIndexed { index, (document, _) ->
        // get the input descriptor id for the document
        // that is in the verifiable presentation
        val inputDescriptorId = inputDescriptorMap.entries
            .firstOrNull { (_, documentIds) ->
                documentIds.contains(document.id)
            }?.key
            ?: throw IllegalArgumentException("No input descriptor found for document")
        // determine the format of the document
        val format = when (document.format) {
            is MsoMdocFormat -> FORMAT_MSO_MDOC
            is SdJwtVcFormat -> FORMAT_SD_JWT_VC
        }
        // create the json path for the document
        // if there are multiple verifiable presentations
        // the json path will be an array
        // e.g. $[0], $[1]
        // otherwise it will be just $
        val jsonPath = JsonPath.jsonPath(
            if (verifiablePresentations.size > 1) {
                "$[$index]"
            } else {
                "$"
            }
        ) ?: throw IllegalStateException("Failed to create JsonPath")

        // create the descriptor map
        DescriptorMap(
            id = inputDescriptorId,
            format = format,
            path = jsonPath
        )
    }

    return descriptorMaps
}

