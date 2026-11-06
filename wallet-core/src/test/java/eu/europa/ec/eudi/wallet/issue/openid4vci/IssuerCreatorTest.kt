/*
 * Copyright (c) 2026 European Commission
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

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.oauth2.sdk.`as`.AuthorizationServerMetadata
import com.nimbusds.oauth2.sdk.id.Issuer
import eu.europa.ec.eudi.openid4vci.CertificateChainTrust
import eu.europa.ec.eudi.openid4vci.ClientAuthentication
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadataError
import eu.europa.ec.eudi.openid4vci.CredentialResponseEncryptionPolicy
import eu.europa.ec.eudi.openid4vci.EcConfig
import eu.europa.ec.eudi.openid4vci.EncryptionSupportConfig
import eu.europa.ec.eudi.openid4vci.IssuerMetadataPolicy
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import eu.europa.ec.eudi.wallet.provider.WalletAttestationKey
import eu.europa.ec.eudi.wallet.provider.WalletKeyManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.KeyInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IssuerCreatorTest {

    private val config = OpenId4VciManager.Config(
        clientAuthenticationType = OpenId4VciManager.ClientAuthenticationType.None("client"),
        authFlowRedirectionURI = "app://redirect",
        dpopConfig = DPopConfig.Disabled,
    )

    private val requestedPaths = mutableListOf<String>()
    private val createdClients = mutableListOf<HttpClient>()

    /**
     * Wallet-initiated issuance and re-issuance fetch the issuer metadata again in the OpenID4VCI
     * library, so the library gets the configured policy also when issuer registration is disabled.
     */
    @Test
    fun `the configured metadata policy is passed to the library when issuer registration is disabled`() = runTest {
        val policy = IssuerMetadataPolicy.RequireSigned(issuerTrust = mockk<CertificateChainTrust>())
        val creator = IssuerCreator(
            context = mockk(relaxed = true),
            config = config,
            ktorHttpClientFactory = { mockk() },
            walletInstanceAttestationProvider = null,
            walletAttestationKeyManager = mockk(),
            logger = null,
            issuerMetadataPolicy = policy,
            issuerRegistrationEnabled = false,
        )

        val libraryConfig = with(creator) {
            config.toOpenId4VCIConfig(AuthorizationServerMetadata(Issuer("https://issuer.example")))
        }

        assertSame(policy, libraryConfig.issuerMetadataPolicy)
    }

    @Test
    fun `wallet-initiated issuance by configuration id fetches each metadata document once`() = runTest {
        val creator = issuerCreator()

        creator.createIssuer(CREDENTIAL_ISSUER, listOf(CredentialConfigurationIdentifier(MDL_CONFIGURATION_ID)))

        assertEquals(1, requestCount(ISSUER_WELL_KNOWN_PATH), "issuer metadata requests: $requestedPaths")
        assertEquals(1, requestCount(AUTHORIZATION_SERVER_WELL_KNOWN_PATH), "authorization server requests: $requestedPaths")
    }

    @Test
    fun `wallet-initiated issuance by format fetches the issuer metadata once more to select the configuration`() = runTest {
        val creator = issuerCreator()

        creator.createIssuer(CREDENTIAL_ISSUER, MsoMdocFormat(MDL_DOCTYPE))

        assertEquals(2, requestCount(ISSUER_WELL_KNOWN_PATH), "issuer metadata requests: $requestedPaths")
        assertEquals(1, requestCount(AUTHORIZATION_SERVER_WELL_KNOWN_PATH), "authorization server requests: $requestedPaths")
    }

    @Test
    fun `attestation key is derived from the authorization server issuer of the resolved offer`() = runTest {
        val keyAliasInput = slot<String>()
        val supportedAlgorithms = slot<List<Algorithm>>()
        val creator = issuerCreator(
            clientAuthenticationType = OpenId4VciManager.ClientAuthenticationType.AttestationBased("client"),
            walletAttestationKeyManager = walletAttestationKeyManager(keyAliasInput, supportedAlgorithms),
        )

        creator.createIssuer(CREDENTIAL_ISSUER, listOf(CredentialConfigurationIdentifier(MDL_CONFIGURATION_ID)))

        assertEquals(AUTHORIZATION_SERVER, keyAliasInput.captured)
        assertEquals(listOf(Algorithm.fromJoseAlgorithmIdentifier("ES256")), supportedAlgorithms.captured)
        assertIs<ClientAuthentication.AttestationBased>(creator.clientAuthentication)
        assertEquals(ATTESTATION_KEY_ALIAS, creator.clientAttestationPopKeyId)
        assertEquals(1, requestCount(ISSUER_WELL_KNOWN_PATH), "issuer metadata requests: $requestedPaths")
        assertEquals(1, requestCount(AUTHORIZATION_SERVER_WELL_KNOWN_PATH), "authorization server requests: $requestedPaths")
        assertTrue(createdClients.single().isActive, "the issuer keeps using the http client")
    }

    @Test
    fun `the http client is closed when the client authentication cannot be built`() = runTest {
        val creator = issuerCreator(
            clientAuthenticationType = OpenId4VciManager.ClientAuthenticationType.AttestationBased("client"),
            authorizationServerMetadata = authorizationServerMetadata(advertisesClientAttestation = false),
        )

        assertFails {
            creator.createIssuer(CREDENTIAL_ISSUER, listOf(CredentialConfigurationIdentifier(MDL_CONFIGURATION_ID)))
        }

        assertFalse(createdClients.single().isActive)
    }

    /**
     * The client authentication passed to [eu.europa.ec.eudi.openid4vci.Issuer.make] must be the one
     * built from the resolved offer, not the placeholder used to resolve it. Only an attestation-based
     * client authentication is checked against the client attestation algorithms the authorization
     * server advertises, so with the placeholder issuer creation would succeed.
     */
    @Test
    fun `Issuer make receives the client authentication built from the resolved offer`() = runTest {
        val creator = issuerCreator(
            clientAuthenticationType = OpenId4VciManager.ClientAuthenticationType.AttestationBased("client"),
            walletAttestationKeyManager = walletAttestationKeyManager(),
            authorizationServerMetadata = authorizationServerMetadata(
                advertisesClientAttestation = true,
                clientAttestationSigningAlgorithm = "ES384",
            ),
        )

        val error = assertFails {
            creator.createIssuer(CREDENTIAL_ISSUER, listOf(CredentialConfigurationIdentifier(MDL_CONFIGURATION_ID)))
        }

        assertTrue(
            error.message.orEmpty().contains("Client Attestation JWS Algorithm not supported"),
            "unexpected failure: $error",
        )
    }

    @Test
    fun `metadata resolution errors are rethrown as reported by the metadata resolver`() = runTest {
        val creator = issuerCreator(issuerMetadataAvailable = false)

        val error = assertFails {
            creator.createIssuer(CREDENTIAL_ISSUER, listOf(CredentialConfigurationIdentifier(MDL_CONFIGURATION_ID)))
        }

        assertIs<CredentialIssuerMetadataError>(error)
        assertFalse(createdClients.single().isActive)
    }

    /**
     * A [WalletKeyManager] that returns an ES256 attestation key, capturing the alias input and the
     * algorithms it is asked for.
     */
    private fun walletAttestationKeyManager(
        keyAliasInput: CapturingSlot<String> = slot(),
        supportedAlgorithms: CapturingSlot<List<Algorithm>> = slot(),
    ): WalletKeyManager {
        val keyInfo = mockk<KeyInfo> {
            every { algorithm } returns Algorithm.ESP256
            every { alias } returns ATTESTATION_KEY_ALIAS
        }
        return mockk {
            coEvery {
                getOrCreateWalletAttestationKey(capture(keyAliasInput), capture(supportedAlgorithms))
            } returns Result.success(WalletAttestationKey(keyInfo) { it })
        }
    }

    /** Number of requests made to the given well-known metadata path. */
    private fun requestCount(wellKnownPath: String): Int =
        requestedPaths.count { it.startsWith(wellKnownPath) }

    /**
     * Creates an [IssuerCreator] whose http clients serve the credential issuer and authorization
     * server metadata from a mock engine, recording every requested path and created client.
     */
    private fun issuerCreator(
        clientAuthenticationType: OpenId4VciManager.ClientAuthenticationType =
            OpenId4VciManager.ClientAuthenticationType.None("client"),
        walletAttestationKeyManager: WalletKeyManager = mockk(),
        authorizationServerMetadata: String = authorizationServerMetadata(advertisesClientAttestation = true),
        issuerMetadataAvailable: Boolean = true,
    ): IssuerCreator {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            requestedPaths += path
            when {
                path.startsWith(ISSUER_WELL_KNOWN_PATH) && issuerMetadataAvailable ->
                    respond(ISSUER_METADATA, HttpStatusCode.OK, jsonHeaders)

                path.startsWith(AUTHORIZATION_SERVER_WELL_KNOWN_PATH) ->
                    respond(authorizationServerMetadata, HttpStatusCode.OK, jsonHeaders)

                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        return IssuerCreator(
            context = mockk(relaxed = true),
            config = OpenId4VciManager.Config(
                clientAuthenticationType = clientAuthenticationType,
                authFlowRedirectionURI = "app://redirect",
                dpopConfig = DPopConfig.Disabled,
                responseEncryptionConfig = EncryptionSupportConfig(
                    credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED,
                    ecConfig = EcConfig(ecKeyCurve = Curve.P_256),
                    rsaConfig = null,
                ),
            ),
            ktorHttpClientFactory = { HttpClient(engine).also { createdClients += it } },
            walletInstanceAttestationProvider = mockk(relaxed = true),
            walletAttestationKeyManager = walletAttestationKeyManager,
            logger = null,
        )
    }

    private companion object {
        const val CREDENTIAL_ISSUER = "https://issuer.example.com"
        const val AUTHORIZATION_SERVER = "https://auth.example.com/realms/test"
        const val ISSUER_WELL_KNOWN_PATH = "/.well-known/openid-credential-issuer"
        const val AUTHORIZATION_SERVER_WELL_KNOWN_PATH = "/.well-known/oauth-authorization-server"
        const val MDL_CONFIGURATION_ID = "org.iso.18013.5.1.mDL"
        const val MDL_DOCTYPE = "org.iso.18013.5.1.mDL"
        const val ATTESTATION_KEY_ALIAS = "client-attestation-test"

        val ISSUER_METADATA = """
            {
              "credential_issuer": "$CREDENTIAL_ISSUER",
              "authorization_servers": ["$AUTHORIZATION_SERVER"],
              "credential_endpoint": "$CREDENTIAL_ISSUER/credential",
              "credential_configurations_supported": {
                "$MDL_CONFIGURATION_ID": {
                  "format": "mso_mdoc",
                  "scope": "mdl",
                  "doctype": "$MDL_DOCTYPE"
                }
              }
            }
        """.trimIndent()

        /**
         * Authorization server metadata, optionally advertising attestation-based client
         * authentication with ES256 proofs of possession and the given client attestation algorithm.
         */
        fun authorizationServerMetadata(
            advertisesClientAttestation: Boolean,
            clientAttestationSigningAlgorithm: String = "ES256",
        ): String {
            val clientAttestation = if (advertisesClientAttestation) {
                """
                  "token_endpoint_auth_methods_supported": ["attest_jwt_client_auth"],
                  "client_attestation_signing_alg_values_supported": ["$clientAttestationSigningAlgorithm"],
                  "client_attestation_pop_signing_alg_values_supported": ["ES256"],
                """
            } else ""
            return """
                {
                  $clientAttestation
                  "issuer": "$AUTHORIZATION_SERVER",
                  "authorization_endpoint": "$AUTHORIZATION_SERVER/authorize",
                  "token_endpoint": "$AUTHORIZATION_SERVER/token",
                  "response_types_supported": ["code"]
                }
            """.trimIndent()
        }
    }
}
