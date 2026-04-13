package eu.europa.ec.eudi.wallet.issue.openid4vci.dpop

import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.openid4vci.CIAuthorizationServerMetadata
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.KeyInfo
import org.multipaz.securearea.SecureArea
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class DPopSignerTest {

    @Test
    fun `makeIfSupported fails when config is Disabled`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val metadata = createAuthServerMetadata(listOf(JWSAlgorithm.ES256))

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = DPopConfig.Disabled,
            authorizationServerMetadata = metadata
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalStateException)
        assertEquals("DPoP is disabled in the configuration", exception?.message)
    }

    @Test
    fun `makeIfSupported fails when server metadata has no DPoP algorithms`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val metadata = createAuthServerMetadata(null)

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = DPopConfig.Default,
            authorizationServerMetadata = metadata
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("dpop_signing_alg_values_supported") == true)
    }

    @Test
    fun `makeIfSupported fails when server DPoP algorithms list is empty`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val metadata = createAuthServerMetadata(emptyList())

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = DPopConfig.Default,
            authorizationServerMetadata = metadata
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("does not support DPoP") == true)
    }

    @Test
    fun `makeIfSupported fails when no common algorithm exists`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val secureArea = createSecureAreaMock(
            supportedAlgorithms = listOf(
                Algorithm.ESP256
            )
        )

        val metadata = createAuthServerMetadata(listOf(JWSAlgorithm.parse("NONEXISTENT_ALG")))

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { mockk<CreateKeySettings>() }
        )

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = config,
            authorizationServerMetadata = metadata
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("No common algorithm found") == true)
    }

    @Test
    fun `makeIfSupported succeeds with Custom config when algorithms match`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val algorithm = Algorithm.ESP256
        val secureArea = createSecureAreaMock(supportedAlgorithms = listOf(algorithm))

        var capturedAlgorithms: List<Algorithm>? = null
        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                capturedAlgorithms = algorithms
                createKeySettingsMock(algorithms.first())
            }
        )

        val metadata = createAuthServerMetadata(listOf(JWSAlgorithm.ES256))

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = config,
            authorizationServerMetadata = metadata
        )

        assertTrue(result.isSuccess)
        val signer = result.getOrNull()
        assertNotNull(signer)
        assertTrue(signer is SecureAreaDpopSigner)

        // Verify that the builder received the matched algorithms
        assertEquals(listOf(algorithm), capturedAlgorithms)
    }

    @Test
    fun `makeIfSupported passes all matched algorithms to builder`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val algorithm1 = Algorithm.ESP256
        val algorithm2 = Algorithm.ESP384
        val secureArea = createSecureAreaMock(
            supportedAlgorithms = listOf(algorithm1, algorithm2)
        )
        val serverAlgorithms = listOf(JWSAlgorithm.ES256, JWSAlgorithm.ES384)

        var capturedAlgorithms: List<Algorithm>? = null
        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                capturedAlgorithms = algorithms
                createKeySettingsMock(algorithms.first())
            }
        )

        val metadata = createAuthServerMetadata(serverAlgorithms)

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = config,
            authorizationServerMetadata = metadata
        )

        assertTrue(result.isSuccess)

        // Verify that the builder received matched algorithms
        assertEquals(listOf(algorithm1, algorithm2), capturedAlgorithms)
    }

    @Test
    fun `makeIfSupported with Custom config and unlock key function`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val secureArea = createSecureAreaMock(
            supportedAlgorithms = listOf(
                Algorithm.ESP256
            )
        )
        val unlockKeyMock = KeyUnlockDataProvider { _, _ -> null }

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                createKeySettingsMock(algorithms.first())
            },
            keyUnlockDataProvider = unlockKeyMock
        )

        val metadata = createAuthServerMetadata(listOf(JWSAlgorithm.ES256))

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = config,
            authorizationServerMetadata = metadata
        )

        assertTrue(result.isSuccess)
        val signer = result.getOrNull()
        assertIs<SecureAreaDpopSigner>(signer)
        // Verify the custom config was used (the signer stores the config)
        assertNotNull(signer.config)
        assertTrue(signer.config.keyUnlockDataProvider === unlockKeyMock)
    }

    @Test
    fun `makeIfSupported filters out non-signing algorithms`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val signingAlgorithm = Algorithm.ESP256
        val nonSigningAlgorithm = Algorithm.ES384
        val secureArea = createSecureAreaMock(
            supportedAlgorithms = listOf(signingAlgorithm, nonSigningAlgorithm)
        )

        var capturedAlgorithms: List<Algorithm>? = null
        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                capturedAlgorithms = algorithms
                createKeySettingsMock(algorithms.first())
            }
        )

        val metadata = createAuthServerMetadata(
            listOf(JWSAlgorithm.ES256, JWSAlgorithm.ES384)
        )

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = config,
            authorizationServerMetadata = metadata
        )

        assertTrue(result.isSuccess)

        // Verify only signing algorithms were passed to builder
        assertEquals(listOf(signingAlgorithm), capturedAlgorithms)
    }

    @Test
    fun `makeIfSupported filters out algorithms without JOSE identifier`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val algorithmWithJoseId = Algorithm.ESP256
        val algorithmWithoutJoseId = Algorithm.ES256
        val secureArea = createSecureAreaMock(
            supportedAlgorithms = listOf(algorithmWithJoseId, algorithmWithoutJoseId)
        )

        var capturedAlgorithms: List<Algorithm>? = null
        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                capturedAlgorithms = algorithms
                createKeySettingsMock(algorithms.first())
            }
        )

        val metadata = createAuthServerMetadata(listOf(JWSAlgorithm.ES256))

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = config,
            authorizationServerMetadata = metadata
        )

        assertTrue(result.isSuccess)

        // Verify only algorithms with JOSE identifiers were passed to builder
        assertEquals(listOf(algorithmWithJoseId), capturedAlgorithms)
    }

    // Helper functions

    private fun createAuthServerMetadata(algorithms: List<JWSAlgorithm>?): CIAuthorizationServerMetadata {
        return mockk<CIAuthorizationServerMetadata>(relaxed = true) {
            every { dPoPJWSAlgs } returns algorithms
        }
    }

    private fun createSecureAreaMock(
        supportedAlgorithms: List<Algorithm>
    ): SecureArea {
        val keyInfo = mockk<KeyInfo>(relaxed = true)
        return mockk<SecureArea>(relaxed = true) {
            every { this@mockk.supportedAlgorithms } returns supportedAlgorithms
            coEvery { createKey(any(), any()) } returns keyInfo
        }
    }

    private fun createKeySettingsMock(algorithm: Algorithm): CreateKeySettings =
        mockk<CreateKeySettings>(relaxed = true) {
            every { this@mockk.algorithm } returns algorithm
        }
}
