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
package eu.europa.ec.eudi.wallet.dcapi

import android.content.Context
import androidx.credentials.registry.provider.ClearCredentialRegistryRequest
import androidx.credentials.registry.provider.RegistryManager
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.internal.e
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [DCAPIIsoMdocRegistration] is responsible for registering MSO MDOC credentials for the
 * Digital Credential API (DCAPI).
 *
 * It collects all issued mdoc documents from the [DocumentManager] and hands them to an
 * [IsoMdocRegistry] (a [DigitalCredentialRegistry] subclass implementing the
 * `org-iso-mdoc` protocol per ISO/IEC TS 18013-7:2025 Annex C) which is then registered
 * with the system [RegistryManager].
 *
 * @property context Application context used by [IsoMdocRegistry] to load the
 *   bundled WASM matcher and resolve app name / locale.
 * @property documentManager The [DocumentManager] instance used to fetch issued documents.
 * @property logger Optional logger.
 * @property ioDispatcher Coroutine dispatcher used for I/O bound work.
 */

class DCAPIIsoMdocRegistration(
    private val context: Context,
    private val documentManager: DocumentManager,
    private var logger: Logger? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DCAPIRegistration {

    private val registryManager: RegistryManager by lazy {
        RegistryManager.create(context)
    }

    override suspend fun registerCredentials() {
        withContext(ioDispatcher) {
            try {
                val issuedMsoMdocDocuments = documentManager.getDocuments()
                    .filterIsInstance<IssuedDocument>()
                    .filter { it.format is MsoMdocFormat }

                // clear previously registered credentials
                registryManager.clearCredentialRegistry(
                    ClearCredentialRegistryRequest(isDeleteAll = true)
                )

                if (issuedMsoMdocDocuments.isEmpty()) {
                    logger?.d(TAG, "No mdoc documents to register; cleared existing registries")
                    return@withContext
                }

                val registry = IsoMdocRegistry(
                    context = context,
                    documents = issuedMsoMdocDocuments,
                    id = REGISTRY_ID,
                    logger = logger,
                    ioDispatcher = ioDispatcher
                )

                registryManager.registerCredentials(registry)
                logger?.d(TAG, "Registered ${issuedMsoMdocDocuments.size} mdoc credential(s)")
            } catch (e: Exception) {
                logger?.e(TAG, "Error during DCAPI registration", e)
            }
        }
    }

    override suspend fun unregisterCredentials() {
        withContext(ioDispatcher) {
            try {
                registryManager.clearCredentialRegistry(
                    ClearCredentialRegistryRequest(isDeleteAll = true)
                )
                logger?.d(TAG, "Unregistered all credentials")
            } catch (e: Exception) {
                logger?.e(TAG, "Error during DCAPI unregistration", e)
            }
        }
    }

    companion object {
        private const val TAG = "DCAPIIsoMdocRegistration"
        private const val REGISTRY_ID = "eudi-mdoc-registry-v1"
    }
}

interface DCAPIRegistration {
    suspend fun registerCredentials()
    suspend fun unregisterCredentials()
}