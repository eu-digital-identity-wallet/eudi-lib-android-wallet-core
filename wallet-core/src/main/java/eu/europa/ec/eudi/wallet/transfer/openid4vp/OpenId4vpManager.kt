/*
 *  Copyright (c) 2023-2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet.transfer.openid4vp

import android.content.Context
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.SessionTranscriptBytes
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.DefaultHttpClientFactory
import eu.europa.ec.eudi.openid4vp.DispatchOutcome
import eu.europa.ec.eudi.openid4vp.JarmConfiguration
import eu.europa.ec.eudi.openid4vp.JwkSetSource
import eu.europa.ec.eudi.openid4vp.PreregisteredClient
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.openid4vp.SiopOpenId4VPConfig
import eu.europa.ec.eudi.openid4vp.SiopOpenId4Vp
import eu.europa.ec.eudi.openid4vp.SupportedClientIdScheme
import eu.europa.ec.eudi.openid4vp.VpToken
import eu.europa.ec.eudi.openid4vp.asException
import eu.europa.ec.eudi.prex.DescriptorMap
import eu.europa.ec.eudi.prex.Id
import eu.europa.ec.eudi.prex.JsonPath
import eu.europa.ec.eudi.prex.PresentationSubmission
import eu.europa.ec.eudi.wallet.internal.Openid4VpUtils
import eu.europa.ec.eudi.wallet.internal.mainExecutor
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.logging.d
import eu.europa.ec.eudi.wallet.logging.e
import eu.europa.ec.eudi.wallet.logging.i
import eu.europa.ec.eudi.wallet.transfer.openid4vp.responseGenerator.OpenId4VpResponseGeneratorDelegator
import eu.europa.ec.eudi.wallet.util.CBOR
import eu.europa.ec.eudi.wallet.util.wrappedWithContentNegotiation
import eu.europa.ec.eudi.wallet.util.wrappedWithLogging
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bouncycastle.util.encoders.Hex
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executor

/**
 * OpenId4vp manager. This class is used to manage the OpenId4vp transfer method. It is used to resolve the request uri and send the response.
 *
 * Example:
 * ```
 * val certificates = listOf<X509Certificate>(
 *     // put trusted reader certificates here
 * )
 * val readerTrustStore = ReaderTrustStore.getDefault(
 *     listOf(context.applicationContext.getCertificate(certificates))
 * )
 *
 * val documentedResolver = DocumentResolver { docRequest: DocRequest ->
 *     // put your code here to resolve the document
 *     // usually document resolution is done based on `docRequest.docType`
 * }
 *
 * val openid4VpCBORResponseGenerator = OpenId4VpCBORResponseGeneratorImpl.Builder(context)
 *                 .readerTrustStore(readerTrustStore)
 *                 .documentsResolver(documentedResolver)
 *                 .build()
 *
 * val openId4vpManager = OpenId4vpManager(
 *    context,
 *    OpenId4VpConfig.Builder()
 *             .withClientIdSchemes(
 *             listOf(
 *                 ClientIdScheme.Preregistered(
 *                     listOf(
 *                         PreregisteredVerifier(
 *                             "VerifierClientId",
 *                             "VerifierLegalName",
 *                             "https://example.com"
 *                         )
 *                     )
 *                 ),
 *                 ClientIdScheme.X509SanDns
 *             ))
 *             .withEncryptionAlgorithms(listOf(EncryptionAlgorithm.ECDH_ES))
 *             .withEncryptionMethods(listOf(EncryptionMethod.A128CBC_HS256))
 *             .build(),
 *    openid4VpCBORResponseGenerator
 * )
 * val transferEventListener = TransferEvent.Listener { event ->
 *   when (event) {
 *      is TransferEvent.Connecting -> {
 *          // inform user
 *      }
 *      is Transfer.Redirect -> {
 *          val redirect_uri = event.redirectUri
 *          // redirect user to the given URI
 *      }
 *      is TransferEvent.RequestReceived -> {
 *          val request = openId4vpManager.resolveRequestUri(event.request)
 *          // handle request and demand from user the documents to be disclosed
 *          val disclosedDocuments = listOf<DisclosedDocument>()
 *          val response = openid4VpCBORResponseGenerator.createResponse(disclosedDocuments)
 *          openId4vpManager.sendResponse(response.deviceResponseBytes)
 *      }
 *   }
 * }
 * openId4vpManager.addTransferEventListener(transferEventListener)
 *
 * // resolve a request URI
 * openId4vpManager.resolveRequestUri(requestURI)
 *
 * ```
 * @property responseGenerator that parses the request and creates the response
 * @property logger the logger
 * @property ktorHttpClientFactory the factory to create the http client. By default, it uses the [DefaultHttpClientFactory]
 * @constructor
 * @param context the application context
 * @param openId4VpConfig the configuration for OpenId4Vp
 * @param responseGenerator that parses the request and creates the response
 */
class OpenId4vpManager(
    context: Context,
    openId4VpConfig: OpenId4VpConfig,
    val responseGenerator: OpenId4VpResponseGeneratorDelegator,
) : TransferEvent.Listenable {

    var logger: Logger? = null

    var ktorHttpClientFactory: () -> HttpClient = DefaultHttpClientFactory
        get() = field.wrappedWithLogging(logger).wrappedWithContentNegotiation()

    private val appContext = context.applicationContext
    private val ioScope = CoroutineScope(Job() + Dispatchers.IO)
    private var executor: Executor? = null

    private var transferEventListeners: MutableList<TransferEvent.Listener> = mutableListOf()
    private val onResultUnderExecutor = { result: TransferEvent ->
        (executor ?: appContext.mainExecutor()).execute {
            logger?.d(TAG, "onResultUnderExecutor $result")
            transferEventListeners.onTransferEvent(result)
        }
    }
    private val siopOpenId4Vp =
        SiopOpenId4Vp(openId4VpConfig.toSiopOpenId4VPConfig(), ktorHttpClientFactory)
    private var resolvedRequestObject: ResolvedRequestObject? = null
    private var mdocGeneratedNonce: String? = null

    /**
     * Setting the `executor` is optional and defines the executor that will be used to
     * execute the callback. If the `executor` is not defined, the callback will be executed on the
     * main thread.
     * @param executor the executor to use for callbacks. If null, the main executor will be used.
     */
    fun setExecutor(executor: Executor) {
        this.executor = executor
    }

    /**
     * Resolve a request uri
     *
     * @param openid4VPURI
     */
    fun resolveRequestUri(openid4VPURI: String) {
        logger?.d(
            TAG,
            "Resolve request uri: ${
                URLDecoder.decode(
                    openid4VPURI,
                    StandardCharsets.UTF_8.name()
                )
            }"
        )
        ioScope.launch {
            onResultUnderExecutor(TransferEvent.Connecting)
            runCatching { siopOpenId4Vp.resolveRequestUri(openid4VPURI) }.onSuccess { resolution ->
                when (resolution) {
                    is Resolution.Invalid -> {
                        logger?.e(TAG, "Resolution.Invalid", resolution.error.asException())
                        onResultUnderExecutor(TransferEvent.Error(resolution.error.asException()))
                    }

                    is Resolution.Success -> {
                        logger?.d(TAG, "Resolution.Success")
                        resolution.requestObject
                            .also { resolvedRequestObject = it }
                            .let { requestObject ->
                                when (requestObject) {
                                    is ResolvedRequestObject.OpenId4VPAuthorization -> {
                                        logger?.d(TAG, "OpenId4VPAuthorization Request received")

                                        val format =
                                            requestObject.presentationDefinition.inputDescriptors.first().format?.jsonObject()?.keys?.first() //TODO Format Type nutzen
                                        val request = when (format) {
                                            "mso_mdoc" -> {
                                                OpenId4VpRequest(
                                                    requestObject,
                                                    requestObject.toSessionTranscript()
                                                )
                                            }

                                            "vc_sd_jwt" -> {
                                                OpenId4VpSdJwtRequest(requestObject)
                                            }

                                            else -> {
                                                throw NotImplementedError(message = "Not supported: ${format}")
                                            }
                                        }

                                        onResultUnderExecutor(
                                            TransferEvent.RequestReceived(
                                                responseGenerator.parseRequest(request),
                                                request
                                            )
                                        )
                                    }

                                    is ResolvedRequestObject.SiopAuthentication -> {
                                        logger?.i(TAG, "SiopAuthentication Request received")
                                        onResultUnderExecutor("SiopAuthentication request received, not supported yet.".err())
                                    }

                                    is ResolvedRequestObject.SiopOpenId4VPAuthentication -> {
                                        logger?.i(
                                            TAG,
                                            "SiopOpenId4VPAuthentication Request received"
                                        )
                                        onResultUnderExecutor("SiopAuthentication request received, not supported yet.".err())
                                    }

                                    else -> {
                                        logger?.e(TAG, "Unknown request received")
                                        onResultUnderExecutor("Unknown request received".err())
                                    }
                                }
                            }
                    }
                }
            }.onFailure {
                logger?.e(TAG, "An error occurred resolving request uri: $openid4VPURI", it)
                onResultUnderExecutor(TransferEvent.Error(it))
            }
        }
    }

    /**
     * Sends a response to the verifier
     *
     * @param deviceResponse
     */
    fun sendResponse(deviceResponse: ByteArray) {

//        logger?.d(TAG, "Device Response to send (hex): ${Hex.toHexString(deviceResponse)}")
//        logger?.d(TAG, "Device Response to send (cbor): ${CBOR.cborPrettyPrint(deviceResponse)}")

        ioScope.launch {
            resolvedRequestObject?.let { resolvedRequestObject ->
                when (resolvedRequestObject) {
                    is ResolvedRequestObject.OpenId4VPAuthorization -> {

                        val vpTokenConsensus = when (responseGenerator.formatState) {
                            OpenId4VpResponseGeneratorDelegator.FormatState.Cbor -> {
                                mDocVPTokenConsensus(deviceResponse, resolvedRequestObject)
                            }

                            OpenId4VpResponseGeneratorDelegator.FormatState.SdJwt -> {
                                sdJwtVPTokenConsensus(deviceResponse, resolvedRequestObject)
                            }
                        }

                        runCatching {
                            siopOpenId4Vp.dispatch(
                                resolvedRequestObject,
                                vpTokenConsensus
                            )
                        }.onSuccess { dispatchOutcome ->
                            when (dispatchOutcome) {
                                is DispatchOutcome.VerifierResponse.Accepted -> {
                                    logger?.d(
                                        TAG,
                                        "VerifierResponse Accepted with redirectUri: $dispatchOutcome.redirectURI"
                                    )
                                    onResultUnderExecutor(TransferEvent.ResponseSent)
                                    dispatchOutcome.redirectURI?.let {
                                        onResultUnderExecutor(TransferEvent.Redirect(it))
                                    }
                                }

                                is DispatchOutcome.VerifierResponse.Rejected -> {
                                    logger?.d(TAG, "VerifierResponse Rejected")
                                    onResultUnderExecutor("DispatchOutcome: VerifierResponse Rejected".err())
                                }

                                is DispatchOutcome.RedirectURI -> {
                                    logger?.d(TAG, "VerifierResponse RedirectURI")
                                    onResultUnderExecutor(TransferEvent.ResponseSent)
                                }
                            }
                            onResultUnderExecutor(TransferEvent.Disconnected)
                        }.onFailure {
                            logger?.e(TAG, "An error occurred in dispatching", it)
                            onResultUnderExecutor(TransferEvent.Error(it))
                        }
                    }

                    else -> {
                        logger?.e(TAG, "${resolvedRequestObject.javaClass} not supported yet.")
                        onResultUnderExecutor("${resolvedRequestObject.javaClass} not supported yet.".err())
                    }
                }
            }
        }
    }

    private fun mDocVPTokenConsensus(
        deviceResponse: ByteArray,
        resolvedRequestObject: ResolvedRequestObject.OpenId4VPAuthorization
    ): Consensus.PositiveConsensus.VPTokenConsensus {
        val vpToken =
            Base64.getUrlEncoder().withoutPadding()
                .encodeToString(deviceResponse)
        logger?.d(TAG, "VpToken: $vpToken")

        val presentationDefinition = resolvedRequestObject.presentationDefinition
        return Consensus.PositiveConsensus.VPTokenConsensus(
            VpToken.MsoMdoc(
                vpToken,
                Base64URL.encode(mdocGeneratedNonce),
            ),
            presentationSubmission = PresentationSubmission(
                id = Id(UUID.randomUUID().toString()),
                definitionId = presentationDefinition.id,
                presentationDefinition.inputDescriptors.map { inputDescriptor ->
                    DescriptorMap(
                        inputDescriptor.id,
                        "mso_mdoc",
                        path = JsonPath.jsonPath("$")!!
                    )
                }
            )
        )
    }

    private fun sdJwtVPTokenConsensus(
        deviceResponse: ByteArray,
        resolvedRequestObject: ResolvedRequestObject.OpenId4VPAuthorization
    ): Consensus.PositiveConsensus.VPTokenConsensus {
        val vpToken =
            Base64.getUrlEncoder().withoutPadding()
                .encodeToString(deviceResponse)
        logger?.d(TAG, "VpToken: $vpToken")

        val presentationDefinition = resolvedRequestObject.presentationDefinition
        return Consensus.PositiveConsensus.VPTokenConsensus(
            VpToken.Generic(
                vpToken,
            ),
            presentationSubmission = PresentationSubmission(
                id = Id(UUID.randomUUID().toString()),
                definitionId = presentationDefinition.id,
                presentationDefinition.inputDescriptors.map { inputDescriptor ->
                    DescriptorMap(
                        inputDescriptor.id,
                        "vc_sd_jwt",
                        path = JsonPath.jsonPath("$")!!
                    )
                }
            )
        )
    }

    /**
     * Closes the OpenId4VpManager
     */
    fun close() {
        logger?.d(TAG, "close")
        resolvedRequestObject = null
        mdocGeneratedNonce = null
    }

    private fun OpenId4VpConfig.toSiopOpenId4VPConfig(): SiopOpenId4VPConfig {
        return SiopOpenId4VPConfig(
            jarmConfiguration = JarmConfiguration.Encryption(
                supportedAlgorithms = this.encryptionAlgorithms.map {
                    JWEAlgorithm.parse(it.name)
                },
                supportedMethods = this.encryptionMethods.map {
                    EncryptionMethod.parse(it.name)
                },
            ),
            supportedClientIdSchemes = this.clientIdSchemes.map { clientIdScheme ->
                when (clientIdScheme) {
                    is ClientIdScheme.Preregistered -> SupportedClientIdScheme.Preregistered(
                        clientIdScheme.preregisteredVerifiers.associate { verifier ->
                            verifier.clientId to PreregisteredClient(
                                verifier.clientId,
                                verifier.legalName,
                                JWSAlgorithm.RS256 to JwkSetSource.ByReference(
                                    URI("${verifier.verifierApi}/wallet/public-keys.json")
                                )
                            )
                        }
                    )

                    is ClientIdScheme.X509SanDns ->
                        SupportedClientIdScheme.X509SanDns(responseGenerator.getOpenid4VpX509CertificateTrust())

                    is ClientIdScheme.X509SanUri ->
                        SupportedClientIdScheme.X509SanUri(responseGenerator.getOpenid4VpX509CertificateTrust())
                }
            }
        )
    }

    private fun ResolvedRequestObject.OpenId4VPAuthorization.toSessionTranscript(): SessionTranscriptBytes {
        val clientId = this.client.id
        val responseUri = when (this.responseMode) {
            is ResponseMode.DirectPost -> (this.responseMode as ResponseMode.DirectPost?)?.responseURI?.toString() ?: ""
            is ResponseMode.DirectPostJwt -> (this.responseMode as ResponseMode.DirectPostJwt?)?.responseURI?.toString() ?: ""
            else -> ""
        }

        val nonce = this.nonce
        val mdocGeneratedNonce = Openid4VpUtils.generateMdocGeneratedNonce().also {
            mdocGeneratedNonce = it
        }

        val sessionTranscriptBytes = Openid4VpUtils.generateSessionTranscript(
            clientId,
            responseUri,
            nonce,
            mdocGeneratedNonce
        )
        logger?.d(
            TAG,
            "Session Transcript: ${
                Hex.toHexString(sessionTranscriptBytes)
            }, for clientId: $clientId, responseUri: $responseUri, nonce: $nonce, mdocGeneratedNonce: $mdocGeneratedNonce"
        )
        return sessionTranscriptBytes
    }

    private fun List<TransferEvent.Listener>.onTransferEvent(event: TransferEvent) {
        forEach { it.onTransferEvent(event) }
    }

    private fun String.err(): TransferEvent.Error {
        return TransferEvent.Error(Throwable(this))
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

    companion object {
        internal const val TAG = "OpenId4vpManager"
    }
}