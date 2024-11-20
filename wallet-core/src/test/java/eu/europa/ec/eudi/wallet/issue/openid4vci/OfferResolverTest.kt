/*
 * Copyright (c) 2024 European Commission
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

import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.CredentialOfferRequestResolver
import io.ktor.client.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import java.net.URLEncoder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OfferResolverTest {

    private lateinit var ktorHttpClientFactory: () -> HttpClient
    private lateinit var credentialOffer: CredentialOffer
    private lateinit var offerResolver: OfferResolver
    private lateinit var credentialOfferResolver: CredentialOfferRequestResolver
    private val offerUri =
        "openid-credential-offer://?credential_offer_uri=${
            URLEncoder.encode(
                "https://offer.uri",
                "UTF-8"
            )
        }"

    @BeforeTest
    fun setUp() {
        ktorHttpClientFactory = mockk(relaxed = true)

        credentialOffer = mockk(relaxed = true)
        credentialOfferResolver = mockk()
        mockkObject(CredentialOfferRequestResolver.Companion)
        every { CredentialOfferRequestResolver.Companion.invoke(any()) } returns credentialOfferResolver

        offerResolver = OfferResolver(ktorHttpClientFactory)
    }


    @Test
    fun `resolve method when useCache is true should return offer if contained in cache`() {
        runTest {
            // Given
            val offer = mockk<Offer>()
            offerResolver.cache[offerUri] = offer

            // When
            val result = offerResolver.resolve(offerUri, true)

            // Then
            coVerify(exactly = 0) { credentialOfferResolver.resolve(offerUri) }
            assertTrue(result.isSuccess)
            assertEquals(offer, result.getOrNull())
        }
    }

    @Test
    fun `resolve method when useCache is true should resolve credentialOffer if not contained in cache and store it in cache`() {
        runTest {
            // Given
            coEvery { credentialOfferResolver.resolve(offerUri) } returns Result.success(
                credentialOffer
            )
            assertNull(offerResolver.cache[offerUri])
            val offer = DefaultOffer(credentialOffer)

            // When
            val result = offerResolver.resolve(offerUri, true)

            // Then
            coVerify(exactly = 1) { credentialOfferResolver.resolve(offerUri) }
            assertTrue(result.isSuccess)
            assertEquals(offer, result.getOrNull())
            assertEquals(offer, offerResolver.cache[offerUri])
        }
    }

    @Test
    fun `resolve method when useCache is false should resolve credentialOffer and store it in cache`() {
        runTest {
            // Given
            coEvery { credentialOfferResolver.resolve(offerUri) } returns Result.success(
                credentialOffer
            )
            val offer = DefaultOffer(credentialOffer)

            // When
            val result = offerResolver.resolve(offerUri, false)

            // Then
            coVerify(exactly = 1) { credentialOfferResolver.resolve(offerUri) }
            assertTrue(result.isSuccess)
            assertEquals(offer, result.getOrNull())
            assertEquals(offer, offerResolver.cache[offerUri])
        }
    }

    @Test
    fun `resolve method should remove failed offer from cache if credentialOffer resolve fails`() {
        runTest {
            // Given
            coEvery { credentialOfferResolver.resolve(offerUri) } returns Result.failure(Exception())
            val offer = DefaultOffer(credentialOffer)
            offerResolver.cache[offerUri] = offer
            assertEquals(offer, offerResolver.cache[offerUri])

            // When
            val result = offerResolver.resolve(offerUri, false)

            // Then
            coVerify(exactly = 1) { credentialOfferResolver.resolve(offerUri) }
            assertTrue(result.isFailure)
            assertNull(offerResolver.cache[offerUri])
        }
    }
}