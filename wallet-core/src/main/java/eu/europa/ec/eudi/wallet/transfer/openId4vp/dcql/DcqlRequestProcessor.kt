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

import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuth
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocument
import eu.europa.ec.eudi.iso18013.transfer.response.RequestedDocuments
import eu.europa.ec.eudi.iso18013.transfer.response.device.MsoMdocItem
import eu.europa.ec.eudi.openid4vp.Format
import eu.europa.ec.eudi.openid4vp.PresentationQuery
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.dcql.metaMsoMdoc
import eu.europa.ec.eudi.openid4vp.dcql.metaSdJwtVc
import eu.europa.ec.eudi.openid4vp.legalName
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.generateMdocGeneratedNonce
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpReaderTrust
import eu.europa.ec.eudi.wallet.transfer.openId4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ReaderTrustResult
import eu.europa.ec.eudi.wallet.transfer.openId4vp.SdJwtVcItem
import kotlinx.coroutines.runBlocking

/**
 * Processes OpenID4VP requests using DCQL (Digital Credentials Query Language).
 *
 * This processor validates the request, extracts credential requirements, and matches them
 * against the locally available documents. It supports both MSO mdoc and SD-JWT VC formats.
 *
 * @property documentManager Manages the documents available in the wallet.
 * @property openid4VpX509CertificateTrust Trust anchor for verifying the reader's certificate.
 */
class DcqlRequestProcessor(
    private val documentManager: DocumentManager,
    var openid4VpX509CertificateTrust: OpenId4VpReaderTrust
) : RequestProcessor {

    /**
     * Processes an [OpenId4VpRequest] and returns a [ProcessedDcqlRequest] containing
     * the matched documents and requested items for each credential in the query.
     *
     * @param request The request to process. Must be an [OpenId4VpRequest] with a resolved [OpenId4VPAuthorization].
     * @return [ProcessedDcqlRequest] with the results of the credential query.
     * @throws IllegalArgumentException if the request or its contents are invalid or unsupported.
     */
    override fun process(request: Request): RequestProcessor.ProcessedRequest {
        // Validate request type and structure
        require(request is OpenId4VpRequest) { "Request must be an OpenId4VpRequest" }
        require(request.resolvedRequestObject is ResolvedRequestObject.OpenId4VPAuthorization) {
            "Request must have be a OpenId4VPAuthorization"
        }

        val presentationQuery = request.resolvedRequestObject.presentationQuery
        require(presentationQuery is PresentationQuery.ByDigitalCredentialsQuery) {
            "Not supported ${presentationQuery::class.java.simpleName}"
        }
        val dcql = presentationQuery.value
        val credentials = dcql.credentials
        requireNotNull(credentials) {
            "No credentials are requested"
        }

        val readerCommonName = request.resolvedRequestObject.client.legalName() ?: ""
        // Extract reader authentication/trust result if available
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
        // Map each credential in the query to the set of matching documents and requested items
        val queryRequestedDocumentsMap = credentials
            .filter { it.meta != null }
            .associate { credential ->
                when (val format = credential.format) {
                    Format.MsoMdoc -> {
                        val docTypeValue = credential.metaMsoMdoc!!.doctypeValue
                        requireNotNull(docTypeValue) {
                            "DocType is missing for credential with id ${credential.id}"
                        }
                        val docType = docTypeValue.value
                        val documents =
                            runBlocking { findDocumentsByFormat(format = MsoMdocFormat(docType)) }
                        val requestedItems = credential.claims?.associate { claim ->
                            MsoMdocItem(
                                namespace = claim.path.value.first().toString(),
                                elementIdentifier = claim.path.value.last().toString()
                            ) to (claim.intentToRetain == true)
                        }
                        val requestedDocuments = RequestedDocuments(documents.map { document ->
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
                        credential.id to requestedDocuments
                    }

                    Format.SdJwtVc -> {
                        val vctValues = credential.metaSdJwtVc!!.vctValues
                        requireNotNull(vctValues) {
                            "VctValues are missing for credential with id ${credential.id}"
                        }
                        require(vctValues.isNotEmpty()) {
                            "VctValues are empty for credential with id ${credential.id}"
                        }
                        val requestedItems = credential.claims?.associate { claim ->
                            SdJwtVcItem(path = claim.path.value.map { it.toString() }) to (claim.intentToRetain == true)
                        }

                        val documents = runBlocking {
                            vctValues.flatMap {
                                findDocumentsByFormat(SdJwtVcFormat(it))
                            }
                        }

                        val requestedDocuments = RequestedDocuments(documents.map { document ->
                            RequestedDocument(
                                documentId = document.id,
                                requestedItems = requestedItems ?: document.data.claims
                                    .filterIsInstance<SdJwtVcClaim>()
                                    .associate {
                                        SdJwtVcItem(
                                            path = it.path.value.map { it.toString() }
                                        ) to false
                                    },
                                readerAuth = readerAuth
                            )
                        })

                        credential.id to requestedDocuments
                    }

                    else -> throw IllegalArgumentException("Not supported format ${format.value}")
                }
            }

        return ProcessedDcqlRequest(
            resolvedRequestObject = request.resolvedRequestObject,
            documentManager = documentManager,
            queryMap = queryRequestedDocumentsMap,
            msoMdocNonce = generateMdocGeneratedNonce()
        )
    }

    /**
     * Finds all issued documents matching the given [DocumentFormat].
     *
     * @param format The document format to match.
     * @return List of [IssuedDocument]s matching the format and containing credentials.
     */
    private suspend fun findDocumentsByFormat(format: DocumentFormat): List<IssuedDocument> {
        return documentManager.getDocuments()
            .filter { it.format == format }
            .filterIsInstance<IssuedDocument>()
            .filter { it.findCredential() != null }
    }
}
