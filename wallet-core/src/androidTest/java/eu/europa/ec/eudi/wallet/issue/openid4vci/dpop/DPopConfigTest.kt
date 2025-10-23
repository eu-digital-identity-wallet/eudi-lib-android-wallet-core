package eu.europa.ec.eudi.wallet.issue.openid4vci.dpop

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.AndroidKeystoreCreateKeySettings
import org.multipaz.securearea.AndroidKeystoreSecureArea

@RunWith(AndroidJUnit4::class)
class DPopConfigTest {
    companion object {

        private lateinit var context: Context

        @BeforeClass
        @JvmStatic
        fun setup() {
            context = InstrumentationRegistry.getInstrumentation().targetContext
        }
    }

    @Test
    fun test_Default_make_creates_Custom_config_with_Android_Keystore() = runTest {

        val dpopConfig = DPopConfig.Default.make(context)

        assertTrue(dpopConfig.secureArea is AndroidKeystoreSecureArea)
        val createKeySettings = dpopConfig.createKeySettingsBuilder(listOf(Algorithm.ESP256, Algorithm.ESP512))
        assertTrue(createKeySettings is AndroidKeystoreCreateKeySettings)
        createKeySettings as AndroidKeystoreCreateKeySettings
        assertEquals(false, createKeySettings.userAuthenticationRequired)
        assertEquals(Algorithm.ESP256, createKeySettings.algorithm)
        assertEquals(KeyUnlockDataProvider.None, dpopConfig.keyUnlockDataProvider)

    }

    @Test
    fun test_Default_make_with_attestation_challenge() = runTest {
        val attestationChallenge = ByteArray(16) { it.toByte() }

        val dpopConfig = DPopConfig.Default.make(context, attestationChallenge)

        assertTrue(dpopConfig.secureArea is AndroidKeystoreSecureArea)
        val createKeySettings = dpopConfig.createKeySettingsBuilder(listOf(Algorithm.ESP256))
        createKeySettings as AndroidKeystoreCreateKeySettings
        assertEquals(createKeySettings.attestationChallenge, ByteString(attestationChallenge))
    }
}
