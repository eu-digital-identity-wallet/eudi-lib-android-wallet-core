package eu.europa.ec.eudi.wallet.transfer.openid4vp.responseGenerator

import android.os.Build
import android.security.keystore.KeyProperties
import android.util.AndroidException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.AsymmetricJWK
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.util.Base64URL
import eu.europa.ec.eudi.iso18013.transfer.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.DocItem
import eu.europa.ec.eudi.iso18013.transfer.DocRequest
import eu.europa.ec.eudi.iso18013.transfer.DocumentsResolver
import eu.europa.ec.eudi.iso18013.transfer.ReaderAuth
import eu.europa.ec.eudi.iso18013.transfer.RequestDocument
import eu.europa.ec.eudi.iso18013.transfer.RequestedDocumentData
import eu.europa.ec.eudi.iso18013.transfer.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.DeviceResponse
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseGenerator
import eu.europa.ec.eudi.openid4vp.legalName
import eu.europa.ec.eudi.sdjwt.HashAlgorithm
import eu.europa.ec.eudi.sdjwt.JsonPointer
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.KeyBindingSigner
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.SdJwtVerifier
import eu.europa.ec.eudi.sdjwt.asJwtVerifier
import eu.europa.ec.eudi.sdjwt.present
import eu.europa.ec.eudi.sdjwt.serializeWithKeyBinding
import eu.europa.ec.eudi.wallet.document.Constants.EU_PID_NAMESPACE
import eu.europa.ec.eudi.wallet.document.Constants.SDJWT_FORMAT
import eu.europa.ec.eudi.wallet.internal.Openid4VpX509CertificateTrust
import eu.europa.ec.eudi.wallet.issue.openid4vci.DocumentManagerSdJwt
import eu.europa.ec.eudi.wallet.keystore.KeyGenerator
import eu.europa.ec.eudi.wallet.keystore.KeyGeneratorImpl
import eu.europa.ec.eudi.wallet.transfer.openid4vp.OpenId4VpSdJwtRequest
import eu.europa.ec.eudi.wallet.util.ZKP_ISSUER_CERT
import eu.europa.ec.eudi.wallet.util.getECPublicKeyFromCert
import eu.europa.ec.eudi.wallet.util.parseCertificateFromSdJwt
import eu.europa.ec.eudi.wallet.zkp.network.ZKPClient
import kotlinx.coroutines.runBlocking
import software.tice.ZKPGenerator
import software.tice.ZKPProverSdJwt
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date

/**
 * OpenId4VpSdJwtResponseGeneratorImpl class is used for parsing a sdjwt request (Presentation Definition) and generating the DeviceResponse
 *
 * @param documentsResolver document manager instance
 */
class OpenId4VpSdJwtResponseGeneratorImpl(
    private val documentsResolver: DocumentsResolver,
) : ResponseGenerator<OpenId4VpSdJwtRequest>() {
    private var readerTrustStore: ReaderTrustStore? = null
    private val openid4VpX509CertificateTrust = Openid4VpX509CertificateTrust(readerTrustStore)

    private var zkpRequestId: String? = null
    private var requestNonce: String? = null
    private var audience: String? = null

    override fun createResponse(disclosedDocuments: DisclosedDocuments) = runBlocking {
        val disclosedDocument = disclosedDocuments.documents.first()

        val credentials = DocumentManagerSdJwt.getDocumentById(disclosedDocument.documentId)?.data
            ?: throw IllegalArgumentException()

        val sdJwt = getSdJwtFromCredentials(credentials)
        sdJwt.jwt

        val jsonPointer = disclosedDocument.docRequest.requestItems.mapNotNull { item ->
            JsonPointer.parse(item.elementIdentifier)
        }.toSet()

        val presentationSdJwt = sdJwt.present(jsonPointer)

        val key = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                KeyGeneratorImpl.getSigningKey(
                    KeyGenerator.SigningKeyConfig(
                        KeyProperties.AUTH_DEVICE_CREDENTIAL,
                        60
                    )
                )
            } catch (exception: java.security.KeyStoreException) {
                return@runBlocking ResponseResult.Failure(exception)
            }
        } else {
            return@runBlocking ResponseResult.Failure(AndroidException("Build version to low."))
        }

        val certificateFactory: CertificateFactory = try {
            CertificateFactory.getInstance("X.509")
        } catch (exception: CertificateException) {
            return@runBlocking ResponseResult.Failure(exception)
        }
        val certificate =
            try {
                certificateFactory.generateCertificate(ByteArrayInputStream(key.certificate.encoded)) as X509Certificate
            } catch (exception: CertificateException) {
                return@runBlocking ResponseResult.Failure(exception)
            }
        val ecKey = try {
            ECKey.parse(certificate)
        } catch (exception: com.nimbusds.jose.JOSEException) {
            return@runBlocking ResponseResult.Failure(exception)
        }

        val presentationJwt = presentationSdJwt!!.serializeWithKeyBinding(
            jwtSerializer = {
                if (zkpRequestId == null) it.first else {
                    runBlocking {
                        val zkpKey = getECPublicKeyFromCert(ZKP_ISSUER_CERT)
                        val prover = ZKPProverSdJwt(ZKPGenerator(zkpKey))
                        val challenge = ZKPClient().getChallenges(
                            zkpRequestId!!,
                            listOf(EU_PID_NAMESPACE to prover.createChallengeRequest(it.first)),
                        ).first().second
                        val zkpJwt = prover.answerChallenge(challenge, it.first)
                        zkpJwt
                    }
                }
            },
            hashAlgorithm = HashAlgorithm.SHA_256,
            keyBindingSigner = object : KeyBindingSigner {
                override val signAlgorithm: JWSAlgorithm = JWSAlgorithm.ES256
                override val publicKey: AsymmetricJWK = ecKey.toPublicJWK()
                override fun getJCAContext(): JCAContext = JCAContext()

                @Throws(java.security.SignatureException::class)
                override fun sign(p0: JWSHeader?, p1: ByteArray?): Base64URL =
                    Base64URL(KeyGeneratorImpl.sign(key.privateKey, p1 ?: ByteArray(0)))
            },
            claimSetBuilderAction = {
                claim(CLAIM_NONCE, requestNonce)
                claim(CLAIM_IAT, Date().time)
                claim(CLAIM_AUD, audience)
            }
        )

        return@runBlocking ResponseResult.Success(DeviceResponse(presentationJwt.toByteArray()))
    }

    override fun setReaderTrustStore(readerTrustStore: ReaderTrustStore) = apply {
        openid4VpX509CertificateTrust.setReaderTrustStore(readerTrustStore)
        this.readerTrustStore = readerTrustStore
    }

    internal fun getOpenid4VpX509CertificateTrust() = openid4VpX509CertificateTrust

    override fun parseRequest(request: OpenId4VpSdJwtRequest): RequestedDocumentData {
        requestNonce = request.openId4VPAuthorization.nonce
        audience = request.openId4VPAuthorization.client.id

        zkpRequestId = request.requestId
        val inputDescriptors =
            request.openId4VPAuthorization.presentationDefinition.inputDescriptors
                .filter { inputDescriptor ->
                    inputDescriptor.format?.json?.contains(SDJWT_FORMAT) == true
                }

        if (inputDescriptors.isEmpty()) {
            throw IllegalArgumentException()
        }

        val requestedFields = inputDescriptors.associate { inputDescriptor ->
            inputDescriptor.id.value.trim() to inputDescriptor.constraints.fields()
                .map { fieldConstraint ->
                    val elementIdentifier = fieldConstraint.paths.first().value
                        .replace(".", "/")
                        .drop(1)

                    EU_PID_NAMESPACE to elementIdentifier
                }.groupBy({ it.first }, { it.second })
                .mapValues { (_, values) -> values.toList() }
                .toMap()
        }

        val readerAuth = openid4VpX509CertificateTrust.getTrustResult()?.let { (chain, isTrusted) ->
            ReaderAuth(
                byteArrayOf(0),
                true, /* It is always true as siop-openid4vp library validates it internally and returns a fail status */
                chain,
                isTrusted,
                request.openId4VPAuthorization.client.legalName() ?: "",
            )
        }

        return createRequestedDocumentData(requestedFields, readerAuth)
    }

    private fun createRequestedDocumentData(
        requestedFields: Map<String, Map<String, List<String>>>,
        readerAuth: ReaderAuth?,
    ): RequestedDocumentData {
        val requestedDocuments = mutableListOf<RequestDocument>()
        requestedFields.forEach { document ->
            // create doc item
            val docItems = mutableListOf<DocItem>()
            document.value.forEach { (namespace, elementIds) ->
                elementIds.forEach { elementId ->
                    docItems.add(DocItem(namespace, elementId))
                }
            }
            val docType = document.key

            requestedDocuments.addAll(
                documentsResolver.resolveDocuments(
                    DocRequest(
                        docType,
                        docItems,
                        readerAuth
                    )
                )
            )
        }
        return RequestedDocumentData(requestedDocuments)
    }

    private suspend fun getSdJwtFromCredentials(credentials: String): SdJwt.Issuance<JwtAndClaims> {
        val certificate = parseCertificateFromSdJwt(credentials)

        val ecKey = ECKey.parse(certificate)
        val jwtSignatureVerifier = ECDSAVerifier(ecKey).asJwtVerifier()

        return SdJwtVerifier.verifyIssuance(
            jwtSignatureVerifier,
            credentials
        ).getOrThrow()
    }

    private companion object {
        const val CLAIM_NONCE = "nonce"
        const val CLAIM_IAT = "iat"
        const val CLAIM_AUD = "aud"
    }
}