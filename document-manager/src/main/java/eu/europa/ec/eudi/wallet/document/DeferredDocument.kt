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

import org.multipaz.document.Document

/**
 * Represents a Deferred Document in the EUDI Wallet.
 *
 * A Deferred Document extends the [UnsignedDocument] class and represents a document that is
 * waiting to be issued. It contains additional related data necessary for the issuance process.
 * Deferred documents are created when a document issuance request has been initiated but the
 * actual issuance is pending or will happen at a later time.
 *
 * @property id The unique identifier of the document
 * @property name The human-readable name of the document
 * @property format The format specification of the document (e.g., MsoMdoc, SdJwtVc)
 * @property documentManagerId The identifier of the [DocumentManager] that manages this document
 * @property createdAt The timestamp when the document was created in the wallet
 * @property issuerMetadata The document metadata provided by the issuer
 * @property relatedData Additional data associated with this document that is needed for
 *                       the deferred issuance process (e.g., issuance request identifiers or tokens)
 */
class DeferredDocument(
    baseDocument: Document,
    val relatedData: ByteArray,
) : UnsignedDocument(baseDocument)

