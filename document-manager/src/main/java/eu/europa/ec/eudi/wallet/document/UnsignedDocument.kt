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

import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigner
import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigners
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.internal.createdAt
import eu.europa.ec.eudi.wallet.document.internal.documentManagerId
import eu.europa.ec.eudi.wallet.document.internal.documentName
import eu.europa.ec.eudi.wallet.document.internal.format
import eu.europa.ec.eudi.wallet.document.internal.issuerMetaData
import eu.europa.ec.eudi.wallet.document.internal.toCoseBytes
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import kotlinx.coroutines.runBlocking
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.securearea.KeyInfo
import org.multipaz.securearea.SecureArea
import java.time.Instant
import kotlin.time.toJavaInstant

/**
 * Represents a document that has been created but not yet fully issued.
 *
 * An UnsignedDocument contains one or more credentials that have not been certified
 * by an issuer. It provides access to these credentials through proof-of-possession
 * signers, which can be used during issuance procedures to prove possession of
 * private keys and receive the issuer's certification.
 *
 * @property id The unique identifier of the document
 * @property name The human-readable name of the document
 * @property format The format specification of the document (e.g., MsoMdoc, SdJwtVc)
 * @property documentManagerId The identifier of the DocumentManager that manages this document
 * @property createdAt The timestamp when the document was created in the wallet
 * @property issuerMetadata The document metadata provided by the issuer, if available
 */
open class UnsignedDocument(
    internal val baseDocument: org.multipaz.document.Document,
) : Document {

    override val format: DocumentFormat
        get() = baseDocument.format

    override val id: DocumentId
        get() = baseDocument.identifier

    override var name: String = baseDocument.documentName

    override val documentManagerId: String
        get() = baseDocument.documentManagerId

    override val createdAt: Instant
        get() = baseDocument.createdAt.toJavaInstant()

    override val issuerMetadata: IssuerMetadata?
        get() = baseDocument.issuerMetaData

    /**
     * Creates proof of possession signers for the document credentials.
     *
     * This method filters all available credentials associated with this document,
     * selects those bound to the secure area, excludes any invalidated keys,
     * and creates appropriate signers that can prove possession of the corresponding
     * private keys. Only credentials that belong to the current document manager are included.
     *
     * @return A collection of [ProofOfPossessionSigner] instances that can be used to sign data
     *         during document issuance
     */
    suspend fun getPoPSigners(): ProofOfPossessionSigners {
        return baseDocument.getPendingCredentials()
            .filterIsInstance<SecureAreaBoundCredential>()
            .filterNot { it.secureArea.getKeyInvalidated(it.alias) }
            .filter { it.domain == baseDocument.documentManagerId }
            .map { ProofOfPossessionSigner(it) }
            .let { ProofOfPossessionSigners(it) }
    }

    /**
     * Returns the number of valid credentials associated with this document.
     *
     * For UnsignedDocument, this represents the number of proof-of-possession signers
     * that can be created from the document's pending credentials. Only credentials that are
     * not invalidated and belong to the current document manager are counted.
     *
     * @return The number of valid credentials available for this document
     * @see getPoPSigners
     */
    override suspend fun credentialsCount(): Int {
        return getPoPSigners().size
    }

    // Deprecated properties and methods moved to the end of the file

    @Deprecated("Use getPoPSigners() instead to access credentials and their secure areas")
    override val secureArea: SecureArea
        get() = runBlocking { getPoPSigners().first().secureArea }

    @Deprecated("For unsigned documents, this is always false. No replacement needed.")
    override val isCertified: Boolean = false

    @Deprecated("Use getPoPSigners().first().getKeyInfo() instead to access credential key information")
    override val keyInfo: KeyInfo
        get() = runBlocking {
            getPoPSigners().first().getKeyInfo()
        }

    @Deprecated("Use getPoPSigners().first().getKeyInfo().alias instead to access credential key aliases")
    override val keyAlias: String
        get() = keyInfo.alias

    @Deprecated("Use getPoPSigners().first().getKeyInfo().publicKey.toCoseBytes instead to access public keys")
    override val publicKeyCoseBytes: ByteArray
        get() = keyInfo.publicKey.toCoseBytes

    @Deprecated("Use getPoPSigners() instead, which automatically filters out invalidated keys")
    override val isKeyInvalidated: Boolean
        get() = runBlocking {
            secureArea.getKeyInvalidated(keyAlias)
        }
}
