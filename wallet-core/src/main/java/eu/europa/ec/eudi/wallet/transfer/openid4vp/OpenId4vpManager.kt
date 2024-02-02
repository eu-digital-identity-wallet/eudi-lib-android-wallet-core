/*
 *  Copyright (c) 2023 European Commission
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
import android.util.Log
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.iso18013.transfer.Request
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.DispatchOutcome
import eu.europa.ec.eudi.openid4vp.JarmConfiguration
import eu.europa.ec.eudi.openid4vp.JwkSetSource
import eu.europa.ec.eudi.openid4vp.PreregisteredClient
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.SiopOpenId4VPConfig
import eu.europa.ec.eudi.openid4vp.SiopOpenId4Vp
import eu.europa.ec.eudi.openid4vp.SupportedClientIdScheme
import eu.europa.ec.eudi.openid4vp.asException
import eu.europa.ec.eudi.prex.ClaimFormat
import eu.europa.ec.eudi.prex.DescriptorMap
import eu.europa.ec.eudi.prex.Id
import eu.europa.ec.eudi.prex.JsonPath
import eu.europa.ec.eudi.prex.PresentationSubmission
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.internal.OpenId4VpUtil
import eu.europa.ec.eudi.wallet.internal.Openid4VpX509CertificateTrust
import eu.europa.ec.eudi.wallet.internal.mainExecutor
import eu.europa.ec.eudi.wallet.util.CBOR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bouncycastle.util.encoders.Hex
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.Executor

/**
 * OpenId4vp manager. This class is used to manage the OpenId4vp transfer method. It is used to resolve the request uri and send the response.
 *
 * Example:
 * ```
 * val openId4vpManager = OpenId4vpManager(
 *    context,
 *    OpenId4VpConfig.Builder()
 *             .withClientIdSchemes(
 *             listOf(
 *                 ClientIdScheme.Preregistered(
 *                     listOf(
 *                         PreregisteredVerifier(
 *                             "Verifier", "https://example.com"
 *                         )
 *                     )
 *                 ),
 *                 ClientIdScheme.X509SanDns
 *             ))
 *             .withEncryptionAlgorithms(listOf(EncryptionAlgorithm.ECDH_ES))
 *             .withEncryptionMethods(listOf(EncryptionMethod.A128CBC_HS256))
 *             .build(),
 *    documentManager
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
 *          val response = EudiWallet.createResponse(disclosedDocuments)
 *          openId4vpManager.sendResponse(response)
 *      }
 *   }
 * }
 * openId4vpManager.addTransferEventListener(transferEventListener)
 *
 * ```
 * @param context the application context
 * @param openId4VpConfig the configuration for OpenId4Vp
 * @param documentManager the document manager used for issuing documents
 */

private const val TAG = "OpenId4vpManager"

class OpenId4vpManager(
    context: Context,
    openId4VpConfig: OpenId4VpConfig,
    private val documentManager: DocumentManager
) : TransferEvent.Listenable {

    private val appContext = context.applicationContext
    private val ioScope = CoroutineScope(Job() + Dispatchers.IO)
    private var executor: Executor? = null

    private var transferEventListeners: MutableList<TransferEvent.Listener> = mutableListOf()
    private val onResultUnderExecutor = { result: TransferEvent ->
        (executor ?: appContext.mainExecutor()).execute {
            Log.d(TAG, "onResultUnderExecutor $result")
            transferEventListeners.onTransferEvent(result)
        }
    }

    private var readerTrustStore: ReaderTrustStore? = null
    private var resolvedRequestObject: ResolvedRequestObject? = null
    private val openid4VpX509CertificateTrust = Openid4VpX509CertificateTrust(readerTrustStore)
    private val siopOpenId4Vp = SiopOpenId4Vp(createSiopOpenId4VpConfig(openId4VpConfig))

    /**
     * Set a ReaderTrustStore (optionally)
     */
    fun setReaderTrustStore(readerTrustStore: ReaderTrustStore) = apply {
        this.readerTrustStore = readerTrustStore
        this.openid4VpX509CertificateTrust.setReaderTrustStore(readerTrustStore)
    }

    /**
     * Setting the `executor` is optional and defines the executor that will be used to
     * execute the callback. If the `executor` is not defined, the callback will be executed on the
     * main thread.
     * @param Executor the executor to use for callbacks. If null, the main executor will be used.
     */
    fun setExecutor(executor: Executor) {
        this.executor = executor
    }

    private fun createSiopOpenId4VpConfig(openId4VpConfig: OpenId4VpConfig): SiopOpenId4VPConfig {
        return SiopOpenId4VPConfig(
            jarmConfiguration = JarmConfiguration.Encryption(
                supportedAlgorithms = openId4VpConfig.encryptionAlgorithms.map {
                    JWEAlgorithm.parse(it.name)
                },
                supportedMethods = openId4VpConfig.encryptionMethods.map {
                    EncryptionMethod.parse(it.name)
                },
            ),
            supportedClientIdSchemes = openId4VpConfig.clientIdSchemes.map { clientIdScheme ->
                when (clientIdScheme) {
                    is ClientIdScheme.Preregistered -> SupportedClientIdScheme.Preregistered(
                        clientIdScheme.preregisteredVerifiers.associate {
                            it.clientId to PreregisteredClient(
                                it.clientId, JWSAlgorithm.RS256 to JwkSetSource.ByReference(
                                    URI("${it.verifierApi}/wallet/public-keys.json")
                                )
                            )
                        }
                    )

                    is ClientIdScheme.X509SanDns ->
                        SupportedClientIdScheme.X509SanDns(openid4VpX509CertificateTrust)

                    is ClientIdScheme.X509SanUri ->
                        SupportedClientIdScheme.X509SanUri(openid4VpX509CertificateTrust)
                }
            }
        )
    }

    /**
     * Resolve a request uri
     *
     * @param openid4VPURI
     */
    fun resolveRequestUri(openid4VPURI: String) {
        Log.d(
            TAG,
            "Resolve request uri: ${URLDecoder.decode(openid4VPURI, StandardCharsets.UTF_8.name())}"
        )
        ioScope.launch {
            onResultUnderExecutor(TransferEvent.Connecting)
            runCatching { siopOpenId4Vp.resolveRequestUri(openid4VPURI) }.onSuccess { resolution ->
                when (resolution) {
                    is Resolution.Invalid -> {
                        Log.e(TAG, "Resolution.Invalid", resolution.error.asException())
                        onResultUnderExecutor(TransferEvent.Error(resolution.error.asException()))
                    }

                    is Resolution.Success -> {
                        Log.d(TAG, "Resolution.Success")
                        resolution.requestObject
                            .also { resolvedRequestObject = it }
                            .let { requestObject ->
                                when (requestObject) {
                                    is ResolvedRequestObject.OpenId4VPAuthorization -> {
                                        Log.d(TAG, "OpenId4VPAuthorization Request received")
                                        onResultUnderExecutor(
                                            TransferEvent.RequestReceived(
                                                requestObject.asRequest()
                                            )
                                        )
                                    }

                                    is ResolvedRequestObject.SiopAuthentication -> {
                                        Log.w(TAG, "SiopAuthentication Request received")
                                        onResultUnderExecutor("SiopAuthentication request received, not supported yet.".err())
                                    }

                                    is ResolvedRequestObject.SiopOpenId4VPAuthentication -> {
                                        Log.w(TAG, "SiopOpenId4VPAuthentication Request received")
                                        onResultUnderExecutor("SiopAuthentication request received, not supported yet.".err())
                                    }

                                    else -> {
                                        Log.w(TAG, "Unknown request received")
                                        onResultUnderExecutor("Unknown request received".err())
                                    }
                                }
                            }
                    }
                }
            }.onFailure {
                Log.e(TAG, "An error occurred resolving request uri: $openid4VPURI", it)
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

        Log.d(TAG, "Device Response to send (hex): ${Hex.toHexString(deviceResponse)}")
        Log.d(TAG, "Device Response to send (cbor): ${CBOR.cborPrettyPrint(deviceResponse)}")

        ioScope.launch {
            resolvedRequestObject?.let { resolvedRequestObject ->
                when (resolvedRequestObject) {
                    is ResolvedRequestObject.OpenId4VPAuthorization -> {
                        val presentationDefinition =
                            (resolvedRequestObject).presentationDefinition
                        val inputDescriptor =
                            presentationDefinition.inputDescriptors.first()
                        val vpToken =
                            Base64.getUrlEncoder().withoutPadding().encodeToString(deviceResponse)
                        Log.d(TAG, "VpToken: $vpToken")
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
                        runCatching { siopOpenId4Vp.dispatch(resolvedRequestObject, consensus) }.onSuccess { dispatchOutcome ->
                            when (dispatchOutcome) {
                                is DispatchOutcome.VerifierResponse.Accepted -> {
                                    Log.d(
                                        TAG,
                                        "VerifierResponse Accepted with redirectUri: $dispatchOutcome.redirectURI"
                                    )
                                    onResultUnderExecutor(TransferEvent.ResponseSent)
                                    dispatchOutcome.redirectURI?.let {
                                        onResultUnderExecutor(TransferEvent.Redirect(it))
                                    }
                                }

                                is DispatchOutcome.VerifierResponse.Rejected -> {
                                    Log.d(TAG, "VerifierResponse Rejected")
                                    onResultUnderExecutor("DispatchOutcome: VerifierResponse Rejected".err())
                                }

                                is DispatchOutcome.RedirectURI -> {
                                    Log.d(TAG, "VerifierResponse RedirectURI")
                                    onResultUnderExecutor(TransferEvent.ResponseSent)
                                }
                            }
                            onResultUnderExecutor(TransferEvent.Disconnected)
                        }.onFailure {
                            Log.e(TAG, "An error occurred in dispatching", it)
                            onResultUnderExecutor(TransferEvent.Error(it))
                        }
                    }
                    else -> {
                        Log.e(TAG, "${resolvedRequestObject.javaClass} not supported yet.")
                        onResultUnderExecutor("${resolvedRequestObject.javaClass} not supported yet.".err())
                    }
                }
            }
        }
    }

    /**
    * Closes the OpenId4VpManager
    */
    fun close() {
        Log.d(TAG, "close")
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
            documentManager, presentationDefinition, openid4VpX509CertificateTrust.getReaderAuth()
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