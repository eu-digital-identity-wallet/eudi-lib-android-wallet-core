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
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.ephemeral.EphemeralStorage
import org.robolectric.RobolectricTestRunner

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
    fun `test Custom with secure area and key settings builder`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val createKeySettingsBuilder: (List<Algorithm>) -> CreateKeySettings = { mockk() }

        val config = Custom(secureArea, createKeySettingsBuilder)

        assertEquals(secureArea, config.secureArea)
        assertNotNull(config.createKeySettingsBuilder)
        assertNotNull(config.keyUnlockDataProvider)
    }

    @Test
    fun `test Custom with custom unlock key`() = runTest {
        val storage = EphemeralStorage()
        val secureArea = SoftwareSecureArea.create(storage)
        val createKeySettingsBuilder: (List<Algorithm>) -> CreateKeySettings = { mockk() }
        val unlockKeyMock = mockk<suspend (String, SecureArea) -> Nothing?>()
        coEvery { unlockKeyMock(any(), any()) } returns null

        val config = Custom(secureArea, createKeySettingsBuilder, unlockKeyMock)

        assertEquals(secureArea, config.secureArea)
        assertNotNull(config.createKeySettingsBuilder)
        // Test the unlock key function
        val result = config.keyUnlockDataProvider("test-key", secureArea)
        assertEquals(null, result)
    }
}
