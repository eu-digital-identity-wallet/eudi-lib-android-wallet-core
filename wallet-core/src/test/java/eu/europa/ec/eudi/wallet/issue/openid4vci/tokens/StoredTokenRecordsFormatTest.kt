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

package eu.europa.ec.eudi.wallet.issue.openid4vci.tokens

import eu.europa.ec.eudi.wallet.internal.crypto.DataSealer
import eu.europa.ec.eudi.wallet.internal.crypto.SoftwareAeadCipher
import eu.europa.ec.eudi.wallet.issue.openid4vci.DeferredContext
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import eu.europa.ec.eudi.wallet.issue.openid4vci.fromBytes
import eu.europa.ec.eudi.wallet.issue.openid4vci.readStoredDeferredContext
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.DeferredTokens
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.IssuanceMetadata
import eu.europa.ec.eudi.wallet.issue.openid4vci.toBytes
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.ByteString
import org.multipaz.storage.ephemeral.EphemeralStorage
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Records stored in format version 1, encrypted with a fixed key. They must stay readable: a change
 * that fails these tests would make the tokens stored on devices unreadable.
 */
class StoredTokenRecordsFormatTest {

    private val sealer = DataSealer(SoftwareAeadCipher(SecretKeySpec(ByteArray(32) { (0x40 + it).toByte() }, "AES")))

    @Test
    fun `stored issuance metadata of format version 1 are read`() = runTest {
        val storage = EphemeralStorage()
        storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC)
            .insert(key = "doc-fixture", data = ByteString(METADATA_V1.toByteArray()))

        assertEquals(
            IssuanceMetadata(
                credentialIssuerId = "https://issuer.example.com",
                credentialConfigurationIdentifier = "eu.europa.ec.eudi.pid_vc_sd_jwt",
                credentialEndpoint = "https://issuer.example.com/credential",
                tokenEndpoint = "https://auth.example.com/token",
                authorizationServerId = "https://auth.example.com",
                clientId = "wallet-client",
                popKeyAliases = listOf("key-1"),
                dPoPKeyAlias = "dpop-key",
                accessToken = "at-fixture",
                accessTokenType = "DPoP",
                refreshToken = "rt-fixture",
                tokenTimestamp = 1700000000L,
                grantType = "authorization_code",
            ),
            IssuanceMetadataStore(storage, sealer).get("doc-fixture")
        )
    }

    @Test
    fun `stored deferred contexts of format version 1 are read`() {
        val (context, tokens) = readStoredDeferredContext(DEFERRED_DPOP_V1.toByteArray(), "deferred-fixture", sealer)

        assertEquals("DPoP", context.accessTokenType)
        assertEquals("old-doc", context.replacesDocumentId)
        assertEquals(DeferredTokens("tx-fixture", "at-fixture", "rt-fixture"), tokens)
    }

    @Test
    fun `stored records hold every field, also one with its default value`() = runTest {
        val storage = EphemeralStorage()
        IssuanceMetadataStore(storage, sealer).insert(
            "doc",
            IssuanceMetadata(
                credentialIssuerId = "https://issuer.example.com",
                credentialConfigurationIdentifier = "eu.europa.ec.eudi.pid_vc_sd_jwt",
                credentialEndpoint = "https://issuer.example.com/credential",
                tokenEndpoint = "https://auth.example.com/token",
                authorizationServerId = "https://auth.example.com",
                clientId = "wallet-client",
                popKeyAliases = listOf("key-1"),
                accessToken = "at-0",
                accessTokenType = "DPoP",
                tokenTimestamp = 1700000000L,
                grantType = "authorization_code",
            )
        )
        val metadata = storage.getTable(IssuanceMetadata.STORAGE_TABLE_SPEC).get("doc")!!
            .toByteArray().toString(Charsets.UTF_8)
        // The DPoP token type of a deferred context is the default value of its field
        val context = DeferredContext.fromBytes(
            DEFERRED_DPOP_V1.toByteArray(),
            "deferred-fixture",
            sealer,
            walletKeyManager = mockk(),
            walletInstanceAttestationProvider = null,
            dpopConfig = mockk<DPopConfig.Custom>(relaxed = true),
        ).toBytes("deferred-fixture", sealer).toString(Charsets.UTF_8)

        assertTrue("\"dPoPKeyAlias\":null" in metadata, metadata)
        assertTrue("\"accessTokenType\":\"DPoP\"" in context, context)
    }

    private companion object {
        const val METADATA_V1 = """{"credentialIssuerId":"https://issuer.example.com","credentialConfigurationIdentifier":"eu.europa.ec.eudi.pid_vc_sd_jwt","credentialEndpoint":"https://issuer.example.com/credential","tokenEndpoint":"https://auth.example.com/token","authorizationServerId":"https://auth.example.com","challengeEndpoint":null,"clientId":"wallet-client","clientAttestationJwt":null,"clientAttestationPopKeyId":null,"popKeyAliases":["key-1"],"dPoPKeyAlias":"dpop-key","accessTokenType":"DPoP","tokenTimestamp":1700000000,"grantType":"authorization_code","selectedReusePolicyType":null,"issuerIdentifier":null,"issuerName":null,"sealedTokens":"AQKs0h6bjzDQ7SFQO/d1LPn2qJgDhE7epRQnJAoy+5Qt4K+fnny6kOKRYM/X+16wXUxJmDb0JMPjoB+PdFz8jz/sUL56zlkiUv7MvuqbSAg70RqQ3w=="}"""

        const val DEFERRED_DPOP_V1 = """{"credentialIssuerId":"https://issuer.example.com","deferredEndpoint":"https://issuer.example.com/deferred","tokenEndpoint":"https://auth.example.com/token","authorizationServerId":"https://auth.example.com","challengeEndpoint":null,"clientId":"wallet-client","popKeyAliases":["key-1"],"dPoPKeyAlias":"dpop-key","clientAttestationPopKeyId":null,"clientAttestationJwt":null,"transactionId":null,"accessToken":null,"accessTokenType":"DPoP","refreshToken":null,"sealedTokens":"AQgC9nZrG73TYmYC5Htu0EX3AF8jmlalh8JzIUD3Pbm1FtjE1DTD82jDzYyTPhu+WYhUuVsEoI/4a6z0NrkPj8kKmj6Pk6rJ+EzjpS49Gql7111PCUTlNSlCx+oztJ2YtHdwLKdutaXrKxpGLm0irzFB","requestEncryptionKeyJwk":null,"requestEncryptionMethod":null,"responseEncryptionMethod":null,"credentialConfigurationIdentifier":"eu.europa.ec.eudi.pid_vc_sd_jwt","credentialEndpoint":"https://issuer.example.com/credential","replacesDocumentId":"old-doc","interactingParty":null,"isUserTriggered":null}"""
    }
}
