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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.wallet.document.credential.ProofOfPossessionSigner
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.securearea.KeyLockedException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BatchProofSignerTest {

    @Test
    fun `test authenticate returns correct operation`() = runTest {

        val signers: List<ProofOfPossessionSigner> = (0..1).map {
            val keys = Crypto.createEcPrivateKey(EcCurve.P256)
            mockk<ProofOfPossessionSigner> {
                coEvery { getKeyInfo() } returns mockk {
                    every { algorithm } returns keys.curve.defaultSigningAlgorithmFullySpecified
                    every { publicKey } returns keys.publicKey
                }
            }
        }

        val proofSigner = BatchProofSigner(signers, null)
        val batchSignOperation = proofSigner.authenticate()
        assertEquals(2, batchSignOperation.operations.size)
    }


    @Test
    fun `test authenticate throws exception when key is locked`() = runTest {
        val key = Crypto.createEcPrivateKey(EcCurve.P256)
        val signers: List<ProofOfPossessionSigner> = listOf(
            mockk {
                coEvery { signPoP(any(), null) } throws KeyLockedException()
                coEvery { getKeyInfo() } returns mockk {
                    every { algorithm } returns Algorithm.ESP256
                    every { publicKey } returns key.publicKey
                }
            }
        )
        val proofSigner = BatchProofSigner(signers, null)
        val batchSignOperation = proofSigner.authenticate()
        assertEquals(1, batchSignOperation.operations.size)
        val exception = assertFailsWith(KeyLockedException::class) {
            batchSignOperation.operations.first().function.sign(byteArrayOf())
        }
        assertEquals(exception, proofSigner.keyLockedException)
    }

    @Test
    fun `test javaAlgorithm throws exception when algorithm is not supported`() = runTest {
        val signers: List<ProofOfPossessionSigner> = listOf(
            mockk {
                coEvery { getKeyInfo() } returns mockk {
                    every { algorithm } returns Algorithm.SHA256
                }
            },
            mockk() // No need to mock the second signer as it's not used in this test
        )

        val exception = assertFailsWith(IllegalArgumentException::class) {
            val proofSigner = BatchProofSigner(signers, null)
        }
        assertEquals("Unsupported algorithm: ${Algorithm.SHA256}", exception.message)

    }

    @Test
    fun `test algorithm property has the correct algorithm identifier`() {
        val signers: List<ProofOfPossessionSigner> = listOf(
            mockk {
                coEvery { getKeyInfo() } returns mockk {
                    every { algorithm } returns Algorithm.ESP256
                }
            },
            mockk() // No need to mock the second signer as it's not used in this test
        )
        val proofSigner = BatchProofSigner(signers, null)

        assertEquals(Algorithm.ESP256, proofSigner.algorithm)
    }
}
