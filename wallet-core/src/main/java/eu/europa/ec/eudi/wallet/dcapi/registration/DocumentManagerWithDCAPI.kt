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

package eu.europa.ec.eudi.wallet.dcapi.registration

import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.Outcome
import eu.europa.ec.eudi.wallet.document.ProofOfDeletion
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.credential.IssuerProvidedCredential
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.multipaz.context.applicationContext

/**
 * A [DocumentManager] wrapper that keeps the Digital Credential API (DCAPI) registration in sync:
 * it re-registers the wallet's documents whenever a document is stored or deleted.
 *
 * @property delegate the document manager this class wraps and delegates to.
 * @property supportedProtocols the protocols the wallet processes; passed to the registration.
 * @property logger optional logger for logging events.
 * @property dcapiRegistration the registration used to register documents; when not provided,
 *   [DefaultDCAPIRegistration] is used.
 */

internal class DocumentManagerWithDCAPI(
    private val delegate: DocumentManager,
    private val supportedProtocols: List<DCAPIProtocol>,
    private val logger: Logger? = null,
    private val dcapiRegistration: DCAPIRegistration? = null
    ) : DocumentManager by delegate {

    private val registration: DCAPIRegistration by lazy {
        dcapiRegistration ?: DefaultDCAPIRegistration(
            context = applicationContext,
            documentManager = delegate,
            supportedProtocols = supportedProtocols,
            logger = logger
        )
    }

    override fun storeIssuedDocument(
        unsignedDocument: UnsignedDocument,
        issuerProvidedData: List<IssuerProvidedCredential>
    ): Outcome<IssuedDocument> {
        return delegate.storeIssuedDocument(unsignedDocument, issuerProvidedData).also {
            updateDCAPICredentials()
        }
    }

    override fun deleteDocumentById(documentId: DocumentId): Outcome<ProofOfDeletion?> {
        return delegate.deleteDocumentById(documentId).also {
            updateDCAPICredentials()
        }
    }

    private fun updateDCAPICredentials() {
        CoroutineScope(Dispatchers.IO).launch {
            registration.registerCredentials()
        }
    }
}