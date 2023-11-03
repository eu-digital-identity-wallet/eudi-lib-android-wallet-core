/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.eudi.wallet.transfer.openid4vp

import android.content.Context
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.internal.OpenId4VpUtil
import eu.europa.ec.eudi.wallet.internal.executeOnMain
import eu.europa.ec.eudi.iso18013.transfer.Request
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.DispatchOutcome
import eu.europa.ec.eudi.openid4vp.JwkSetSource
import eu.europa.ec.eudi.openid4vp.PreregisteredClient
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.SiopOpenId4Vp
import eu.europa.ec.eudi.openid4vp.SubjectSyntaxType
import eu.europa.ec.eudi.openid4vp.SupportedClientIdScheme
import eu.europa.ec.eudi.openid4vp.WalletOpenId4VPConfig
import eu.europa.ec.eudi.openid4vp.asException
import eu.europa.ec.eudi.prex.ClaimFormat
import eu.europa.ec.eudi.prex.DescriptorMap
import eu.europa.ec.eudi.prex.Id
import eu.europa.ec.eudi.prex.JsonPath
import eu.europa.ec.eudi.prex.PresentationSubmission
import java.net.URI
import java.time.Duration
import java.util.Base64
import java.util.Date
import java.util.UUID

/**
 * OpenId4vp manager. This class is used to manage the OpenId4vp. It is used to resolve the request uri and send the response.
 *
 * Example:
 * ```
 * val openId4vpManager = OpenId4vpManager(
 *    context,
 *    verifierApi = "https://verifier-api.com"
 *    documentManager = documentManager
 * )
 * val transferEventListener = TransferEvent.Listener { event ->
 *   when (event) {
 *      is TransferEvent.Connecting -> {
 *          // inform user
 *      }
 *      is TransferEvent.RequestReceived -> {
 *          val request = openId4vpManager.resolveRequestUri(event.request)
 *          // handle request and demand from user the documents to be disclosed
 *          val disclosedDocuments = listOf<DisclosedDocument>()
 *          val response = EudiWalletSDK.createResponse(disclosedDocuments)
 *          openId4vpManager.sendResponse(response)
 *      }
 *   }
 * }
 * openId4vpManager.addTransferEventListener(transferEventListener)
 *
 * @property documentManager
 * @constructor
 *
 * @param context
 * @param verifierApi
 */
class OpenId4vpManager(
    context: Context,
    verifierApi: String,
    private val documentManager: DocumentManager,
) : TransferEvent.Listenable {

    private val context = context.applicationContext

    private val verifierMetaData = PreregisteredClient(
        "Verifier",
        JWSAlgorithm.RS256.name,
        JwkSetSource.ByReference(URI("$verifierApi/wallet/public-keys.json"))
    )

    private val walletKeyPair = RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key (optional)
        .keyID(UUID.randomUUID().toString()) // give the key a unique ID (optional)
        .issueTime(Date(System.currentTimeMillis())) // issued-at timestamp (optional)
        .generate()

    private val config = WalletOpenId4VPConfig(
        presentationDefinitionUriSupported = true,
        supportedClientIdSchemes = listOf(
            SupportedClientIdScheme.Preregistered(
                mapOf(
                    verifierMetaData.clientId to verifierMetaData
                )
            )
        ),
        vpFormatsSupported = emptyList(),
        subjectSyntaxTypesSupported = emptyList(),
        signingKey = walletKeyPair,
        signingKeySet = JWKSet(walletKeyPair),
        idTokenTTL = Duration.ofMinutes(10),
        preferredSubjectSyntaxType = SubjectSyntaxType.JWKThumbprint,
        decentralizedIdentifier = "DID:example:12341512#$",
        authorizationSigningAlgValuesSupported = emptyList(),
        authorizationEncryptionAlgValuesSupported = listOf(JWEAlgorithm.parse("ECDH-ES")),
        authorizationEncryptionEncValuesSupported = listOf(EncryptionMethod.parse("A256GCM"))
    )

    private val siopOpenId4Vp = SiopOpenId4Vp.ktor(config)
    private var transferEventListeners: MutableList<TransferEvent.Listener> = mutableListOf()
    private var resolvedRequestObject: ResolvedRequestObject? = null

    /**
     * Resolve request uri and call the listener with the request object.
     *
     * @param openid4VPURI
     */
    fun resolveRequestUri(openid4VPURI: String) {
        transferEventListeners.onTransferEvent(TransferEvent.Connecting)

        context.executeOnMain {
            when (val resolution = siopOpenId4Vp.resolveRequestUri(openid4VPURI)) {
                is Resolution.Invalid -> {
                    transferEventListeners.onTransferEvent(TransferEvent.Error(resolution.error.asException()))
                }

                is Resolution.Success -> resolution.requestObject
                    .also { resolvedRequestObject = it }
                    .let { requestObject ->
                        when (requestObject) {
                            is ResolvedRequestObject.OpenId4VPAuthorization -> {
                                transferEventListeners.onTransferEvent(
                                    TransferEvent.RequestReceived(
                                        requestObject.asRequest()
                                    )
                                )
                            }

                            is ResolvedRequestObject.SiopAuthentication -> {
                                transferEventListeners.onTransferEvent("SiopAuthentication request received, not supported yet.".err())
                            }

                            is ResolvedRequestObject.SiopOpenId4VPAuthentication -> {
                                transferEventListeners.onTransferEvent("SiopAuthentication request received, not supported yet.".err())
                            }

                            else -> transferEventListeners.onTransferEvent("Unknown request received".err())

                        }
                    }
            }
        }
    }

    /**
     * Send response to the verifier.
     *
     * @param deviceResponse
     */
    fun sendResponse(deviceResponse: ByteArray) {
        context.executeOnMain {
            resolvedRequestObject?.let { resolvedRequestObject ->
                when (resolvedRequestObject) {
                    is ResolvedRequestObject.OpenId4VPAuthorization -> {
                        val presentationDefinition =
                            (resolvedRequestObject).presentationDefinition
                        val inputDescriptor =
                            presentationDefinition.inputDescriptors.first()
                        val vpToken =
                            Base64.getUrlEncoder().withoutPadding().encodeToString(deviceResponse)
                        val consensus = Consensus.PositiveConsensus.VPTokenConsensus(
                            vpToken,
                            presentationSubmission = PresentationSubmission(
                                id = Id("pid-res"), // TODO id value ?
                                definitionId = presentationDefinition.id,
                                listOf(
                                    DescriptorMap(
                                        id = inputDescriptor.id,
                                        format = ClaimFormat.MsoMdoc,
                                        path = JsonPath.jsonPath("$")!! // TODO path ?
                                    )
                                )
                            )
                        )

                        val authorizationResponse =
                            siopOpenId4Vp.build(resolvedRequestObject, consensus)
                        when (siopOpenId4Vp.dispatch(authorizationResponse)) {
                            is DispatchOutcome.VerifierResponse.Accepted -> {
                                transferEventListeners.onTransferEvent(TransferEvent.ResponseSent)
                            }

                            is DispatchOutcome.VerifierResponse.Rejected -> {
                                transferEventListeners.onTransferEvent("DispatchOutcome: VerifierResponse Rejected".err())
                            }

                            is DispatchOutcome.RedirectURI -> {
                                transferEventListeners.onTransferEvent(TransferEvent.ResponseSent)
                            }
                        }
                        transferEventListeners.onTransferEvent(TransferEvent.Disconnected)
                    }

                    else -> {
                        transferEventListeners.onTransferEvent("${resolvedRequestObject.javaClass} not supported yet.".err())
                    }
                }
            }
        }
    }

    fun close() {
        resolvedRequestObject = null
        removeAllTransferEventListeners()
    }

    private fun List<TransferEvent.Listener>.onTransferEvent(event: TransferEvent) {
        forEach { it.onTransferEvent(event) }
    }

    private fun String.err(): TransferEvent.Error {
        return TransferEvent.Error(Throwable(this))
    }

    private fun ResolvedRequestObject.OpenId4VPAuthorization.asRequest(): Request {
        return OpenId4VpUtil.parsePresentationDefinition(
            documentManager, presentationDefinition
        )
    }

    override fun addTransferEventListener(listener: TransferEvent.Listener): OpenId4vpManager =
        apply {
            transferEventListeners.add(listener)
        }

    override fun removeTransferEventListener(listener: TransferEvent.Listener): OpenId4vpManager =
        apply {
            transferEventListeners.remove(listener)
        }

    override fun removeAllTransferEventListeners(): OpenId4vpManager = apply {
        transferEventListeners.clear()
    }
}
