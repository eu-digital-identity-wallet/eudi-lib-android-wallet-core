package eu.europa.ec.eudi.wallet.util

import com.nimbusds.jose.jwk.ECKey
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.util.Base64

const val ZKP_ISSUER_CERT = "-----BEGIN CERTIFICATE-----\n" +
        "MIICdDCCAhugAwIBAgIBAjAKBggqhkjOPQQDAjCBiDELMAkGA1UEBhMCREUxDzANBgNVBAcMBkJlcmxpbjEdMBsGA1UECgwUQnVuZGVzZHJ1Y2tlcmVpIEdtYkgxETAPBgNVBAsMCFQgQ1MgSURFMTYwNAYDVQQDDC1TUFJJTkQgRnVua2UgRVVESSBXYWxsZXQgUHJvdG90eXBlIElzc3VpbmcgQ0EwHhcNMjQwNTMxMDgxMzE3WhcNMjUwNzA1MDgxMzE3WjBsMQswCQYDVQQGEwJERTEdMBsGA1UECgwUQnVuZGVzZHJ1Y2tlcmVpIEdtYkgxCjAIBgNVBAsMAUkxMjAwBgNVBAMMKVNQUklORCBGdW5rZSBFVURJIFdhbGxldCBQcm90b3R5cGUgSXNzdWVyMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEOFBq4YMKg4w5fTifsytwBuJf/7E7VhRPXiNm52S3q1ETIgBdXyDK3kVxGxgeHPivLP3uuMvS6iDEc7qMxmvduKOBkDCBjTAdBgNVHQ4EFgQUiPhCkLErDXPLW2/J0WVeghyw+mIwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCB4AwLQYDVR0RBCYwJIIiZGVtby5waWQtaXNzdWVyLmJ1bmRlc2RydWNrZXJlaS5kZTAfBgNVHSMEGDAWgBTUVhjAiTjoDliEGMl2Yr+ru8WQvjAKBggqhkjOPQQDAgNHADBEAiAbf5TzkcQzhfWoIoyi1VN7d8I9BsFKm1MWluRph2byGQIgKYkdrNf2xXPjVSbjW/U/5S5vAEC5XxcOanusOBroBbU=\n" +
        "-----END CERTIFICATE-----"

fun parseCertificateFromSdJwt(credential: String): X509Certificate {
    val headerString = credential.split(".").first()
    val headerJson =
        JSONObject(String(Base64.getUrlDecoder().decode(headerString)))
    val keyString =
        headerJson.getJSONArray("x5c").getString(0).replace("\n", "")

    val pemKey = "-----BEGIN CERTIFICATE-----\n" +
            "${keyString}\n" +
            "-----END CERTIFICATE-----"

    return x509Certificate(pemKey)
}

@Throws(com.nimbusds.jose.JOSEException::class)
fun getECPublicKeyFromCert(certString: String): ECPublicKey {
    val certificate = x509Certificate(certString)
    val ecKey = ECKey.parse(certificate)
    return ecKey.toECPublicKey()
}

fun x509Certificate(pemKey: String): X509Certificate {
    val certificateFactory: CertificateFactory = CertificateFactory.getInstance("X.509")
    return certificateFactory.generateCertificate(ByteArrayInputStream(pemKey.toByteArray())) as X509Certificate
}