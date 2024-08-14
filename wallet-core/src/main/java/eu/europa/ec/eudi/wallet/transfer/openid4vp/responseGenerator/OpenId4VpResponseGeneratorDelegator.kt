package eu.europa.ec.eudi.wallet.transfer.openid4vp.responseGenerator

import android.content.Context
import com.android.identity.android.securearea.AndroidKeystoreSecureArea
import com.android.identity.android.storage.AndroidStorageEngine
import com.android.identity.storage.StorageEngine
import eu.europa.ec.eudi.iso18013.transfer.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.DocumentsResolver
import eu.europa.ec.eudi.iso18013.transfer.RequestedDocumentData
import eu.europa.ec.eudi.iso18013.transfer.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.Request
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openid4vp.OpenId4VpRequest
import eu.europa.ec.eudi.wallet.transfer.openid4vp.OpenId4VpSdJwtRequest


class OpenId4VpResponseGeneratorDelegator(
    private val mDocGenerator: OpenId4VpCBORResponseGeneratorImpl,
    private val sdJwtGenerator: OpenId4VpSdJwtResponseGeneratorImpl
) {
    sealed class FormatState(open val isZkp: Boolean) {
        data class Cbor(override val isZkp: Boolean) : FormatState(isZkp)
        data class SdJwt(override val isZkp: Boolean) : FormatState(isZkp)
    }

    companion object {
        public var formatState: FormatState = FormatState.Cbor(false)
            private set
    }

    fun createResponse(disclosedDocuments: DisclosedDocuments): ResponseResult {
        return when (formatState) {
            is FormatState.Cbor -> mDocGenerator.createResponse(disclosedDocuments)
            is FormatState.SdJwt -> sdJwtGenerator.createResponse(disclosedDocuments)
        }
    }

    fun setReaderTrustStore(readerTrustStore: ReaderTrustStore) {
        sdJwtGenerator.setReaderTrustStore(readerTrustStore)
        mDocGenerator.setReaderTrustStore(readerTrustStore)
    }

    internal fun getOpenid4VpX509CertificateTrust() = when (formatState) {
        is FormatState.Cbor -> mDocGenerator.getOpenid4VpX509CertificateTrust()
        is FormatState.SdJwt -> sdJwtGenerator.getOpenid4VpX509CertificateTrust()
    }

    fun parseRequest(request: Request): RequestedDocumentData {
        return when (request) {
            is OpenId4VpRequest -> {
                formatState = FormatState.Cbor(request.requestId != null)
                mDocGenerator.parseRequest(request)
            }

            is OpenId4VpSdJwtRequest -> {
                formatState = FormatState.SdJwt(request.requestId != null)
                sdJwtGenerator.parseRequest(request)
            }

            else -> {
                throw NotImplementedError(message = "Not supported: ${request::class.simpleName}")
            }
        }
    }

    class Builder(context: Context) {
        private val _context = context.applicationContext
        var documentsResolver: DocumentsResolver? = null
        var readerTrustStore: ReaderTrustStore? = null
        var logger: Logger? = null

        /**
         * Reader trust store that will be used to validate the certificate chain of the mdoc verifier
         *
         * @param readerTrustStore
         */
        fun readerTrustStore(readerTrustStore: ReaderTrustStore) =
            apply { this.readerTrustStore = readerTrustStore }

        fun build(): OpenId4VpResponseGeneratorDelegator {
            return documentsResolver?.let { documentsResolver ->
                val openId4VpCBORResponseGeneratorImpl = OpenId4VpCBORResponseGeneratorImpl(
                    documentsResolver,
                    storageEngine,
                    androidSecureArea,
                    logger
                ).apply {
                    readerTrustStore?.let { setReaderTrustStore(it) }
                }

                val openId4VpSdJwtResponseGeneratorImpl = OpenId4VpSdJwtResponseGeneratorImpl(
                    documentsResolver
                ).apply {
                    readerTrustStore?.let { setReaderTrustStore(it) }
                }

                OpenId4VpResponseGeneratorDelegator(
                    openId4VpCBORResponseGeneratorImpl,
                    openId4VpSdJwtResponseGeneratorImpl
                ).apply {
                    readerTrustStore?.let { setReaderTrustStore(it) }
                }
            } ?: throw IllegalArgumentException("documentResolver not set")
        }

        private val storageEngine: StorageEngine
            get() = AndroidStorageEngine.Builder(_context, _context.noBackupFilesDir)
                .setUseEncryption(true)
                .build()
        private val androidSecureArea: AndroidKeystoreSecureArea
            get() = AndroidKeystoreSecureArea(_context, storageEngine)
    }
}