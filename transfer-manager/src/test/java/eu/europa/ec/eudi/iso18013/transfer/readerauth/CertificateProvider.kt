/*
 *  Copyright (c) 2023-2026 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package eu.europa.ec.eudi.iso18013.transfer.readerauth

import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.CRLDistPoint
import org.bouncycastle.asn1.x509.CRLReason
import org.bouncycastle.asn1.x509.DistributionPoint
import org.bouncycastle.asn1.x509.DistributionPointName
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.X509v2CRLBuilder
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter
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

private const val signatureAlgorithm: String = "SHA256withECDSA"
private val extUtils = JcaX509ExtensionUtils()
private val trustedKeyPair: KeyPair by lazy {
    KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()
}

val trustedCertificate: X509Certificate by lazy {
    val serialNumber = BigInteger(64, SecureRandom())
    val issuer = X500Name("CN=Root CA")
    val subject = X500Name("CN=Root CA")
    val notBefore = Date.from(Instant.now().minusSeconds(86400))
    val notAfter = Date(notBefore.time + 30 * 86400000L)
    val builder = JcaX509v3CertificateBuilder(
        issuer,
        serialNumber,
        notBefore,
        notAfter,
        subject,
        trustedKeyPair.public
    ).apply {
        // Add required extensions for a CA certificate
        addExtension(
            Extension.basicConstraints,
            true,  // Make this a critical extension
            BasicConstraints(true)  // isCA=true
        )
        addExtension(
            Extension.keyUsage,
            true,  // Make this a critical extension
            KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign)  // Key can be used to sign certificates and CRLs
        )
        addExtension(
            Extension.subjectKeyIdentifier,
            false,
            extUtils.createSubjectKeyIdentifier(trustedKeyPair.public)
        )
        // Self-signed CA also has the same subject and authority key identifiers
        addExtension(
            Extension.authorityKeyIdentifier,
            false,
            extUtils.createAuthorityKeyIdentifier(trustedKeyPair.public)
        )
    }
    val signer = JcaContentSignerBuilder(signatureAlgorithm).build(trustedKeyPair.private)
    JcaX509CertificateConverter().getCertificate(builder.build(signer))
}

val validCertificate: X509Certificate by lazy {
    val keyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()
    val serialNumber = BigInteger(64, SecureRandom())
    val issuer = X500Name(trustedCertificate.subjectX500Principal.name)
    val subject = X500Name("CN=ValidCertificate")
    val notBefore = Date()
    val notAfter = Date(notBefore.time + 30 * 86400000L)
    val builder = JcaX509v3CertificateBuilder(
        issuer,
        serialNumber,
        notBefore,
        notAfter,
        subject,
        keyPair.public
    ).apply {
        // Basic constraints for leaf certificate (not a CA)
        addExtension(
            Extension.basicConstraints,
            true,
            BasicConstraints(false)  // isCA=false
        )

        addExtension(
            Extension.subjectKeyIdentifier,
            false,
            extUtils.createSubjectKeyIdentifier(keyPair.public)
        )
        addExtension(
            Extension.authorityKeyIdentifier,
            false,
            extUtils.createAuthorityKeyIdentifier(trustedCertificate)
        )
        addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.digitalSignature)
        )
        addExtension(
            Extension.extendedKeyUsage,
            false,
            ExtendedKeyUsage.getInstance(
                ASN1EncodableVector().apply {
                    add(ASN1ObjectIdentifier("1.0.18013.5.1.6"))
                }.let { DERSequence(it) }
            )
        )

        // Add CRL Distribution Points
        val crlDp = GeneralName(
            GeneralName.uniformResourceIdentifier,
            "https://example.com/crl.pem"
        )
        val generalNames = GeneralNames(crlDp)
        val distPointName = DistributionPointName(generalNames)
        val distPoint = DistributionPoint(distPointName, null, null)
        addExtension(
            Extension.cRLDistributionPoints,
            false,
            CRLDistPoint(arrayOf(distPoint))
        )
    }
    val signer = JcaContentSignerBuilder(signatureAlgorithm).build(trustedKeyPair.private)
    JcaX509CertificateConverter().getCertificate(builder.build(signer))
}

private val untrustedKeyPair = KeyPairGenerator.getInstance("RSA").apply {
    initialize(2048)
}.generateKeyPair()

private val untrustedRoot: X509Certificate
    get() {
        val serialNumber = BigInteger(64, SecureRandom())
        val issuer = X500Name("CN=Untrusted Root CA")
        val subject = X500Name("CN=Untrusted Root CA")
        val notBefore = Date.from(Instant.now().minusSeconds(86400))
        val notAfter = Date(notBefore.time + 30 * 86400000L)
        val builder = JcaX509v3CertificateBuilder(
            issuer,
            serialNumber,
            notBefore,
            notAfter,
            subject,
            untrustedKeyPair.public
        ).apply {
            addExtension(
                Extension.basicConstraints,
                true,
                BasicConstraints(true)  // isCA=true
            )
            addExtension(
                Extension.keyUsage,
                true,
                KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign)
            )
            addExtension(
                Extension.subjectKeyIdentifier,
                false,
                extUtils.createSubjectKeyIdentifier(untrustedKeyPair.public)
            )
            addExtension(
                Extension.authorityKeyIdentifier,
                false,
                extUtils.createAuthorityKeyIdentifier(untrustedKeyPair.public)
            )
        }
        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(untrustedKeyPair.private)
        return JcaX509CertificateConverter().getCertificate(builder.build(signer))
    }

/**
 * Builds a 3-certificate chain `[leaf, intermediate, root]` under the trusted root.
 *
 * Every certificate's AuthorityKeyIdentifier is correctly set to its issuer's
 * SubjectKeyIdentifier, so an ISO 18013-5 Annex B Authority Key profile
 * validator must accept this chain.
 */
val validThreeCertChain: List<X509Certificate> by lazy {
    buildThreeCertChain(tamperIntermediateAki = false)
}

/**
 * Builds a 3-certificate chain `[leaf, intermediate, root]` where the
 * intermediate certificate's AuthorityKeyIdentifier is deliberately corrupted
 * so it does NOT match the root's SubjectKeyIdentifier.
 *
 * The chain is still cryptographically signed correctly (PKIX path validation
 * passes), exposing the profile gap described in issue 114.
 */
val threeCertChainWithIntermediateAkiMismatch: List<X509Certificate> by lazy {
    buildThreeCertChain(tamperIntermediateAki = true)
}

private fun buildThreeCertChain(tamperIntermediateAki: Boolean): List<X509Certificate> {
    // Root CA is the existing trustedCertificate (self-signed).
    val root = trustedCertificate

    // Intermediate CA signed by the root.
    val intermediateKeyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()
    val intermediateSerial = BigInteger(64, SecureRandom())
    val intermediateIssuer = X500Name(root.subjectX500Principal.name)
    val intermediateSubject = X500Name("CN=Intermediate CA")
    val intermediateNotBefore = Date.from(Instant.now().minusSeconds(3600))
    val intermediateNotAfter = Date(intermediateNotBefore.time + 30 * 86400000L)

    val intermediateAkiExtension = if (tamperIntermediateAki) {
        // Replace the AKI key identifier with bytes that do NOT match the root's SKI.
        val bogus = ByteArray(20) { 0xCC.toByte() }
        AuthorityKeyIdentifier(bogus)
    } else {
        extUtils.createAuthorityKeyIdentifier(root)
    }

    val intermediateBuilder = JcaX509v3CertificateBuilder(
        intermediateIssuer,
        intermediateSerial,
        intermediateNotBefore,
        intermediateNotAfter,
        intermediateSubject,
        intermediateKeyPair.public
    ).apply {
        addExtension(Extension.basicConstraints, true, BasicConstraints(true))
        addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign)
        )
        addExtension(
            Extension.subjectKeyIdentifier,
            false,
            extUtils.createSubjectKeyIdentifier(intermediateKeyPair.public)
        )
        addExtension(Extension.authorityKeyIdentifier, false, intermediateAkiExtension)
    }
    val intermediateSigner =
        JcaContentSignerBuilder(signatureAlgorithm).build(trustedKeyPair.private)
    val intermediate: X509Certificate = JcaX509CertificateConverter()
        .getCertificate(intermediateBuilder.build(intermediateSigner))

    // Leaf certificate signed by the intermediate.
    val leafKeyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()
    val leafSerial = BigInteger(64, SecureRandom())
    val leafIssuer = X500Name(intermediate.subjectX500Principal.name)
    val leafSubject = X500Name("CN=ValidCertificate")
    val leafNotBefore = Date()
    val leafNotAfter = Date(leafNotBefore.time + 30 * 86400000L)
    val leafBuilder = JcaX509v3CertificateBuilder(
        leafIssuer,
        leafSerial,
        leafNotBefore,
        leafNotAfter,
        leafSubject,
        leafKeyPair.public
    ).apply {
        addExtension(Extension.basicConstraints, true, BasicConstraints(false))
        addExtension(
            Extension.subjectKeyIdentifier,
            false,
            extUtils.createSubjectKeyIdentifier(leafKeyPair.public)
        )
        addExtension(
            Extension.authorityKeyIdentifier,
            false,
            extUtils.createAuthorityKeyIdentifier(intermediate)
        )
        addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature))
    }
    val leafSigner =
        JcaContentSignerBuilder(signatureAlgorithm).build(intermediateKeyPair.private)
    val leaf: X509Certificate = JcaX509CertificateConverter()
        .getCertificate(leafBuilder.build(leafSigner))

    return listOf(leaf, intermediate, root)
}

val revokedCertificateCRL: java.security.cert.X509CRL by lazy {
    val issuerName = X500Name(trustedCertificate.subjectX500Principal.name)
    val now = Date()
    val nextUpdate = Date(now.time + 30 * 86400000L)

    val crlBuilder = X509v2CRLBuilder(issuerName, now).apply {
        addCRLEntry(validCertificate.serialNumber, now, CRLReason.unspecified)
        setNextUpdate(nextUpdate)
        addExtension(
            Extension.authorityKeyIdentifier,
            false,
            extUtils.createAuthorityKeyIdentifier(trustedKeyPair.public)
        )
    }

    val signer = JcaContentSignerBuilder(signatureAlgorithm).build(trustedKeyPair.private)
    JcaX509CRLConverter().getCRL(crlBuilder.build(signer))
}

private fun buildLeafCertificateWithValidity(
    notBefore: Date,
    notAfter: Date,
): X509Certificate {
    val keyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()
    val serialNumber = BigInteger(64, SecureRandom())
    val issuer = X500Name(trustedCertificate.subjectX500Principal.name)
    val subject = X500Name("CN=ValidityTestCertificate")
    val builder = JcaX509v3CertificateBuilder(
        issuer,
        serialNumber,
        notBefore,
        notAfter,
        subject,
        keyPair.public,
    )
    val signer = JcaContentSignerBuilder(signatureAlgorithm).build(trustedKeyPair.private)
    return JcaX509CertificateConverter().getCertificate(builder.build(signer))
}

val notYetValidCertificate: X509Certificate by lazy {
    // notBefore starts 1 day in the future
    val notBefore = Date.from(Instant.now().plusSeconds(86400))
    val notAfter = Date(notBefore.time + 30 * 86400000L)
    buildLeafCertificateWithValidity(notBefore, notAfter)
}

val expiredCertificate: X509Certificate by lazy {
    // notAfter ended 1 day ago
    val notAfter = Date.from(Instant.now().minusSeconds(86400))
    val notBefore = Date(notAfter.time - 30 * 86400000L)
    buildLeafCertificateWithValidity(notBefore, notAfter)
}

val tooLongButCurrentlyValidCertificate: X509Certificate by lazy {
    // Currently valid window, but total duration exceeds MAX_VALIDITY_PERIOD_DAYS (1187)
    val notBefore = Date.from(Instant.now().minusSeconds(86400))
    // 1500 days after notBefore
    val notAfter = Date(notBefore.time + 1500L * 86400000L)
    buildLeafCertificateWithValidity(notBefore, notAfter)
}

val invalidCertificate: X509Certificate by lazy {
    val keyPair = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)
    }.generateKeyPair()
    val serialNumber = BigInteger(64, SecureRandom())
    val issuer = X500Name(untrustedRoot.issuerX500Principal.name)
    val subject = X500Name("CN=ValidCertificate")
    val notBefore = Date.from(Instant.now().minusSeconds(10))
    // add 10 years to notBefore
    val notAfter = Date.from(notBefore.toInstant().plusSeconds(10 * 365 * 86400L))
    val builder = JcaX509v3CertificateBuilder(
        issuer,
        serialNumber,
        notBefore,
        notAfter,
        subject,
        keyPair.public
    )
    val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(untrustedKeyPair.private)
    JcaX509CertificateConverter().getCertificate(builder.build(signer))
}

fun loadCert(): X509Certificate = validCertificate

fun loadInvalidCert(): X509Certificate = invalidCertificate

fun loadTrustCert(): X509Certificate = trustedCertificate

fun loadValidThreeCertChain(): List<X509Certificate> = validThreeCertChain

fun loadThreeCertChainWithIntermediateAkiMismatch(): List<X509Certificate> =
    threeCertChainWithIntermediateAkiMismatch

fun loadNotYetValidCert(): X509Certificate = notYetValidCertificate

fun loadExpiredCert(): X509Certificate = expiredCertificate

fun loadTooLongButCurrentlyValidCert(): X509Certificate = tooLongButCurrentlyValidCertificate
