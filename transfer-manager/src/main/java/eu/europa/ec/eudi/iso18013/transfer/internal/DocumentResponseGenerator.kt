/*
 * Copyright (c) 2024-2026 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer.internal

import eu.europa.ec.eudi.wallet.document.ElementIdentifier
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.NameSpace
import eu.europa.ec.eudi.wallet.document.credential.CredentialIssuedData
import eu.europa.ec.eudi.wallet.document.credential.getIssuedData
import kotlinx.coroutines.withContext
import org.multipaz.document.DocumentRequest
import org.multipaz.document.NameSpacedData
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.mdoc.response.DocumentGenerator
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.prompt.Reason
import org.multipaz.securearea.KeyUnlockData

/**
 * Helpers for generating ISO 18013-5 Document responses from an [IssuedDocument].
 *
 * Public so other library modules (notably `wallet-core`'s OpenID4VP MSO mdoc VP generation)
 * can reuse the credential-policy / unlock plumbing without re-implementing it.
 */
object DocumentResponseGenerator {

    /**
     * Generate a device response for a given document, consuming the credential according
     * to the document's [eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy].
     *
     * Document must be in MsoMdocFormat and not have an invalidated key.
     *
     * @param document the document to generate the response for
     * @param transcript the transcript to use for the response
     * @param elements the elements to include in the response
     * @param keyUnlockData the key unlock data for unlocking the document key if needed
     * @throws IllegalArgumentException if the document format is not MsoMdocFormat, the document key is invalidated,
     * @throws org.multipaz.securearea.KeyLockedException if the document key is locked and cannot be unlocked
     */
    suspend fun generate(
        document: IssuedDocument,
        transcript: ByteArray,
        elements: Map<NameSpace, List<ElementIdentifier>>? = null,
        keyUnlockData: KeyUnlockData? = null
    ): ByteArray = withContext(keyUnlockData.asProvider()) {
        document.consumingCredential {
            require(this is MdocCredential) { "Document must be in MsoMdocFormat" }
            generateDocumentBytes(
                credential = this,
                transcript = transcript,
                elements = elements
            )
        }.getOrThrow()
    }

    /**
     * Generate a device response for a given document without consuming the credential.
     * Uses [IssuedDocument.findCredential] instead of [IssuedDocument.consumingCredential],
     * bypassing the [eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy]
     * checks. This is used for ZK proof generation where the credential key is not sent
     * to the verifier and should not count against usage limits.
     *
     * @param document the document to generate the response for
     * @param transcript the transcript to use for the response
     * @param elements the elements to include in the response
     * @param keyUnlockData the key unlock data for unlocking the document key if needed
     * @throws IllegalArgumentException if the document format is not MsoMdocFormat
     * @throws IllegalStateException if no credential is found
     * @throws org.multipaz.securearea.KeyLockedException if the document key is locked and cannot be unlocked
     */
    suspend fun generateWithoutConsuming(
        document: IssuedDocument,
        transcript: ByteArray,
        elements: Map<NameSpace, List<ElementIdentifier>>? = null,
        keyUnlockData: KeyUnlockData? = null
    ): ByteArray = withContext(keyUnlockData.asProvider()) {
        val credential = checkNotNull(document.findCredential()) {
            "No credential found in the issued document"
        }
        require(credential is MdocCredential) { "Document must be in MsoMdocFormat" }
        generateDocumentBytes(
            credential = credential,
            transcript = transcript,
            elements = elements
        )
    }

    /**
     * Generates the signed device response bytes from an [MdocCredential].
     * Shared logic used by both [generate] and [generateWithoutConsuming].
     */
    private suspend fun generateDocumentBytes(
        credential: MdocCredential,
        transcript: ByteArray,
        elements: Map<NameSpace, List<ElementIdentifier>>?
    ): ByteArray {
        val credentialIssuedData =
            credential.getIssuedData<CredentialIssuedData.MsoMdoc>()
        val (nameSpacedData, staticAuthData) = credentialIssuedData.getOrThrow()
        val dataElements = (elements ?: nameSpacedData.nameSpaceNames.associateWith {
            nameSpacedData.getDataElementNames(it)
        }).flatMap { (nameSpace, elementIdentifiers) ->
            elementIdentifiers.map { elementIdentifier ->
                DocumentRequest.DataElement(nameSpace, elementIdentifier, false)
            }
        }
        val request = DocumentRequest(dataElements)

        val mergedIssuerNamespaces = MdocUtil.mergeIssuerNamesSpaces(
            request = request,
            documentData = nameSpacedData,
            staticAuthData = staticAuthData
        )

        return DocumentGenerator(credential.docType, staticAuthData.issuerAuth, transcript)
            .setIssuerNamespaces(mergedIssuerNamespaces)
            .setDeviceNamespacesSignature(
                dataElements = NameSpacedData.Builder().build(),
                secureArea = credential.secureArea,
                keyAlias = credential.alias,
                unlockReason = Reason.Unspecified
            )
            .generate()
    }

    /**
     * Suspend extension wrapping [generate] in a [Result] for safe error handling.
     */
    suspend fun IssuedDocument.generateDocumentResponse(
        transcript: ByteArray,
        elements: Map<NameSpace, List<ElementIdentifier>>? = null,
        keyUnlockData: KeyUnlockData? = null
    ): Result<ByteArray> = runCatching { generate(this, transcript, elements, keyUnlockData) }

    /**
     * Suspend extension wrapping [generateWithoutConsuming] in a [Result] for safe error handling.
     */
    suspend fun IssuedDocument.generateDocumentResponseWithoutConsuming(
        transcript: ByteArray,
        elements: Map<NameSpace, List<ElementIdentifier>>? = null,
        keyUnlockData: KeyUnlockData? = null
    ): Result<ByteArray> =
        runCatching { generateWithoutConsuming(this, transcript, elements, keyUnlockData) }
}