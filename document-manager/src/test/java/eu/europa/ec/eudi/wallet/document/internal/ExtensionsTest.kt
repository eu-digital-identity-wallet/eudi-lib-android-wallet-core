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

package eu.europa.ec.eudi.wallet.document.internal

import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.multipaz.cbor.CborMap
import org.multipaz.cbor.Tstr
import org.multipaz.cbor.buildCborMap
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ExtensionsTest {

    @Test
    fun `test EcPublicKey toCoseBytes and toEcPublicKey`() {
        // Create a test public key
        val xCoordinate = ByteArray(32) { (it + 1).toByte() }
        val yCoordinate = ByteArray(32) { (it + 33).toByte() }
        val originalKey = EcPublicKeyDoubleCoordinate(EcCurve.P256, xCoordinate, yCoordinate)

        // Convert to COSE bytes
        val coseBytes = originalKey.toCoseBytes

        // Convert back to EcPublicKey
        val reconstructedKey = coseBytes.toEcPublicKey

        // Verify properties match
        assertEquals(originalKey.curve, reconstructedKey.curve)
        // Note: We can't directly compare EcPublicKey instances since they don't override equals,
        // but we can verify they have the same properties
        if (reconstructedKey is EcPublicKeyDoubleCoordinate) {
            assertTrue(xCoordinate.contentEquals(reconstructedKey.x))
            assertTrue(yCoordinate.contentEquals(reconstructedKey.y))
        } else {
            throw AssertionError("Reconstructed key is not of expected type")
        }
    }

    @Test
    fun `test ByteArray sdJwtVcString`() {
        val testBytes = "test.sd-jwt.payload".toByteArray(Charsets.US_ASCII)
        val sdJwtVcString = testBytes.sdJwtVcString

        assertEquals("test.sd-jwt.payload", sdJwtVcString)
    }

    @Test
    fun `test CredentialPolicy toDataItem and fromDataItem for OnceOnly`() {
        // Create policy without reissue trigger (legacy behavior)
        val policy = CreateDocumentSettings.CredentialPolicy.OnceOnly()

        // Convert to DataItem
        val dataItem = policy.toDataItem()

        // Ensure it's a map and has the expected type
        assertTrue(dataItem is CborMap)
        assertEquals(
            CreateDocumentSettings.CredentialPolicy.OnceOnly::class.java.name,
            (dataItem as CborMap)["type"].asTstr
        )

        // Reconstruct policy from DataItem
        val reconstructedPolicy = CreateDocumentSettings.CredentialPolicy.fromDataItem(dataItem)

        // Verify it matches the original
        assertEquals(policy, reconstructedPolicy)
    }

    @Test
    fun `test CredentialPolicy toDataItem and fromDataItem for OnceOnly with reissueTriggerUnused`() {
        // Create policy with reissue trigger
        val policy = CreateDocumentSettings.CredentialPolicy.OnceOnly(reissueTriggerUnused = 3)

        // Convert to DataItem
        val dataItem = policy.toDataItem()

        // Ensure it's a map and has the expected type
        assertTrue(dataItem is CborMap)
        assertEquals(
            CreateDocumentSettings.CredentialPolicy.OnceOnly::class.java.name,
            (dataItem as CborMap)["type"].asTstr
        )

        // Reconstruct policy from DataItem
        val reconstructedPolicy = CreateDocumentSettings.CredentialPolicy.fromDataItem(dataItem)

        // Verify it matches the original
        assertEquals(policy, reconstructedPolicy)
        assertIs<CreateDocumentSettings.CredentialPolicy.OnceOnly>(reconstructedPolicy)
        assertEquals(3, (reconstructedPolicy as CreateDocumentSettings.CredentialPolicy.OnceOnly).reissueTriggerUnused)
    }

    @Test
    fun `test CredentialPolicy toDataItem and fromDataItem for RotatingBatch`() {
        // Create policy without reissue trigger (legacy behavior)
        val policy = CreateDocumentSettings.CredentialPolicy.RotatingBatch()

        // Convert to DataItem
        val dataItem = policy.toDataItem()

        // Ensure it's a map and has the expected type
        assertTrue(dataItem is CborMap)
        assertEquals(
            CreateDocumentSettings.CredentialPolicy.RotatingBatch::class.java.name,
            (dataItem as CborMap)["type"].asTstr
        )

        // Reconstruct policy from DataItem
        val reconstructedPolicy = CreateDocumentSettings.CredentialPolicy.fromDataItem(dataItem)

        // Verify it matches the original
        assertEquals(policy, reconstructedPolicy)
    }

    @Test
    fun `test CredentialPolicy fromDataItem throws for invalid types`() {
        // Create a DataItem with an unknown policy type
        val invalidDataItem = buildCborMap {
            put("type", "unknown.policy.type")
        }

        // Verify it throws an exception
        assertFailsWith<IllegalArgumentException> {
            CreateDocumentSettings.CredentialPolicy.fromDataItem(invalidDataItem)
        }

        // Test with non-map DataItem
        val nonMapDataItem = Tstr("not a map")
        assertFailsWith<IllegalArgumentException> {
            CreateDocumentSettings.CredentialPolicy.fromDataItem(nonMapDataItem)
        }
    }

    @Test
    fun `test DocumentFormat toDataItem and fromDataItem for MsoMdocFormat`() {
        // Create format
        val docType = "eu.europa.ec.eudiw.pid.1"
        val format = MsoMdocFormat(docType)

        // Convert to DataItem
        val dataItem = format.toDataItem()

        // Ensure it's a map and has the expected docType
        assertTrue(dataItem is CborMap)
        assertEquals(docType, (dataItem as CborMap)["docType"].asTstr)

        // Reconstruct format from DataItem
        val reconstructedFormat = DocumentFormat.fromDataItem(dataItem)

        // Verify it's the right type and matches the original
        assertTrue(reconstructedFormat is MsoMdocFormat)
        assertEquals(format, reconstructedFormat)
    }

    @Test
    fun `test DocumentFormat toDataItem and fromDataItem for SdJwtVcFormat`() {
        // Create format
        val vct = "urn:eu.europa.ec.eudi.pid.1"
        val format = SdJwtVcFormat(vct)

        // Convert to DataItem
        val dataItem = format.toDataItem()

        // Ensure it's a map and has the expected vct
        assertTrue(dataItem is CborMap)
        assertEquals(vct, (dataItem as CborMap)["vct"].asTstr)

        // Reconstruct format from DataItem
        val reconstructedFormat = DocumentFormat.fromDataItem(dataItem)

        // Verify it's the right type and matches the original
        assertTrue(reconstructedFormat is SdJwtVcFormat)
        assertEquals(format, reconstructedFormat)
    }

    @Test
    fun `test DocumentFormat fromDataItem throws for invalid formats`() {
        // Create a DataItem with neither docType nor vct
        val invalidDataItem = buildCborMap {
            put("someOtherKey", "value")
        }

        // Verify it throws an exception
        assertFailsWith<IllegalArgumentException> {
            DocumentFormat.fromDataItem(invalidDataItem)
        }

        // Test with non-map DataItem
        val nonMapDataItem = Tstr("not a map")
        assertFailsWith<IllegalArgumentException> {
            DocumentFormat.fromDataItem(nonMapDataItem)
        }
    }
}
