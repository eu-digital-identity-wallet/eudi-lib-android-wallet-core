package eu.europa.ec.eudi.wallet.trust

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.Date

private val extUtils = JcaX509ExtensionUtils()

// A dedicated RSA KeyPair for the Trust Anchor
private val rsaTrustedKeyPair: KeyPair by lazy {
    KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)
    }.generateKeyPair()
}

// The RSA Root CA (Trust Anchor)
val rsaTrustedRootCertificate: X509Certificate by lazy {
    val serialNumber = BigInteger(64, SecureRandom())
    val issuer = X500Name("CN=RSA Root CA")
    val subject = X500Name("CN=RSA Root CA")
    val notBefore = Date.from(Instant.now().minusSeconds(86400))
    val notAfter = Date(notBefore.time + 30 * 86400000L)

    val builder = JcaX509v3CertificateBuilder(
        issuer,
        serialNumber,
        notBefore,
        notAfter,
        subject,
        rsaTrustedKeyPair.public
    ).apply {
        addExtension(Extension.basicConstraints, true, BasicConstraints(true))
        addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign))
        // Subject Key ID
        addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(rsaTrustedKeyPair.public))
        // Authority Key ID (Self-signed)
        addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(rsaTrustedKeyPair.public))
    }

    // SIGNATURE Signed with RSA
    val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(rsaTrustedKeyPair.private)
    JcaX509CertificateConverter().getCertificate(builder.build(signer))
}

// 2. NEW: EC Intermediate KeyPair
private val ecIntermediateKeyPair: KeyPair by lazy {
    KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()
}

// EC Intermediate Certificate
val ecIntermediateCertificate: X509Certificate by lazy {
    val serialNumber = BigInteger(64, SecureRandom())
    val issuer = X500Name(rsaTrustedRootCertificate.subjectX500Principal.name) // Issuer = RSA Root
    val subject = X500Name("CN=EC Intermediate CA")
    val notBefore = Date.from(Instant.now().minusSeconds(86400))
    val notAfter = Date(notBefore.time + 30 * 86400000L)

    val builder = JcaX509v3CertificateBuilder(
        issuer,
        serialNumber,
        notBefore,
        notAfter,
        subject,
        ecIntermediateKeyPair.public // Intermediate has an EC Key
    ).apply {
        // Critical: Basic Constraints CA = TRUE
        addExtension(Extension.basicConstraints, true, BasicConstraints(true))
        addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign))

        addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(ecIntermediateKeyPair.public))
        addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(rsaTrustedRootCertificate))
    }

    // SIGNATURE: Signed by RSA Root
    val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(rsaTrustedKeyPair.private)
    JcaX509CertificateConverter().getCertificate(builder.build(signer))
}

// The EC Leaf Certificate (Verifier), signed by the RSA Root
val ecLeafSignedByIntermediateCertificate: X509Certificate by lazy {
    // The Verifier has an EC KeyPair
    val leafEcKeyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    val serialNumber = BigInteger(64, SecureRandom())
    val issuer = X500Name(ecIntermediateCertificate.subjectX500Principal.name) // Intermediate Certificate signs it
    val subject = X500Name("CN=EC Leaf Signed By RSA")
    val notBefore = Date.from(Instant.now().minusSeconds(86400))
    val notAfter = Date(notBefore.time + 30 * 86400000L)

    val builder = JcaX509v3CertificateBuilder(
        issuer,
        serialNumber,
        notBefore,
        notAfter,
        subject,
        leafEcKeyPair.public // The Leaf has an EC Public Key
    ).apply {
        addExtension(Extension.basicConstraints, true, BasicConstraints(false))
        addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature))

        // Subject Key ID (calculated from Leaf's EC key)
        addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(leafEcKeyPair.public))

        // AUTHORITY KEY: Refers to the EC Intermediate
        addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(ecIntermediateCertificate))
    }

    // SIGNATURE: Signed by the Intermediate's EC Key using ECDSA Algorithm
    val signer = JcaContentSignerBuilder("SHA256WithECDSA").build(ecIntermediateKeyPair.private)
    JcaX509CertificateConverter().getCertificate(builder.build(signer))
}