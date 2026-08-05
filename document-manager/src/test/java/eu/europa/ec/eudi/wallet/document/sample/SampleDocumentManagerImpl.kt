/*
 * Copyright (c) 2023-2025 European Commission
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
package eu.europa.ec.eudi.wallet.document.sample

import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocType
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.credential.IssuerProvidedCredential
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import kotlinx.coroutines.runBlocking
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jetbrains.annotations.VisibleForTesting
import java.security.Security

/**
 * A [SampleDocumentManager] implementation that composes a [DocumentManager] and provides methods to load sample data.
 *
 * @constructor
 * @param delegate [DocumentManager] implementation to delegate the document management operations
 */
class SampleDocumentManagerImpl(
    @get:VisibleForTesting internal val delegate: DocumentManager
) : DocumentManager by delegate, SampleDocumentManager {

    init {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    override fun loadMdocSampleDocuments(
        sampleData: ByteArray,
        createSettings: CreateDocumentSettings,
        documentNamesMap: Map<DocType, String>?
    ): Outcome<List<DocumentId>> {
        try {
            val documentIds = mutableListOf<DocumentId>()
            val cbor = CBORObject.DecodeFromBytes(sampleData)
            val documents = cbor["documents"]
            documents.values.forEach { documentCbor ->
                val docType = documentCbor["docType"].AsString()
                val issuerSigned = documentCbor["issuerSigned"]
                val nameSpaces = issuerSigned["nameSpaces"]
                val unsignedDocument = createDocument(
                    format = MsoMdocFormat(docType),
                    createSettings = createSettings
                ).getOrThrow()
                unsignedDocument.name = documentNamesMap?.get(docType) ?: docType
                val data = runBlocking {
                    unsignedDocument.getPoPSigners().map {
                        val authKey = it.getKeyInfo()
                        val mso = generateMso(DIGEST_ALG, docType, authKey.publicKey, nameSpaces)
                        val issuerAuth = signMso(mso)
                        IssuerProvidedCredential(
                            publicKeyAlias = authKey.alias,
                            data = generateData(nameSpaces, issuerAuth)
                        )
                    }

                }

                val issuedDocument = storeIssuedDocument(unsignedDocument, data).getOrThrow()
                documentIds.add(issuedDocument.id)
            }
            return Outcome.success(documentIds)
        } catch (e: Exception) {
            return Outcome.failure(e)
        }
    }

    private companion object {
        private const val DIGEST_ALG = "sha-256"
    }
}
