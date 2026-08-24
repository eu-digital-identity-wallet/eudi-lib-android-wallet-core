/*
 * Copyright (c) 2024-2026 European Commission
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

import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIdentifier
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExpandToIssuanceItemsTest {

    private val configIdA = CredentialConfigurationIdentifier("config-a")
    private val configIdB = CredentialConfigurationIdentifier("config-b")

    private fun createOfferedDocument(
        configId: CredentialConfigurationIdentifier,
    ): Offer.OfferedDocument = mockk {
        every { configurationIdentifier } returns configId
    }

    private fun createOffer(
        vararg offeredDocuments: Offer.OfferedDocument,
    ): Offer = mockk {
        every { this@mockk.offeredDocuments } returns offeredDocuments.toList()
    }

    @Test
    fun `null credentialIdentifiers produces ConfigurationBased for each offered document`() {
        val docA = createOfferedDocument(configIdA)
        val docB = createOfferedDocument(configIdB)
        val offer = createOffer(docA, docB)

        val result = expandToIssuanceItems(offer, credentialIdentifiers = null)

        assertEquals(2, result.size)
        assertIs<IssuanceItem.ConfigurationBased>(result[0])
        assertEquals(docA, result[0].offeredDocument)
        assertIs<IssuanceItem.ConfigurationBased>(result[1])
        assertEquals(docB, result[1].offeredDocument)
    }

    @Test
    fun `empty map produces ConfigurationBased for each offered document`() {
        val docA = createOfferedDocument(configIdA)
        val offer = createOffer(docA)

        val result = expandToIssuanceItems(offer, credentialIdentifiers = emptyMap())

        assertEquals(1, result.size)
        assertIs<IssuanceItem.ConfigurationBased>(result[0])
        assertEquals(docA, result[0].offeredDocument)
    }

    @Test
    fun `empty list for a config produces ConfigurationBased`() {
        val docA = createOfferedDocument(configIdA)
        val offer = createOffer(docA)

        val result = expandToIssuanceItems(
            offer,
            credentialIdentifiers = mapOf(configIdA to emptyList()),
        )

        assertEquals(1, result.size)
        assertIs<IssuanceItem.ConfigurationBased>(result[0])
    }

    @Test
    fun `single identifier produces one IdentifierBased`() {
        val docA = createOfferedDocument(configIdA)
        val offer = createOffer(docA)
        val credId = CredentialIdentifier("degree-1")

        val result = expandToIssuanceItems(
            offer,
            credentialIdentifiers = mapOf(configIdA to listOf(credId)),
        )

        assertEquals(1, result.size)
        val item = assertIs<IssuanceItem.IdentifierBased>(result[0])
        assertEquals(docA, item.offeredDocument)
        assertEquals(credId, item.credentialIdentifier)
    }

    @Test
    fun `multiple identifiers for one config produces one IdentifierBased per identifier`() {
        val docA = createOfferedDocument(configIdA)
        val offer = createOffer(docA)
        val credId1 = CredentialIdentifier("degree-civil")
        val credId2 = CredentialIdentifier("degree-electrical")
        val credId3 = CredentialIdentifier("degree-mechanical")

        val result = expandToIssuanceItems(
            offer,
            credentialIdentifiers = mapOf(configIdA to listOf(credId1, credId2, credId3)),
        )

        assertEquals(3, result.size)
        result.forEachIndexed { index, item ->
            val identifierBased = assertIs<IssuanceItem.IdentifierBased>(item)
            assertEquals(docA, identifierBased.offeredDocument)
        }
        assertEquals(credId1, (result[0] as IssuanceItem.IdentifierBased).credentialIdentifier)
        assertEquals(credId2, (result[1] as IssuanceItem.IdentifierBased).credentialIdentifier)
        assertEquals(credId3, (result[2] as IssuanceItem.IdentifierBased).credentialIdentifier)
    }

    @Test
    fun `mixed configs - some with identifiers, some without`() {
        val docA = createOfferedDocument(configIdA)
        val docB = createOfferedDocument(configIdB)
        val offer = createOffer(docA, docB)
        val credId1 = CredentialIdentifier("id-1")
        val credId2 = CredentialIdentifier("id-2")

        val result = expandToIssuanceItems(
            offer,
            credentialIdentifiers = mapOf(
                configIdA to listOf(credId1, credId2),
                // configIdB not present in map → falls back to ConfigurationBased
            ),
        )

        assertEquals(3, result.size)
        // config-a expanded to 2 IdentifierBased items
        assertIs<IssuanceItem.IdentifierBased>(result[0])
        assertEquals(credId1, (result[0] as IssuanceItem.IdentifierBased).credentialIdentifier)
        assertIs<IssuanceItem.IdentifierBased>(result[1])
        assertEquals(credId2, (result[1] as IssuanceItem.IdentifierBased).credentialIdentifier)
        // config-b falls back to ConfigurationBased
        assertIs<IssuanceItem.ConfigurationBased>(result[2])
        assertEquals(docB, result[2].offeredDocument)
    }

    @Test
    fun `identifiers for unknown config are ignored - only offered documents matter`() {
        val docA = createOfferedDocument(configIdA)
        val offer = createOffer(docA)
        val unknownConfigId = CredentialConfigurationIdentifier("unknown-config")

        val result = expandToIssuanceItems(
            offer,
            credentialIdentifiers = mapOf(
                unknownConfigId to listOf(CredentialIdentifier("id-1")),
            ),
        )

        assertEquals(1, result.size)
        assertIs<IssuanceItem.ConfigurationBased>(result[0])
        assertEquals(docA, result[0].offeredDocument)
    }
}
