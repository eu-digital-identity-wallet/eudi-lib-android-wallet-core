package eu.europa.ec.eudi.wallet.issue.openid4vci.dpop

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.openid4vci.CIAuthorizationServerMetadata
import eu.europa.ec.eudi.wallet.logging.Logger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class DPopSignerTest {

    companion object {

        private lateinit var context: Context

        @BeforeClass
        @JvmStatic
        fun setup() {
            context = InstrumentationRegistry.getInstrumentation().targetContext
        }
    }

    @Test
    fun makeIfSupported_succeeds_with_Default_config_when_server_supports_DPoP() = runTest {
        val metadata = createAuthServerMetadata(listOf(JWSAlgorithm.ES256, JWSAlgorithm.ES384))

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = DPopConfig.Default,
            authorizationServerMetadata = metadata
        )

        assertTrue(result.isSuccess)
        val signer = result.getOrNull()
        assertNotNull(signer)
        assertTrue(signer is SecureAreaDpopSigner)
    }

    @Test
    fun makeIfSupported_with_logger_logs_matched_algorithms() = runTest {
        val logger = Logger { record ->
            Log.d(record.sourceClassName, record.message, record.thrown)
        }
        val metadata = createAuthServerMetadata(listOf(JWSAlgorithm.ES256))

        val result = DPopSigner.makeIfSupported(
            context = context,
            config = DPopConfig.Default,
            authorizationServerMetadata = metadata,
            logger = logger
        )

        assertTrue(result.isSuccess)
    }

    // Helper functions

    private fun createAuthServerMetadata(algorithms: List<JWSAlgorithm>?): CIAuthorizationServerMetadata {
        return Mockito.mock<CIAuthorizationServerMetadata>().apply {
            `when`(dPoPJWSAlgs).thenReturn(algorithms)
        }
    }
}

