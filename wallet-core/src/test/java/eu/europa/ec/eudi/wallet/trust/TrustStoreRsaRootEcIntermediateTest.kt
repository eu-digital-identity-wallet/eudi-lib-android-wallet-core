package eu.europa.ec.eudi.wallet.trust

import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreImpl
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.security.Security
import java.security.cert.X509Certificate
import kotlin.test.assertTrue

class TrustStoreRsaRootEcIntermediateTest {

    // Fully qualified name of the internal class we need to mock
    private val crlClassName = "eu.europa.ec.eudi.iso18013.transfer.internal.readerauth.crl.CertificateCRLValidation"

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())

        // Mock the internal class by Name
        mockkStatic(crlClassName)

        // Stub the 'verify' method to do nothing
        val crlClass = Class.forName(crlClassName)
        val verifyMethod = crlClass.getMethod("verify", X509Certificate::class.java)


        // Define the stub
        every {
            verifyMethod.invoke(null, any<X509Certificate>())
        } returns Unit
    }

    @After
    fun tearDown() {
        // Clean up the static mock to avoid affecting other tests
        unmockkStatic(crlClassName)
    }

    @Test
    fun `Interop Hybrid Chain (RSA Root - EC Leaf) is valid with Default Config`() {

        val certificateChain = listOf(ecLeafSignedByIntermediateCertificate, ecIntermediateCertificate)

        val trustStore = ReaderTrustStoreImpl(
            listOf(rsaTrustedRootCertificate),
            profileValidation = { _, _ -> true }
        )

        val result = trustStore!!.validateCertificationTrustPath(certificateChain)

        assertTrue(result, "Trust store should accept RSA Root -> EC Intermediate -> EC Leaf chain")
    }
}