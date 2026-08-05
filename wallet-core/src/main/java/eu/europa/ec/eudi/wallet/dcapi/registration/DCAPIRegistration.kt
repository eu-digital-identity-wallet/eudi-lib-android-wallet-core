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

@file:JvmMultifileClass
package eu.europa.ec.eudi.wallet.dcapi.registration

import android.content.Context
import androidx.credentials.registry.provider.ClearCredentialRegistryRequest
import androidx.credentials.registry.provider.RegistryManager
import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.internal.i
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Default [DCAPIRegistration] that registers the wallet's issued documents with the Digital
 * Credential API (DCAPI), so they can be offered to verifiers.
 *
 * It collects the issued documents from the [DocumentManager] and registers them with the system
 * registry. Which document formats are registered depends on [supportedProtocols]: MSO mdoc is
 * always registered, while SD-JWT VC is registered only when an OpenID4VP protocol is enabled,
 * since it can only be presented over OpenID4VP.
 *
 * @property context application context.
 * @property documentManager the document manager used to fetch the issued documents.
 * @property supportedProtocols the protocols the wallet processes; determines which document
 *   formats are registered.
 * @property logger optional logger.
 * @property ioDispatcher coroutine dispatcher used for I/O bound work.
 */

class DefaultDCAPIRegistration(
    private val context: Context,
    private val documentManager: DocumentManager,
    private val supportedProtocols: List<DCAPIProtocol>,
    private var logger: Logger? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DCAPIRegistration {

    private val registryManager: RegistryManager by lazy {
        RegistryManager.create(context)
    }

    override suspend fun registerCredentials() {
        withContext(ioDispatcher) {
            try {
                logger?.d(TAG, "registerCredentials() started, documentManager=${documentManager::class.qualifiedName}@${System.identityHashCode(documentManager).toString(16)}")
                // SD-JWT VC can only be presented over OpenID4VP; if no OpenID4VP protocol is
                // enabled, omit SD-JWT documents from the registry so they never surface in the
                // OS picker. mdoc is presentable over org-iso-mdoc and/or OpenID4VP, so it is
                // always registered.
                val openId4VpEnabled = supportedProtocols.any { it.isOpenId4Vp }
                val issuedDocuments = documentManager.getDocuments()
                    .filterIsInstance<IssuedDocument>()
                    .filter { document ->
                        when (document.format) {
                            is MsoMdocFormat -> true
                            is SdJwtVcFormat -> openId4VpEnabled
                        }
                    }
                logger?.d(TAG, "Found ${issuedDocuments.size} issued documents for DC API (openId4VpEnabled=$openId4VpEnabled): ${issuedDocuments.map { it.id }}")

                logger?.d(TAG, "Calling clearCredentialRegistry(isDeleteAll=true)...")
                registryManager.clearCredentialRegistry(
                    ClearCredentialRegistryRequest(isDeleteAll = true)
                )
                logger?.d(TAG, "clearCredentialRegistry completed")

                if (issuedDocuments.isEmpty()) {
                    logger?.i(TAG, "No documents to register for DC API; cleared existing registrations")
                    return@withContext
                }

                val registries = DCAPICredentialRegistry(
                    context = context,
                    documents = issuedDocuments,
                    id = REGISTRY_ID,
                    logger = logger,
                    ioDispatcher = ioDispatcher,
                    protocols = supportedProtocols
                )

                logger?.d(TAG, "Registering ${registries.size} DC API registr(ies)...")
                registries.forEach { registry ->
                    registryManager.registerCredentials(registry)
                    logger?.d(TAG, "Registered registry id=${registry.id}")
                }
                logger?.i(TAG, "Registered ${issuedDocuments.size} document(s) for DC API")
            } catch (e: Exception) {
                logger?.e(TAG, "Error during DCAPI registration: ${e::class.simpleName}: ${e.message}", e)
            }
        }
    }

    override suspend fun unregisterCredentials() {
        withContext(ioDispatcher) {
            try {
                registryManager.clearCredentialRegistry(
                    ClearCredentialRegistryRequest(isDeleteAll = true)
                )
                logger?.i(TAG, "Unregistered all DC API documents")
            } catch (e: Exception) {
                logger?.e(TAG, "Error during DCAPI unregistration", e)
            }
        }
    }

    companion object {
        private const val TAG = "DefaultDCAPIRegistration"
        private const val REGISTRY_ID = "eudi-credential-registry-v1"
    }
}

/**
 * Registers and unregisters the wallet's documents with the Digital Credential API (DCAPI).
 *
 * Provide a custom implementation to control how the wallet's documents are exposed to verifiers;
 * otherwise [DefaultDCAPIRegistration] is used.
 */
interface DCAPIRegistration {
    /** Registers the wallet's currently issued documents with the system registry. */
    suspend fun registerCredentials()

    /** Removes all of the wallet's documents from the system registry. */
    suspend fun unregisterCredentials()
}