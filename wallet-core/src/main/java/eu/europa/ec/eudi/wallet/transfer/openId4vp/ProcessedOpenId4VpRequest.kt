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

import com.android.identity.crypto.Algorithm
import com.android.identity.securearea.KeyUnlockData
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.AsymmetricJWK
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.iso18013.transfer.response.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse
import eu.europa.ec.eudi.iso18013.transfer.response.device.ProcessedDeviceRequest
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VpToken
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
import eu.europa.ec.eudi.sdjwt.NimbusSdJwtOps.kbJwtIssuer
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import eu.europa.ec.eudi.sdjwt.vc.ClaimPathElement
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.OpenId4VpUtils.getSessionTranscriptBytes
import eu.europa.ec.eudi.wallet.issue.openid4vci.toJoseEncoded
import kotlinx.coroutines.runBlocking
import java.util.Base64
import java.util.Date
import java.util.UUID

class ProcessedMsoMdocOpenId4VpRequest(
    private val processedDeviceRequest: ProcessedDeviceRequest,
    private val resolvedRequestObject: ResolvedRequestObject,
    val msoMdocNonce: String,
) : RequestProcessor.ProcessedRequest.Success(processedDeviceRequest.requestedDocuments) {
    override fun generateResponse(
        disclosedDocuments: DisclosedDocuments,
        signatureAlgorithm: Algorithm?,
    ): ResponseResult {
        return try {
            require(resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization)
            val deviceResponse = processedDeviceRequest.generateResponse(
                disclosedDocuments,
                signatureAlgorithm
            ).getOrThrow() as DeviceResponse
            val vpToken = VpToken.MsoMdoc(
                Base64URL.encode(msoMdocNonce),
                Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(deviceResponse.deviceResponseBytes)
            )

            val presentationDefinition = resolvedRequestObject.presentationDefinition
            val consensus = Consensus.PositiveConsensus.VPTokenConsensus(
                vpToken = vpToken,
                presentationSubmission = PresentationSubmission(
                    id = Id(UUID.randomUUID().toString()),
                    definitionId = presentationDefinition.id,
                    descriptorMaps = presentationDefinition.inputDescriptors.map { inputDescriptor ->
                        DescriptorMap(
                            id = inputDescriptor.id,
                            format = "mso_mdoc",
                            path = JsonPath.Companion.jsonPath("$")!!
                        )
                    }
                )
            )
            ResponseResult.Success(
                OpenId4VpResponse.DeviceResponse(
                    resolvedRequestObject = resolvedRequestObject,
                    consensus = consensus,
                    vpToken = vpToken,
                    responseBytes = deviceResponse.deviceResponseBytes,
                    msoMdocNonce = msoMdocNonce
                )
            )
        } catch (e: Throwable) {
            ResponseResult.Failure(e)
        }
    }
}

class ProcessedGenericOpenId4VpRequest(
    private val documentManager: DocumentManager,
    private val resolvedRequestObject: ResolvedRequestObject,
    private val inputDescriptorMap: Map<InputDescriptorId, List<DocumentId>>,
    requestedDocuments: RequestedDocuments,
    val msoMdocNonce: String?,
) : RequestProcessor.ProcessedRequest.Success(requestedDocuments) {
    override fun generateResponse(
        disclosedDocuments: DisclosedDocuments,
        signatureAlgorithm: Algorithm?,
    ): ResponseResult {
        return try {
            require(resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization)
            val verifiablePresentations = disclosedDocuments.map { disclosedDocument ->
                val document =
                    documentManager.getValidIssuedDocumentById(disclosedDocument.documentId)
                when (document.format) {
                    is SdJwtVcFormat -> {
                        val issuedSdJwt =
                            DefaultSdJwtOps.unverifiedIssuanceFrom(String(document.issuerProvidedData))
                                .getOrThrow()
                        document.id to VerifiablePresentation.Generic(
                            issuedSdJwt.present(disclosedDocument.disclosedItems.map { disclosedItem ->
                                require(disclosedItem is SdJwtVcItem)
                                ClaimPath(disclosedItem.path.map {
                                    ClaimPathElement.Claim(it)
                                })
                            }.toSet())?.run {
                                // check if cnf is present and present with key binding
                                issuedSdJwt.jwt.second["cnf"]?.run {
                                    presentWithKeyBinding(
                                        signatureAlgorithm ?: Algorithm.ES256,
                                        document,
                                        disclosedDocument.keyUnlockData,
                                        resolvedRequestObject.client.id,
                                        resolvedRequestObject.nonce,
                                        Date()
                                    )
                                } ?: serialize()
                            }
                                ?: return ResponseResult.Failure(IllegalArgumentException("Failed to create SD JWT VC presentation")))
                    }

                    is MsoMdocFormat -> {
                        val deviceResponse = ProcessedDeviceRequest(
                            documentManager = documentManager,
                            sessionTranscript = resolvedRequestObject.getSessionTranscriptBytes(
                                msoMdocNonce!!
                            ),
                            requestedDocuments = RequestedDocuments(requestedDocuments.filter { it.documentId == disclosedDocument.documentId })
                        ).generateResponse(
                            DisclosedDocuments(disclosedDocument),
                            signatureAlgorithm
                        ).getOrThrow() as DeviceResponse
                        document.id to VerifiablePresentation.MsoMdoc(
                            Base64.getUrlEncoder().withoutPadding()
                                .encodeToString(deviceResponse.deviceResponseBytes)
                        )
                    }
                }
            }.toList()
            val vpToken = VpToken((verifiablePresentations.map { it.second }).toList(),
                msoMdocNonce?.let { Base64URL.encode(it) })
            val presentationDefinition = resolvedRequestObject.presentationDefinition
            val consensus = Consensus.PositiveConsensus.VPTokenConsensus(
                vpToken = vpToken,
                presentationSubmission = PresentationSubmission(
                    id = Id(UUID.randomUUID().toString()),
                    definitionId = presentationDefinition.id,
                    descriptorMaps = inputDescriptorMap.entries.flatMap { inputDescriptor ->
                        verifiablePresentations.mapIndexed { index, vp ->
                            inputDescriptor.value.takeIf { it.contains(vp.first) }?.let {
                                DescriptorMap(
                                    id = inputDescriptor.key,
                                    format = when (documentManager.getValidIssuedDocumentById(vp.first).format) {
                                        is MsoMdocFormat -> "mso_mdoc"
                                        is SdJwtVcFormat -> "vc+sd-jwt"
                                    },
                                    path = JsonPath.jsonPath(if (verifiablePresentations.size > 1) "$[$index]" else "$")!!
                                )
                            }
                        }
                    }.filterNotNull()
                )
            )
            ResponseResult.Success(
                OpenId4VpResponse.GenericResponse(
                    resolvedRequestObject = resolvedRequestObject,
                    consensus = consensus,
                    vpToken = vpToken,
                    response = verifiablePresentations.map { it.second.toString() }.toList()
                )
            )
        } catch (e: Throwable) {
            ResponseResult.Failure(e)
        }
    }
}

private fun SdJwt<JwtAndClaims>.presentWithKeyBinding(
    signatureAlgorithm: Algorithm,
    document: IssuedDocument,
    keyUnlockData: KeyUnlockData?,
    clientId: ClientId,
    nonce: String,
    issueDate: Date,
): String {
    return runBlocking {
        val algorithm = JWSAlgorithm.parse((signatureAlgorithm).jwseAlgorithmIdentifier)
        val buildKbJwt = kbJwtIssuer(
            signer = object : JWSSigner {
                override fun getJCAContext(): JCAContext = JCAContext()
                override fun supportedJWSAlgorithms(): Set<JWSAlgorithm> = setOf(algorithm)
                override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
                    val signature =
                        document.sign(signingInput, signatureAlgorithm, keyUnlockData).getOrThrow()
                    return Base64URL.encode(signature.toJoseEncoded(algorithm))
                }
            },
            signAlgorithm = algorithm,
            publicKey = JWK.parseFromPEMEncodedObjects(document.keyInfo.publicKey.toPem()) as AsymmetricJWK
        ) {
            audience(clientId)
            claim("nonce", nonce)
            issueTime(issueDate)
        }
        serializeWithKeyBinding(buildKbJwt).getOrThrow()
    }
}