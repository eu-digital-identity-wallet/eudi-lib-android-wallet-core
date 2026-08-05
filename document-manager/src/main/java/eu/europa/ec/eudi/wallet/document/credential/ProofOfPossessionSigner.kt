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

import eu.europa.ec.eudi.wallet.document.internal.asProvider
import kotlinx.coroutines.withContext
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.crypto.EcSignature
import org.multipaz.prompt.Reason
import org.multipaz.securearea.KeyInfo
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.SecureArea

/**
 * A collection of [ProofOfPossessionSigner] instances.
 *
 * This class delegates all collection operations to the underlying list of signers.
 *
 * @property list The list of [ProofOfPossessionSigner] instances to delegate to.
 */
class ProofOfPossessionSigners(private val list: List<ProofOfPossessionSigner>) :
    List<ProofOfPossessionSigner> by list

/**
 * Interface for creating Proof of Possession (PoP) signatures.
 *
 * Implementations of this interface are responsible for generating cryptographic
 * signatures that prove possession of a private key.
 */
interface ProofOfPossessionSigner {
    /**
     * The alias of the key used for signing.
     */
    suspend fun getKeyInfo(): KeyInfo

    /**
     * The secure area associated with the key.
     */
    val secureArea: SecureArea

    /**
     * The alias of the key used for signing.
     */
    val keyAlias: String

    /**
     * Signs the provided data to create a Proof of Possession signature.
     *
     * @param dataToSign The data bytes to be signed.
     * @param keyUnlockData Optional data required to unlock the key for signing operations.
     * @return An [EcSignature] containing the signature data.
     */
    suspend fun signPoP(dataToSign: ByteArray, keyUnlockData: KeyUnlockData?): EcSignature

    companion object {
        operator fun invoke(credential: SecureAreaBoundCredential): ProofOfPossessionSigner {
            return ProofOfPossessionSignerImpl(credential)
        }
    }
}

/**
 * Default implementation of [ProofOfPossessionSigner] that uses a [SecureAreaBoundCredential].
 *
 * This implementation delegates signing operations to the secure area associated with the credential.
 *
 * @property credential The credential bound to a secure area that will be used for signing.
 */
class ProofOfPossessionSignerImpl(
    private val credential: SecureAreaBoundCredential
) : ProofOfPossessionSigner {

    override suspend fun getKeyInfo(): KeyInfo {
        return credential.secureArea.getKeyInfo(credential.alias)
    }

    override val keyAlias: String
        get() = credential.alias

    override val secureArea: SecureArea
        get() = credential.secureArea

    /**
     * Creates a Proof of Possession signature by delegating to the secure area
     * associated with the credential.
     *
     * @param dataToSign The data bytes to be signed.
     * @param keyUnlockData Optional data required to unlock the key for signing operations.
     * @return An [EcSignature] containing the signature data.
     */
    override suspend fun signPoP(
        dataToSign: ByteArray,
        keyUnlockData: KeyUnlockData?
    ): EcSignature {
        val provider = keyUnlockData.asProvider()
        return withContext(provider) {
            secureArea.sign(
                alias = keyAlias,
                dataToSign = dataToSign,
                unlockReason = Reason.Unspecified
            )
        }
    }
}
