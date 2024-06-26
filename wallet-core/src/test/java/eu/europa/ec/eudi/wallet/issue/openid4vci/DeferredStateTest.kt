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

import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.openid4vci.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Instant
import java.util.*

class DeferredStateTest {

    @Test
    fun `test encode and decode methods`() {
        val deferredCredential = IssuedCredential.Deferred(
            transactionId = TransactionId("transactionId"),
        )
        val credentialIdentifiers = mapOf(
            CredentialConfigurationIdentifier("credentialConfigurationId") to listOf(
                CredentialIdentifier("credentialId")
            )
        )
        val authorizedRequest = AuthorizedRequest.ProofRequired(
            accessToken = AccessToken(accessToken = "accessToken", expiresInSec = 3600, useDPoP = false),
            refreshToken = RefreshToken("refreshToken", expiresInSec = 3600),
            cNonce = CNonce(value = "cNonce"),
            credentialIdentifiers = credentialIdentifiers,
            timestamp = Instant.now()
        )

        val credentialIssuerMetadata = CredentialIssuerMetadata(
            credentialIssuerIdentifier = CredentialIssuerId("https://localhost:8080").getOrThrow(),
            authorizationServers = listOf(HttpsUrl("https://localhost:8080").getOrThrow()),
            credentialEndpoint = CredentialIssuerEndpoint("https://localhost:8080").getOrThrow(),
            batchCredentialEndpoint = CredentialIssuerEndpoint("https://localhost:8080").getOrThrow(),
            deferredCredentialEndpoint = CredentialIssuerEndpoint("https://localhost:8080").getOrThrow(),
            notificationEndpoint = CredentialIssuerEndpoint("https://localhost:8080").getOrThrow(),
            credentialResponseEncryption = CredentialResponseEncryption.NotSupported,
            credentialIdentifiersSupported = true,
            credentialConfigurationsSupported = mapOf(
                CredentialConfigurationIdentifier("credentialConfigurationId") to MsoMdocCredential(
                    credentialSigningAlgorithmsSupported = listOf("alg"),
                    isoCredentialSigningAlgorithmsSupported = listOf(CoseAlgorithm.ES256),
                    isoCredentialCurvesSupported = listOf(CoseCurve.P_256),
                    proofTypesSupported = ProofTypesSupported(
                        setOf(
                            ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES256)),
                            ProofTypeMeta.Cwt(listOf(CoseAlgorithm.ES256), listOf(CoseCurve.P_256))
                        )
                    ),
                    display = listOf(
                        Display(
                            name = "name",
                            locale = Locale.US,
                            logo = Display.Logo(URI("http://localhost:8080"), "image/png"),
                            textColor = "#444444"
                        )
                    ),
                    claims = mapOf(
                        "namespace" to mapOf(
                            "claim" to Claim(
                                true, "claim", listOf(
                                    Claim.Display("name", Locale.US)
                                )
                            )
                        )
                    ),
                    order = listOf("claim"),
                    docType = "docType",
                    isoPolicy = MsoMdocPolicy(true, 10),
                    scope = "scope",
                    cryptographicBindingMethodsSupported = listOf(CryptographicBindingMethod.JWK),
                )
            ),
            display = listOf(
                CredentialIssuerMetadata.Display(name = "name", locale = "el_GR")
            ),
        )
        val state = DeferredState(
            clientId = "clientId",
            tokenEndpoint = URI("http://localhost:8080"),
            credentialIssuerMetadata = credentialIssuerMetadata,
            authorizedRequest = authorizedRequest,
            deferredCredential = deferredCredential
        )

        val encoded = state.encode()

        val decoded = DeferredState.decode(encoded)

        assertEquals(state, decoded)

    }
}