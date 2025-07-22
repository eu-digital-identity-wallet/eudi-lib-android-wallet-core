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

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.multipaz.crypto.Algorithm

class DPoPSignerTest {

    @Test
    fun factoryInvokeCreatesDPoPSignerWithDefaultAlgorithm() {
        val signer = DPoPSigner().getOrNull()
        assertTrue(signer is DPoPSigner)
        assertEquals(Algorithm.ESP256, signer!!.algorithm)
    }

    @Test
    fun createDPoPSignerWithCustomAlgorithm() {
        val signer = DPoPSigner(Algorithm.ESP384).getOrNull()
        assertNotNull(signer)
        assertEquals(Algorithm.ESP384, signer!!.algorithm)
        assertEquals("SHA384withECDSA", signer.javaAlgorithm)
    }

    @Test
    fun createDPoPSignerWithInvalidAlgorithmFails() {
        // RS256 doesn't have a curve, so it should fail
        val result2 = DPoPSigner(Algorithm.RS256)
        assertTrue(result2.isFailure)
    }

    @Test
    fun javaAlgorithmReturnsCorrectAlgorithmIdentifier() {
        val esp256Signer = DPoPSigner(Algorithm.ESP256).getOrNull()
        assertNotNull(esp256Signer)
        assertEquals("SHA256withECDSA", esp256Signer!!.javaAlgorithm)

        val es384Signer = DPoPSigner(Algorithm.ESP384).getOrNull()
        assertNotNull(es384Signer)
        assertEquals("SHA384withECDSA", es384Signer!!.javaAlgorithm)
    }

    @Test
    fun authenticateCreatesValidSignOperation() = runBlocking {
        val signer = DPoPSigner().getOrNull()
        assertNotNull(signer)

        val signOperation = signer!!.authenticate()

        // Verify the sign operation was created successfully
        assertNotNull(signOperation)
        assertNotNull(signOperation.function)
        assertNotNull(signOperation.publicMaterial)

        // Verify the public material is a valid JWK
        val jwk = signOperation.publicMaterial

        // Check that the JWK contains expected fields for an EC key
        assertEquals("EC", jwk.keyType.value)
        assertNotNull(jwk.toJSONObject()["crv"])
        assertNotNull(jwk.toJSONObject()["x"])
        assertNotNull(jwk.toJSONObject()["y"])
    }

    @Test
    fun signOperationCanSignData() = runBlocking {
        val signer = DPoPSigner().getOrNull()
        assertNotNull(signer)

        val signOperation = signer!!.authenticate()
        val dataToSign = "test data".toByteArray()

        // The sign function should produce a signature without throwing
        val signature = try {
            signOperation.function.sign(dataToSign)
            true
        } catch (e: Exception) {
            fail("Signing operation failed: ${e.message}")
            false
        }

        assertTrue(signature)
    }

    @Test
    fun releaseDoesNotThrowException() = runBlocking {
        val signer = DPoPSigner().getOrNull()
        assertNotNull(signer)

        val signOperation = signer!!.authenticate()

        // The release function should not throw
        try {
            signer.release(signOperation)
            assertTrue(true) // If we reached here, no exception was thrown
        } catch (e: Exception) {
            fail("Release operation failed: ${e.message}")
        }

        // Releasing null should also not throw
        try {
            signer.release(null)
            assertTrue(true) // If we reached here, no exception was thrown
        } catch (e: Exception) {
            fail("Release null operation failed: ${e.message}")
        }
    }

    @Test
    fun differentSignersProduceDifferentKeys() = runBlocking {
        val signer1 = DPoPSigner().getOrNull()
        val signer2 = DPoPSigner().getOrNull()

        assertNotNull(signer1)
        assertNotNull(signer2)

        val signOp1 = signer1!!.authenticate()
        val signOp2 = signer2!!.authenticate()

        val jwk1 = signOp1.publicMaterial
        val jwk2 = signOp2.publicMaterial

        // Two different signers should produce different keys
        // Compare the x and y coordinates of the EC keys
        val x1 = jwk1.toJSONObject()["x"].toString()
        val y1 = jwk1.toJSONObject()["y"].toString()
        val x2 = jwk2.toJSONObject()["x"].toString()
        val y2 = jwk2.toJSONObject()["y"].toString()

        assertTrue(x1 != x2 || y1 != y2)
    }

    @Test
    fun sameDataDifferentSignersDifferentSignatures() = runBlocking {
        val signer1 = DPoPSigner().getOrNull()
        val signer2 = DPoPSigner().getOrNull()

        assertNotNull(signer1)
        assertNotNull(signer2)

        val signOp1 = signer1!!.authenticate()
        val signOp2 = signer2!!.authenticate()

        val dataToSign = "test data".toByteArray()

        val signature1 = signOp1.function.sign(dataToSign)
        val signature2 = signOp2.function.sign(dataToSign)

        // Different signers should produce different signatures for the same data
        assertTrue(!signature1.contentEquals(signature2))
    }
}