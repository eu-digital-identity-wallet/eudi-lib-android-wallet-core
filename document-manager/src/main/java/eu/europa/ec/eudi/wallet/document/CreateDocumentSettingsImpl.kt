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

package eu.europa.ec.eudi.wallet.document

import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy
import org.multipaz.securearea.CreateKeySettings

/**
 * Implementation of [CreateDocumentSettings] interface that provides configuration for document creation.
 *
 * This class encapsulates all necessary parameters required when creating digital documents
 * through the [DocumentManager.createDocument] method. It specifies where document keys should be
 * stored, how they should be created, and defines credential management policies.
 *
 * @property secureAreaIdentifier The secure area identifier where the document's keys should be stored.
 *                               This identifier must reference an existing secure area in the system.
 * @property createKeySettings The configuration settings for key creation within the specified secure area.
 *                            These settings control properties such as key algorithms, sizes, and other
 *                            security parameters required by the secure area implementation.
 * @property credentialPolicy Defines how credentials are managed after use. Controls whether credentials are
 *                           used once and deleted or rotated through multiple uses. The number of credentials
 *                           to create is determined by the policy's [CredentialPolicy.numberOfCredentials].
 *                           Defaults to [CredentialPolicy.RotatingBatch].
 *
 * @see CreateDocumentSettings The interface this class implements
 * @see DocumentManager For usage in document creation operations
 * @see CredentialPolicy For available credential management policies
 */
data class CreateDocumentSettingsImpl(
    override val secureAreaIdentifier: String,
    override val createKeySettings: CreateKeySettings,
    override val credentialPolicy: CredentialPolicy = CredentialPolicy.RotatingBatch(),
) : CreateDocumentSettings
