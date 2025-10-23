package eu.europa.ec.eudi.wallet.issue.openid4vci.dpop

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import eu.europa.ec.eudi.wallet.logging.Logger
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.software.SoftwareCreateKeySettings
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.ephemeral.EphemeralStorage
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class SecureAreaDpopSignerTest {

    @Test
    fun `constructor creates key in secure area`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)

        assertNotNull(signer.keyInfo)
        assertNotNull(signer.keyInfo.alias)
        assertEquals("ES256", signer.keyInfo.algorithm.joseAlgorithmIdentifier)
        assertNotNull(signer.keyInfo.publicKey)
    }

    @Test
    fun `constructor uses createKeySettingsBuilder from config`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256, Algorithm.ESP384)

        var builderCalled = false
        var receivedAlgorithms: List<Algorithm>? = null

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algs ->
                builderCalled = true
                receivedAlgorithms = algs
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algs.first())
                    .build()
            }
        )

        SecureAreaDpopSigner(config, algorithms)

        assertTrue(builderCalled)
        assertEquals(algorithms, receivedAlgorithms)
    }

    @Test
    fun `constructor logs key creation when logger provided`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)
        val logger = spyk(object : Logger {
            override fun log(record: Logger.Record) {
                println(record)
            }
        })

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        SecureAreaDpopSigner(config, algorithms, logger)

        verify {
            logger.log(
                match { r -> r.message.contains("Creating DPoP key") && r.message.contains("ES256") }
            )
        }
    }

    @Test
    fun `javaAlgorithm returns correct value for ESP256`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)

        assertEquals("SHA256withECDSA", signer.javaAlgorithm)
    }

    @Test
    fun `javaAlgorithm returns correct value for ESP384`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP384)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)

        assertEquals("SHA384withECDSA", signer.javaAlgorithm)
    }

    @Test
    fun `javaAlgorithm returns correct value for ESP512`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP512)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)

        assertEquals("SHA512withECDSA", signer.javaAlgorithm)
    }

    @Test
    fun `acquire returns SignOperation with valid JWK`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)
        val signOperation = signer.acquire()

        assertNotNull(signOperation)
        assertNotNull(signOperation.publicMaterial)
        assertIs<JWK>(signOperation.publicMaterial)

        // Verify it's an EC key with correct curve
        val ecKey = signOperation.publicMaterial as ECKey
        assertEquals(Curve.P_256, ecKey.curve)
    }

    @Test
    fun `acquire returns SignOperation with correct curve for ESP384`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP384)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)
        val signOperation = signer.acquire()

        val ecKey = signOperation.publicMaterial as ECKey
        assertEquals(Curve.P_384, ecKey.curve)
    }

    @Test
    fun `acquire returns SignOperation with correct curve for ESP512`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP512)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)
        val signOperation = signer.acquire()

        val ecKey = signOperation.publicMaterial as ECKey
        assertEquals(Curve.P_521, ecKey.curve)
    }

    @Test
    fun `acquire signing function can sign data`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)
        val signOperation = signer.acquire()

        val dataToSign = "test data".toByteArray()
        val signature = signOperation.function.sign(dataToSign)

        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())
    }

    @Test
    fun `acquire signing function produces different signatures for different data`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)
        val signOperation = signer.acquire()

        val data1 = "test data 1".toByteArray()
        val data2 = "test data 2".toByteArray()

        val signature1 = signOperation.function.sign(data1)
        val signature2 = signOperation.function.sign(data2)

        assertFalse(signature1.contentEquals(signature2))
    }

    @Test
    fun `acquire uses keyUnlockDataProvider when signing`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        var providerCalled = false
        var receivedKeyAlias: String? = null
        var receivedSecureArea: SecureArea? = null

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            },
            keyUnlockDataProvider = { keyAlias, secureAreaParam ->
                providerCalled = true
                receivedKeyAlias = keyAlias
                receivedSecureArea = secureAreaParam
                null
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)
        val signOperation = signer.acquire()

        val dataToSign = "test data".toByteArray()
        signOperation.function.sign(dataToSign)

        assertTrue(providerCalled)
        assertEquals(signer.keyInfo.alias, receivedKeyAlias)
        assertEquals(secureArea, receivedSecureArea)
    }

    @Test
    fun `acquire can be called multiple times`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)

        val signOperation1 = signer.acquire()
        val signOperation2 = signer.acquire()

        assertNotNull(signOperation1)
        assertNotNull(signOperation2)

        // Both should have the same public key
        assertEquals(signOperation1.publicMaterial, signOperation2.publicMaterial)
    }

    @Test
    fun `release does nothing and accepts null`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)

        // Should not throw
        signer.release(null)
    }

    @Test
    fun `release does nothing with SignOperation`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)
        val signOperation = signer.acquire()

        // Should not throw
        signer.release(signOperation)

        // Key should still be usable after release
        val dataToSign = "test data".toByteArray()
        val signature = signOperation.function.sign(dataToSign)
        assertNotNull(signature)
    }

    @Test
    fun `key persists after release`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)
        val signOperation1 = signer.acquire()
        signer.release(signOperation1)

        // Should be able to acquire again with same key
        val signOperation2 = signer.acquire()
        assertEquals(signOperation1.publicMaterial, signOperation2.publicMaterial)
    }

    @Test
    fun `multiple signers with same secure area create different keys`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config1 = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val config2 = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer1 = SecureAreaDpopSigner(config1, algorithms)
        val signer2 = SecureAreaDpopSigner(config2, algorithms)

        // Keys should have different aliases
        assertNotEquals(signer1.keyInfo.alias, signer2.keyInfo.alias)

        // Public keys should be different
        val signOp1 = signer1.acquire()
        val signOp2 = signer2.acquire()
        assertNotEquals(signOp1.publicMaterial, signOp2.publicMaterial)
    }

    @Test
    fun `config property exposes the configuration`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)

        assertEquals(config, signer.config)
        assertEquals(secureArea, signer.config.secureArea)
    }

    @Test
    fun `signing with KeyUnlockDataProvider None works`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            },
            keyUnlockDataProvider = KeyUnlockDataProvider.None
        )

        val signer = SecureAreaDpopSigner(config, algorithms)
        val signOperation = signer.acquire()

        val dataToSign = "test data".toByteArray()
        val signature = signOperation.function.sign(dataToSign)

        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())
    }

    @Test
    fun `keyInfo contains valid public key`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val algorithms = listOf(Algorithm.ESP256)

        val config = DPopConfig.Custom(
            secureArea = secureArea,
            createKeySettingsBuilder = { algorithms ->
                SoftwareCreateKeySettings.Builder()
                    .setAlgorithm(algorithms.first())
                    .build()
            }
        )

        val signer = SecureAreaDpopSigner(config, algorithms)

        val publicKey = signer.keyInfo.publicKey
        assertNotNull(publicKey)
    }
}
        

