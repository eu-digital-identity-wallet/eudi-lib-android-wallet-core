/*
 * Copyright (c) 2023-2025 European Commission
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

import com.upokecenter.cbor.CBORObject
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.multipaz.document.NameSpacedData
import kotlin.test.Test


class IssuedDocumentExtensionsTest {
    @Test
    fun testDocumentCborToJson() {
        val nameSpacedData = NameSpacedData.Builder()
            .putEntryNumber("namespace1", "element1", 1)
            .putEntryString("namespace1", "element2", "AQID")
            .putEntryString("namespace2", "element3", "value3")
            .putEntry("namespace2", "element4", CBORObject.NewMap().apply {
                Add("subelement1", CBORObject.FromObjectAndTag("2023-11-09T00:01:02Z", 0))
                Add("subelement2", 5.4f)
            }.EncodeToBytes())
            .putEntry("namespace3", "element1", CBORObject.NewArray().apply {
                Add(1)
                Add("string")
                Add("AQID")
            }.EncodeToBytes())
            .build()

        // Create a mock MsoMdocData with the nameSpacedData
        val mdocData = mockk<MsoMdocData>()
        every { mdocData.nameSpacedDataDecoded } returns mapOf(
            "namespace1" to mapOf(
                "element1" to 1,
                "element2" to "AQID"
            ),
            "namespace2" to mapOf(
                "element3" to "value3",
                "element4" to mapOf(
                    "subelement1" to "2023-11-09T00:01:02Z",
                    "subelement2" to 5.4
                )
            ),
            "namespace3" to mapOf(
                "element1" to listOf(1, "string", "AQID")
            )
        )

        // Create a mock IssuedDocument
        val issuedDocument = mockk<IssuedDocument>()
        every { issuedDocument.data } returns mdocData

        // Test the extension property
        val json = issuedDocument.nameSpacedDataJSONObject

        Assert.assertEquals(3, json.keys().asSequence().toList().size)
        Assert.assertEquals(2, json.getJSONObject("namespace1").keys().asSequence().toList().size)
        Assert.assertEquals(2, json.getJSONObject("namespace2").keys().asSequence().toList().size)
        Assert.assertEquals(1, json.getJSONObject("namespace3").keys().asSequence().toList().size)

        Assert.assertEquals(1, json.getJSONObject("namespace1").getInt("element1"))
        Assert.assertEquals("AQID", json.getJSONObject("namespace1").getString("element2"))

        Assert.assertEquals("value3", json.getJSONObject("namespace2").getString("element3"))
        Assert.assertEquals(
            "2023-11-09T00:01:02Z",
            json.getJSONObject("namespace2").getJSONObject("element4").getString("subelement1")
        )
        Assert.assertEquals(
            5.4,
            json.getJSONObject("namespace2").getJSONObject("element4").getDouble("subelement2"),
            0.0001
        )

        Assert.assertEquals(1, json.getJSONObject("namespace3").getJSONArray("element1").get(0))
        Assert.assertEquals(
            "string",
            json.getJSONObject("namespace3").getJSONArray("element1").get(1)
        )
        Assert.assertEquals(
            "AQID",
            json.getJSONObject("namespace3").getJSONArray("element1").get(2)
        )
    }
}
