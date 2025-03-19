/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet

import android.util.Log
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStoreImpl
import eu.europa.ec.eudi.iso18013.transfer.readerauth.profile.ProfileValidation
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertTrue

class TrustStoreTest {

    @BeforeTest
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(),any(), any()) } returns 0
        every { Log.d(any(),any()) } returns 0
        every { Log.e(any(),any(), any()) } returns 0
        every { Log.e(any(),any()) } returns 0
        every { Log.i(any(),any(), any()) } returns 0
        every { Log.i(any(),any()) } returns 0
        every { Log.w(any(),any(), any()) } returns 0
    }

    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun testChain() {

//        Security.addProvider(BouncyCastleProvider())

        val pemToCertificate = { pem: String ->
            ByteArrayInputStream(Base64.getDecoder().decode(pem)).use {
                CertificateFactory.getInstance("X.509").generateCertificate(it)
            } as X509Certificate
        }

        val certificateChain = arrayOf(
//            "MIICMjCCAbigAwIBAgIQTklTVF9UZXN0X1JEUkNfODAKBggqhkjOPQQDAzArMQswCQYDVQQGEwJVUzEcMBoGA1UEAwwTTklTVCBURVNUIFJlYWRlciBDQTAeFw0yNDEyMDQxNjI3NDdaFw0yNTEyMDQxNjI3NDdaMDIxCzAJBgNVBAYTAlVTMSMwIQYDVQQDDBpOSVNUIFRFU1QgUmVhZGVyIGZvciBQLTI1NjBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABAqk1xBCdihbHt1vLziM/5nOd9LIlQWvNIC8Vd075gmMHwSGGxy/sUVH+HvVU5BdMppkRITDW4B+wiNGRCvABL+jgbYwgbMwDgYDVR0PAQH/BAQDAgeAMCkGA1UdEQQiMCCGFGh0dHBzOi8vd3d3Lm5pc3QuZ292gghuaXN0LmdvdjAfBgNVHRIEGDAWhhRodHRwczovL3d3dy5uaXN0LmdvdjAVBgNVHSUBAf8ECzAJBgcogYxdBQEGMB0GA1UdDgQWBBSDJC99zoR0NVXrJMDgCc1qYiCXGzAfBgNVHSMEGDAWgBSZCq9uTcMOmWmdms+QlbCDct9gmzAKBggqhkjOPQQDAwNoADBlAjEAgZ7m6L7LVoJPZuLFk7vPcWoPbE75Q7ug28fpPFDE0KBO4FvxPqFnRb6sI+ngl9qEAjAGxBa/3SD5lHlRodcM1r7CEaQv+m1bd7lkMJKhbiY8e6xLpg4n1QJbIHqVhFchp/0=",
//            """MIICDzCCAZWgAwIBAgIQTklTVF9UZXN0X1JEUkNfMTAKBggqhkjOPQQDAzArMQswCQYDVQQGEwJVUzEcMBoGA1UEAwwTTklTVCBURVNUIFJlYWRlciBDQTAeFw0yMTA5MDYyMjAwMDBaFw0zMDA5MDYyMjAwMDBaMCsxCzAJBgNVBAYTAlVTMRwwGgYDVQQDDBNOSVNUIFRFU1QgUmVhZGVyIENBMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEUl9fqD7ZHYLN1Q+0WKv7JzD2pypz6nucE4z6Oe+AP+zK5jbb8wfQhByNHC8YaQ5cTMm0dWh+2DAsTePbSmDMWvkVVveftnRG4jXW3AhibwKTTOSPksZR+YPRNXl4SPjbo34wfDAdBgNVHQ4EFgQUmQqvbk3DDplpnZrPkJWwg3LfYJswDgYDVR0PAQH/BAQDAgEGMCAGA1UdEgQZMBeGFWh0dHBzOi8vd3d3Lm5pc3QuZ292LzASBgNVHRMBAf8ECDAGAQH/AgEAMBUGA1UdJQEB/wQLMAkGByiBjF0FAQYwCgYIKoZIzj0EAwMDaAAwZQIwdoPPRC46DhYnQX7KZNvbRYyW2Ra+9uYBRt0eSNuRfvU+xMqUuqE+NGsNyqlVp6osAjEA9np4Z76pMkGGc/4ko1ZuRbq5AzuHzK7BKtlAqleoefuqHcFqRQ6OuufYjIR5me8O"""

            "MIIDLzCCArSgAwIBAgIUckoWtC7oIXcmW2YQmuNjqKiexDkwCgYIKoZIzj0EAwIwXDEeMBwGA1UEAwwVUElEIElzc3VlciBDQSAtIFVUIDAxMS0wKwYDVQQKDCRFVURJIFdhbGxldCBSZWZlcmVuY2UgSW1wbGVtZW50YXRpb24xCzAJBgNVBAYTAlVUMB4XDTI0MDIyNjAyMzQzNFoXDTI2MDIyNTAyMzQzM1owaTEdMBsGA1UEAwwURVVESSBSZW1vdGUgVmVyaWZpZXIxDDAKBgNVBAUTAzAwMTEtMCsGA1UECgwkRVVESSBXYWxsZXQgUmVmZXJlbmNlIEltcGxlbWVudGF0aW9uMQswCQYDVQQGEwJVVDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABBNAOLgA5kJaB3OTI9PddwLFcPvm0mRgyorYbzG20Ri1pS3YRA/31ruWtVQqLm/3yM+5fOgFaUjzNUmkR3k+CayjggFFMIIBQTAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFLNsuJEXHNekGmYxh0Lhi8BAzJUbMCkGA1UdEQQiMCCCHmRldi52ZXJpZmllci1iYWNrZW5kLmV1ZGl3LmRldjASBgNVHSUECzAJBgcogYxdBQEGMEMGA1UdHwQ8MDowOKA2oDSGMmh0dHBzOi8vcHJlcHJvZC5wa2kuZXVkaXcuZGV2L2NybC9waWRfQ0FfVVRfMDEuY3JsMB0GA1UdDgQWBBRRYFB0UydB7Ozi6BLghBS3ViqnpjAOBgNVHQ8BAf8EBAMCB4AwXQYDVR0SBFYwVIZSaHR0cHM6Ly9naXRodWIuY29tL2V1LWRpZ2l0YWwtaWRlbnRpdHktd2FsbGV0L2FyY2hpdGVjdHVyZS1hbmQtcmVmZXJlbmNlLWZyYW1ld29yazAKBggqhkjOPQQDAgNpADBmAjEAvBREn4wUiQFfTzNx6w34ICqvhT97eyYUc5nxW/jmjzAM8FkCASKh3p/sS7MiOLfcAjEA2no4L7L7MKGHrd1n5j+CGgNBsdnXPE2d/T3mebxKrd28bmTrsNPgaNnCvsqNXYpl",
            "MIIDHTCCAqOgAwIBAgIUVqjgtJqf4hUYJkqdYzi+0xwhwFYwCgYIKoZIzj0EAwMwXDEeMBwGA1UEAwwVUElEIElzc3VlciBDQSAtIFVUIDAxMS0wKwYDVQQKDCRFVURJIFdhbGxldCBSZWZlcmVuY2UgSW1wbGVtZW50YXRpb24xCzAJBgNVBAYTAlVUMB4XDTIzMDkwMTE4MzQxN1oXDTMyMTEyNzE4MzQxNlowXDEeMBwGA1UEAwwVUElEIElzc3VlciBDQSAtIFVUIDAxMS0wKwYDVQQKDCRFVURJIFdhbGxldCBSZWZlcmVuY2UgSW1wbGVtZW50YXRpb24xCzAJBgNVBAYTAlVUMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEFg5Shfsxp5R/UFIEKS3L27dwnFhnjSgUh2btKOQEnfb3doyeqMAvBtUMlClhsF3uefKinCw08NB31rwC+dtj6X/LE3n2C9jROIUN8PrnlLS5Qs4Rs4ZU5OIgztoaO8G9o4IBJDCCASAwEgYDVR0TAQH/BAgwBgEB/wIBADAfBgNVHSMEGDAWgBSzbLiRFxzXpBpmMYdC4YvAQMyVGzAWBgNVHSUBAf8EDDAKBggrgQICAAABBzBDBgNVHR8EPDA6MDigNqA0hjJodHRwczovL3ByZXByb2QucGtpLmV1ZGl3LmRldi9jcmwvcGlkX0NBX1VUXzAxLmNybDAdBgNVHQ4EFgQUs2y4kRcc16QaZjGHQuGLwEDMlRswDgYDVR0PAQH/BAQDAgEGMF0GA1UdEgRWMFSGUmh0dHBzOi8vZ2l0aHViLmNvbS9ldS1kaWdpdGFsLWlkZW50aXR5LXdhbGxldC9hcmNoaXRlY3R1cmUtYW5kLXJlZmVyZW5jZS1mcmFtZXdvcmswCgYIKoZIzj0EAwMDaAAwZQIwaXUA3j++xl/tdD76tXEWCikfM1CaRz4vzBC7NS0wCdItKiz6HZeV8EPtNCnsfKpNAjEAqrdeKDnr5Kwf8BA7tATehxNlOV4Hnc10XO1XULtigCwb49RpkqlS2Hul+DpqObUs",
//            "MIIDLzCCArSgAwIBAgIUckoWtC7oIXcmW2YQmuNjqKiexDkwCgYIKoZIzj0EAwIwXDEeMBwGA1UEAwwVUElEIElzc3VlciBDQSAtIFVUIDAxMS0wKwYDVQQKDCRFVURJIFdhbGxldCBSZWZlcmVuY2UgSW1wbGVtZW50YXRpb24xCzAJBgNVBAYTAlVUMB4XDTI0MDIyNjAyMzQzNFoXDTI2MDIyNTAyMzQzM1owaTEdMBsGA1UEAwwURVVESSBSZW1vdGUgVmVyaWZpZXIxDDAKBgNVBAUTAzAwMTEtMCsGA1UECgwkRVVESSBXYWxsZXQgUmVmZXJlbmNlIEltcGxlbWVudGF0aW9uMQswCQYDVQQGEwJVVDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABBNAOLgA5kJaB3OTI9PddwLFcPvm0mRgyorYbzG20Ri1pS3YRA/31ruWtVQqLm/3yM+5fOgFaUjzNUmkR3k+CayjggFFMIIBQTAMBgNVHRMBAf8EAjAAMB8GA1UdIwQYMBaAFLNsuJEXHNekGmYxh0Lhi8BAzJUbMCkGA1UdEQQiMCCCHmRldi52ZXJpZmllci1iYWNrZW5kLmV1ZGl3LmRldjASBgNVHSUECzAJBgcogYxdBQEGMEMGA1UdHwQ8MDowOKA2oDSGMmh0dHBzOi8vcHJlcHJvZC5wa2kuZXVkaXcuZGV2L2NybC9waWRfQ0FfVVRfMDEuY3JsMB0GA1UdDgQWBBRRYFB0UydB7Ozi6BLghBS3ViqnpjAOBgNVHQ8BAf8EBAMCB4AwXQYDVR0SBFYwVIZSaHR0cHM6Ly9naXRodWIuY29tL2V1LWRpZ2l0YWwtaWRlbnRpdHktd2FsbGV0L2FyY2hpdGVjdHVyZS1hbmQtcmVmZXJlbmNlLWZyYW1ld29yazAKBggqhkjOPQQDAgNpADBmAjEAvBREn4wUiQFfTzNx6w34ICqvhT97eyYUc5nxW/jmjzAM8FkCASKh3p/sS7MiOLfcAjEA2no4L7L7MKGHrd1n5j+CGgNBsdnXPE2d/T3mebxKrd28bmTrsNPgaNnCvsqNXYpl",
//            "MIIDHTCCAqOgAwIBAgIUVqjgtJqf4hUYJkqdYzi+0xwhwFYwCgYIKoZIzj0EAwMwXDEeMBwGA1UEAwwVUElEIElzc3VlciBDQSAtIFVUIDAxMS0wKwYDVQQKDCRFVURJIFdhbGxldCBSZWZlcmVuY2UgSW1wbGVtZW50YXRpb24xCzAJBgNVBAYTAlVUMB4XDTIzMDkwMTE4MzQxN1oXDTMyMTEyNzE4MzQxNlowXDEeMBwGA1UEAwwVUElEIElzc3VlciBDQSAtIFVUIDAxMS0wKwYDVQQKDCRFVURJIFdhbGxldCBSZWZlcmVuY2UgSW1wbGVtZW50YXRpb24xCzAJBgNVBAYTAlVUMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEFg5Shfsxp5R/UFIEKS3L27dwnFhnjSgUh2btKOQEnfb3doyeqMAvBtUMlClhsF3uefKinCw08NB31rwC+dtj6X/LE3n2C9jROIUN8PrnlLS5Qs4Rs4ZU5OIgztoaO8G9o4IBJDCCASAwEgYDVR0TAQH/BAgwBgEB/wIBADAfBgNVHSMEGDAWgBSzbLiRFxzXpBpmMYdC4YvAQMyVGzAWBgNVHSUBAf8EDDAKBggrgQICAAABBzBDBgNVHR8EPDA6MDigNqA0hjJodHRwczovL3ByZXByb2QucGtpLmV1ZGl3LmRldi9jcmwvcGlkX0NBX1VUXzAxLmNybDAdBgNVHQ4EFgQUs2y4kRcc16QaZjGHQuGLwEDMlRswDgYDVR0PAQH/BAQDAgEGMF0GA1UdEgRWMFSGUmh0dHBzOi8vZ2l0aHViLmNvbS9ldS1kaWdpdGFsLWlkZW50aXR5LXdhbGxldC9hcmNoaXRlY3R1cmUtYW5kLXJlZmVyZW5jZS1mcmFtZXdvcmswCgYIKoZIzj0EAwMDaAAwZQIwaXUA3j++xl/tdD76tXEWCikfM1CaRz4vzBC7NS0wCdItKiz6HZeV8EPtNCnsfKpNAjEAqrdeKDnr5Kwf8BA7tATehxNlOV4Hnc10XO1XULtigCwb49RpkqlS2Hul+DpqObUs",
        ).map { pemToCertificate(it) }

        val profileValidation = spyk(ProfileValidation.DEFAULT)
        val trustStore = ReaderTrustStoreImpl(
            trustedCertificates = listOf(certificateChain.last()),
            profileValidation = profileValidation,
            errorLogger = { tag: String, msg: String, th: Throwable ->
                println("$tag:  $msg")
                th.printStackTrace()
            }
        )
        val result = trustStore.validateCertificationTrustPath(certificateChain)
        assertTrue(result)

        verify(exactly = 1) {
            profileValidation.validate(certificateChain, certificateChain.last())
        }
    }
}

