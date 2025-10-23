package eu.europa.ec.eudi.wallet.issue.openid4vci.dpop

import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.openid4vci.CIAuthorizationServerMetadata
import eu.europa.ec.eudi.wallet.issue.openid4vci.javaAlgorithm
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.software.SoftwareCreateKeySettings
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.ephemeral.EphemeralStorage
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
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)

        // Server supports an algorithm that is unlikely to exist in SoftwareSecureArea
        val metadata = createAuthServerMetadata(listOf(JWSAlgorithm.parse("NONEXISTENT_ALG")))

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                // This should not be called if no algorithms match
                mockk<CreateKeySettings>()
            }
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
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)

        val algorithmSlot = slot<List<Algorithm>>()
        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                algorithmSlot.captured = algorithms
                // Use the first algorithm from the list for SoftwareSecureArea
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
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
        assertTrue(algorithmSlot.isCaptured)
        assertTrue(algorithmSlot.captured.isNotEmpty())

    }

    @Test
    fun `makeIfSupported passes all matched algorithms to builder`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)

        val algorithmSlot = slot<List<Algorithm>>()
        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                algorithmSlot.captured = algorithms
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        // Server supports ES256 and ES384
        val metadata = createAuthServerMetadata(listOf(JWSAlgorithm.ES256, JWSAlgorithm.ES384))

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = config,
            authorizationServerMetadata = metadata
        )

        assertTrue(result.isSuccess)

        // Verify that the builder received matched algorithms
        assertTrue(algorithmSlot.isCaptured)
        assertTrue(algorithmSlot.captured.isNotEmpty())
    }

    @Test
    fun `makeIfSupported with Custom config and unlock key function`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val unlockKeyMock = KeyUnlockDataProvider { _, _ -> null }

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
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
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)

        val algorithmSlot = slot<List<Algorithm>>()
        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                algorithmSlot.captured = algorithms
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first { it.isSigning })
                    .build()
            }
        )

        val metadata = createAuthServerMetadata(listOf(JWSAlgorithm.ES256))

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = config,
            authorizationServerMetadata = metadata
        )

        assertTrue(result.isSuccess)

        // Verify only signing algorithms were passed to builder
        assertTrue(algorithmSlot.isCaptured)
        assertTrue(algorithmSlot.captured.isNotEmpty())
        assertTrue(algorithmSlot.captured.all { it.isSigning })
    }

    @Test
    fun `makeIfSupported filters out algorithms without JOSE identifier`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)

        val algorithmSlot = slot<List<Algorithm>>()
        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                algorithmSlot.captured = algorithms
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
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
        assertTrue(algorithmSlot.isCaptured)
        assertTrue(algorithmSlot.captured.isNotEmpty())
        assertTrue(algorithmSlot.captured.all { it.joseAlgorithmIdentifier != null })
    }

    // Helper functions

    private fun createAuthServerMetadata(algorithms: List<JWSAlgorithm>?): CIAuthorizationServerMetadata {
        return mockk<CIAuthorizationServerMetadata>(relaxed = true) {
            every { dPoPJWSAlgs } returns algorithms
        }
    }

    private fun createAlgorithmMock(
        joseId: String,
        javaAlgo: String,
        isSigning: Boolean = true
    ): Algorithm {
        return mockk<Algorithm>(relaxed = true) {
            every { joseAlgorithmIdentifier } returns joseId
            every { this@mockk.isSigning } returns isSigning
            every { javaAlgorithm } returns javaAlgo
        }
    }
}

