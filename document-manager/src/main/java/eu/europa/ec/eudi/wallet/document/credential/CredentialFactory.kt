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

package eu.europa.ec.eudi.wallet.document.credential

import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.document.Document
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.sdjwt.credential.KeyBoundSdJwtVcCredential
import org.multipaz.securearea.SecureArea

/**
 * Factory interface for creating credentials based on document format.
 * Provides functionality to create appropriate credential types for different document formats.
 */
fun interface CredentialFactory {
    /**
     * Creates a list of credentials for a document based on the given format and settings.
     *
     * @param format the document format that determines the type of credential to create
     * @param document the document that will contain the credentials
     * @param createDocumentSettings settings for creating the document credentials
     * @param secureArea the secure area for storing cryptographic keys
     * @return a list of credentials bound to the secure area
     */
    suspend fun createCredentials(
        format: DocumentFormat,
        document: Document,
        createDocumentSettings: CreateDocumentSettings,
        secureArea: SecureArea,
    ): Pair<List<SecureAreaBoundCredential>, String?>

    companion object {
        /**
         * Creates an appropriate credential factory implementation based on the document format.
         *
         * @param domain the domain for the credentials
         * @param format the document format that determines which factory implementation to use
         * @return a credential factory implementation specific to the document format
         */
        operator fun invoke(domain: String, format: DocumentFormat): CredentialFactory {
            return when (format) {
                is MsoMdocFormat -> MdocCredentialFactory(domain)
                is SdJwtVcFormat -> SdJwtVcCredentialFactory(domain)
            }
        }
    }
}

/**
 * Implementation of CredentialFactory for creating ISO/IEC 18013-5 mobile driving license (mDL) credentials.
 *
 * @property domain the domain for the credentials
 */
class MdocCredentialFactory(
    val domain: String,
) : CredentialFactory {
    /**
     * Creates mDL credentials for a document based on MSO mDOC format settings.
     *
     * @param format the document format, must be an instance of MsoMdocFormat
     * @param document the document that will contain the credentials
     * @param createDocumentSettings settings for creating the document credentials
     * @param secureArea the secure area for storing cryptographic keys
     * @return a list of MdocCredential instances bound to the document
     * @throws IllegalArgumentException if the provided format is not an instance of MsoMdocFormat
     */
    override suspend fun createCredentials(
        format: DocumentFormat,
        document: Document,
        createDocumentSettings: CreateDocumentSettings,
        secureArea: SecureArea
    ): Pair<List<MdocCredential>, String?> {
        require(format is MsoMdocFormat) {
            "Expected ${MsoMdocFormat::class}"
        }

        return MdocCredential.createBatch(
            numberOfCredentials = createDocumentSettings.credentialPolicy.numberOfCredentials,
            document = document,
            domain = domain,
            secureArea = secureArea,
            docType = format.docType,
            createKeySettings = createDocumentSettings.createKeySettings
        )
    }
}

/**
 * Implementation of CredentialFactory for creating SD-JWT VC (Selective Disclosure JWT Verifiable Credentials) according to RFC 9901.
 *
 * @property domain the domain for the credentials
 */
class SdJwtVcCredentialFactory(val domain: String) :
    CredentialFactory {
    /**
     * Creates SD-JWT VC credentials for a document based on SD-JWT VC format settings.
     *
     * @param format the document format, must be an instance of SdJwtVcFormat
     * @param document the document that will contain the credentials
     * @param createDocumentSettings settings for creating the document credentials
     * @param secureArea the secure area for storing cryptographic keys
     * @return a list of SdJwtVcCredential instances bound to the document
     * @throws IllegalArgumentException if the provided format is not an instance of SdJwtVcFormat
     */
    override suspend fun createCredentials(
        format: DocumentFormat,
        document: Document,
        createDocumentSettings: CreateDocumentSettings,
        secureArea: SecureArea
    ): Pair<List<KeyBoundSdJwtVcCredential>, String?> {
        require(format is SdJwtVcFormat) {
            "Expected ${SdJwtVcFormat::class}"
        }

        return KeyBoundSdJwtVcCredential.createBatch(
            numberOfCredentials = createDocumentSettings.credentialPolicy.numberOfCredentials,
            document = document,
            domain = domain,
            secureArea = secureArea,
            vct = format.vct,
            createKeySettings = createDocumentSettings.createKeySettings
        )

    }
}

