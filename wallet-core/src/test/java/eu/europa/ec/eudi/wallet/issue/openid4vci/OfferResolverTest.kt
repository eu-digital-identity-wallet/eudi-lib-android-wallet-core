/*
 *  Copyright (c) 2024 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.CredentialOfferRequestResolver
import io.ktor.client.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URLEncoder

class OfferResolverTest {

    private lateinit var ktorHttpClientFactory: () -> HttpClient
    private lateinit var credentialOffer: CredentialOffer
    private lateinit var offerResolver: OfferResolver
    private lateinit var credentialOfferResolver: CredentialOfferRequestResolver
    private val offerUri =
        "openid-credential-offer://?credential_offer_uri=${URLEncoder.encode("https://offer.uri", "UTF-8")}"

    @BeforeEach
    fun setUp() {
        ktorHttpClientFactory = mockk(relaxed = true)

        credentialOffer = mockk(relaxed = true)
        credentialOfferResolver = mockk()
        mockkObject(CredentialOfferRequestResolver.Companion)
        every { CredentialOfferRequestResolver.Companion.invoke(any()) } returns credentialOfferResolver

        val proofTypes = listOf(OpenId4VciManager.Config.ProofType.JWT)
        offerResolver = OfferResolver(proofTypes, ktorHttpClientFactory)
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
            assert(result.isSuccess)
            assert(result.getOrNull() == offer)
        }
    }

    @Test
    fun `resolve method when useCache is true should resolve credentialOffer if not contained in cache and store it in cache`() {
        runTest {
            // Given
            coEvery { credentialOfferResolver.resolve(offerUri) } returns Result.success(credentialOffer)
            assert(offerResolver.cache[offerUri] == null)
            val offer = DefaultOffer(credentialOffer, offerResolver.credentialConfigurationFilter)

            // When
            val result = offerResolver.resolve(offerUri, true)

            // Then
            coVerify(exactly = 1) { credentialOfferResolver.resolve(offerUri) }
            assert(result.isSuccess)
            assert(result.getOrNull() == offer)
            assert(offerResolver.cache[offerUri] == offer)
        }
    }

    @Test
    fun `resolve method when useCache is false should resolve credentialOffer and store it in cache`() {
        runTest {
            // Given
            coEvery { credentialOfferResolver.resolve(offerUri) } returns Result.success(credentialOffer)
            val offer = DefaultOffer(credentialOffer, offerResolver.credentialConfigurationFilter)

            // When
            val result = offerResolver.resolve(offerUri, false)

            // Then
            coVerify(exactly = 1) { credentialOfferResolver.resolve(offerUri) }
            assert(result.isSuccess)
            assert(result.getOrNull() == offer)
            assert(offerResolver.cache[offerUri] == offer)
        }
    }

    @Test
    fun `resolve method should remove failed offer from cache if credentialOffer resolve fails`() {
        runTest {
            // Given
            coEvery { credentialOfferResolver.resolve(offerUri) } returns Result.failure(Exception())
            val offer = DefaultOffer(credentialOffer, offerResolver.credentialConfigurationFilter)
            offerResolver.cache[offerUri] = offer
            assert(offerResolver.cache[offerUri] == offer)

            // When
            val result = offerResolver.resolve(offerUri, false)

            // Then
            coVerify(exactly = 1) { credentialOfferResolver.resolve(offerUri) }
            assert(result.isFailure)
            assert(offerResolver.cache[offerUri] == null)
        }
    }
}