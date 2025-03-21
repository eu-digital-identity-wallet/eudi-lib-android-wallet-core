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

package eu.europa.ec.eudi.wallet.transfer

import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.prex.InputDescriptorId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.transfer.openId4vp.FORMAT_MSO_MDOC
import eu.europa.ec.eudi.wallet.transfer.openId4vp.FORMAT_SD_JWT_VC
import eu.europa.ec.eudi.wallet.transfer.openId4vp.constructDescriptorsMap
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals


class ConstructDescriptorsMapTest {
    @Test
    fun `test constructDescriptorsMap with 2 different format types`() {
        val inputDescriptorMap = mapOf(
            InputDescriptorId("descriptor1") to listOf("doc1", "doc3"),
            InputDescriptorId("descriptor2") to listOf("doc2", "doc4")
        )

        val issuedDocument1 = mockk<IssuedDocument> {
            every { id } returns "doc1"
            every { format } returns mockk<MsoMdocFormat>()
        }

        val issuedDocument2 = mockk<IssuedDocument> {
            every { id } returns "doc2"
            every { format } returns mockk<SdJwtVcFormat>()
        }

        val verifiablePresentations = listOf(
            Pair(issuedDocument1, mockk<VerifiablePresentation.Generic>(relaxed = true)),
            Pair(issuedDocument2, mockk<VerifiablePresentation.Generic>(relaxed = true))
        )



        val result = constructDescriptorsMap(inputDescriptorMap, verifiablePresentations)

        assertEquals(2, result.size)
        assertEquals("descriptor1", result[0].id.value)
        assertEquals("descriptor2", result[1].id.value)
        assertEquals(FORMAT_MSO_MDOC, result[0].format)
        assertEquals(FORMAT_SD_JWT_VC, result[1].format)
        assertEquals("$[0]", result[0].path.value)
        assertEquals("$[1]", result[1].path.value)
    }

    @Test
    fun `test constructDescriptorsMap with 1 document`() {
        val inputDescriptorMap = mapOf(
            InputDescriptorId("descriptor1") to listOf("doc1", "doc3"),
            InputDescriptorId("descriptor2") to listOf("doc2", "doc4")
        )


        val issuedDocument2 = mockk<IssuedDocument> {
            every { id } returns "doc2"
            every { format } returns mockk<SdJwtVcFormat>()
        }

        val verifiablePresentations = listOf(
            Pair(issuedDocument2, mockk<VerifiablePresentation.Generic>(relaxed = true)),
        )



        val result = constructDescriptorsMap(inputDescriptorMap, verifiablePresentations)

        assertEquals(1, result.size)
        assertEquals("descriptor2", result[0].id.value)
        assertEquals(FORMAT_SD_JWT_VC, result[0].format)
        assertEquals("$", result[0].path.value)
    }

    @Test
    fun `test constructDescriptorsMap throws when inputDescriptor is available for the presented documents`() {
        val inputDescriptorMap = mapOf(
            InputDescriptorId("descriptor1") to listOf("doc1", "doc3"),
            InputDescriptorId("descriptor2") to listOf("doc4")
        )


        val issuedDocument1 = mockk<IssuedDocument> {
            every { id } returns "doc1"
            every { format } returns mockk<MsoMdocFormat>()
        }

        val issuedDocument2 = mockk<IssuedDocument> {
            every { id } returns "doc2"
            every { format } returns mockk<SdJwtVcFormat>()
        }

        val verifiablePresentations = listOf(
            Pair(issuedDocument1, mockk<VerifiablePresentation.Generic>(relaxed = true)),
            Pair(issuedDocument2, mockk<VerifiablePresentation.Generic>(relaxed = true))
        )

        assertThrows(IllegalArgumentException::class.java) {
            constructDescriptorsMap(inputDescriptorMap, verifiablePresentations)
        }
    }
}
