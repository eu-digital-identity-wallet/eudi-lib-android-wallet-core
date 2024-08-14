package eu.europa.ec.eudi.wallet.util

import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64

public fun parseCertificateFromSdJwt(credential: String): X509Certificate {
    val headerString = credential.split(".").first()
    val headerJson =
        JSONObject(String(Base64.getUrlDecoder().decode(headerString)))
    val keyString =
        headerJson.getJSONArray("x5c").getString(0).replace("\n", "")

    val pemKey = "-----BEGIN CERTIFICATE-----\n" +
            "${keyString}\n" +
            "-----END CERTIFICATE-----"

    val certificateFactory: CertificateFactory =
        CertificateFactory.getInstance("X.509")
    return certificateFactory.generateCertificate(ByteArrayInputStream(pemKey.toByteArray())) as X509Certificate
}