package eu.europa.ec.eudi.wallet.issue.openid4vci.dpop

import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig.Custom
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.SecureArea
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.Closeable

@RunWith(RobolectricTestRunner::class)
class DPopConfigTest {

    @Test
    fun `test Disabled object`() {
        assertNotNull(DPopConfig.Disabled)
    }

    @Test
    fun `test Default object`() {
        assertNotNull(DPopConfig.Default)
    }

    @Test
    fun `test Default make creates Custom config with Android Keystore`() = runTest {
        val context = RuntimeEnvironment.getApplication()

        val dpopConfig = DPopConfig.Default.make(context)

        try {
            assertNotNull(dpopConfig.secureArea)
            assertNotNull(dpopConfig.createKeySettingsBuilder)
            assertNotNull(dpopConfig.unlockKey)
        } finally {
            (dpopConfig.secureArea as? Closeable)?.close()
        }
    }

    @Test
    fun `test Default make with attestation challenge`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val attestationChallenge = ByteArray(16) { it.toByte() }

        val dpopConfig = DPopConfig.Default.make(context, attestationChallenge)

        try {
            assertNotNull(dpopConfig.secureArea)
        } finally {
            (dpopConfig.secureArea as? Closeable)?.close()
        }
    }

    @Test
    fun `test Custom with secure area and key settings builder`() {
        val secureArea: SecureArea = mockk(relaxed = true)
        val createKeySettingsBuilder: (Algorithm) -> CreateKeySettings = { mockk() }

        val config = Custom(secureArea, createKeySettingsBuilder)

        assertEquals(secureArea, config.secureArea)
        assertNotNull(config.createKeySettingsBuilder)
        assertNotNull(config.unlockKey)
    }

    @Test
    fun `test Custom with custom unlock key`() = runTest {
        val secureArea: SecureArea = mockk(relaxed = true)
        val createKeySettingsBuilder: (Algorithm) -> CreateKeySettings = { mockk() }
        val unlockKeyMock = mockk<suspend (String, SecureArea) -> Nothing?>()
        coEvery { unlockKeyMock(any(), any()) } returns null

        val config = Custom(secureArea, createKeySettingsBuilder, unlockKeyMock)

        assertEquals(secureArea, config.secureArea)
        assertNotNull(config.createKeySettingsBuilder)
        // Test the unlock key function
        val result = config.unlockKey("test-key", secureArea)
        assertEquals(null, result)
    }
}
