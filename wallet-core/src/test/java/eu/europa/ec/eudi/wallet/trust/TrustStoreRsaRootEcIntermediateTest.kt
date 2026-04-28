package eu.europa.ec.eudi.wallet.trust

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreImpl
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import java.security.Security
import kotlin.test.assertTrue

class TrustStoreRsaRootEcIntermediateTest {

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun `Interop Hybrid Chain (RSA Root - EC Leaf) is valid with Default Config`() {

        val certificateChain = listOf(ecLeafSignedByIntermediateCertificate, ecIntermediateCertificate)

        val trustStore = ReaderTrustStoreImpl(
            listOf(rsaTrustedRootCertificate),
            profileValidation = { _, _ -> true }
        )

        val result = trustStore.validateCertificationTrustPath(certificateChain)

        assertTrue(result, "Trust store should accept RSA Root -> EC Intermediate -> EC Leaf chain")
    }
}