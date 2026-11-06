/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicyException
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.ResponseResult
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.multipaz.cbor.Tstr
import org.multipaz.claim.MdocClaim
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.crypto.X509CertChain
import org.multipaz.crypto.fromJavaX509Certificates
import org.multipaz.request.OpenID4VPRequesterIdentity
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Certificates, a document, a configuration and assertions for tests that resolve signed OpenID4VP
 * requests with the OpenID4VP library.
 *
 * [verifierChain] is trusted. [attackerChain] carries the verifier's leaf certificate with a
 * certificate of the attacker as its issuer, and is not trusted.
 */
internal class ReaderTrustFixture {

    private val rootKeys = ecKeyPair()
    private val root = certificate(ROOT_NAME, ROOT_NAME, rootKeys.public, rootKeys.private, ca = true)
    private val verifierKeys = ecKeyPair()
    private val leaf = certificate(
        subject = "CN=Test Verifier",
        issuer = ROOT_NAME,
        subjectKey = verifierKeys.public,
        signerKey = rootKeys.private,
        ca = false,
        dnsName = VERIFIER_DNS,
    )
    private val attackerCaKeys = ecKeyPair()
    private val attackerCa = certificate(
        ATTACKER_CA_NAME,
        ATTACKER_CA_NAME,
        attackerCaKeys.public,
        attackerCaKeys.private,
        ca = true,
    )

    /** The private key of the verifier's leaf certificate. */
    val verifierKey: PrivateKey = verifierKeys.private

    val verifierChain: List<X509Certificate> = listOf(leaf, root)
    val attackerChain: List<X509Certificate> = listOf(leaf, attackerCa)

    /** Trusts [verifierChain] only. */
    val readerTrustStore: ReaderTrustStore = mockk {
        every { validateCertificationTrustPath(any()) } answers {
            firstArg<List<X509Certificate>>() == verifierChain
        }
        every { createCertificationTrustPath(any()) } returns null
    }

    val x509SanDnsClientId = "x509_san_dns:$VERIFIER_DNS"

    val x509HashClientId = "x509_hash:" + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(MessageDigest.getInstance("SHA-256").digest(leaf.encoded))

    /** A `verifier_info` with a registration certificate. */
    val verifierInfo: JsonArray = buildJsonArray {
        addJsonObject {
            put("format", "registration_cert")
            put("data", "registration-certificate")
        }
    }

    /** A DCQL query for the family name of an mDL. */
    val dcql: JsonObject = buildJsonObject {
        putJsonArray("credentials") {
            addJsonObject {
                put("id", "mdl")
                put("format", "mso_mdoc")
                putJsonObject("meta") { put("doctype_value", MDL_DOCTYPE) }
                putJsonArray("claims") {
                    addJsonObject {
                        putJsonArray("path") {
                            add(MDL_NAMESPACE)
                            add("family_name")
                        }
                    }
                }
            }
        }
    }

    fun config(): OpenId4VpConfig = OpenId4VpConfig.Builder()
        .withClientIdSchemes(ClientIdScheme.X509SanDns, ClientIdScheme.X509Hash, ClientIdScheme.RedirectUri)
        .withSchemes("openid4vp")
        .withFormats(Format.MsoMdoc.ES256)
        .withEncryptionPolicy(OpenId4VpConfig.EncryptionPolicy { })
        .build()

    fun nonce(): String = UUID.randomUUID().toString()

    /** Signs [claims] with [signingKey] as a request object that carries [x5c]. */
    fun requestObject(claims: JsonObject, x5c: List<X509Certificate>, signingKey: PrivateKey): String {
        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .type(JOSEObjectType("oauth-authz-req+jwt"))
            .x509CertChain(x5c.map { com.nimbusds.jose.util.Base64.encode(it.encoded) })
            .build()
        return SignedJWT(header, JWTClaimsSet.parse(claims.toString()))
            .apply { sign(ECDSASigner(signingKey as ECPrivateKey)) }
            .serialize()
    }

    /** A document manager that holds one mDL. */
    fun documentManager(): DocumentManager {
        val documents = listOf(mdl())
        return mockk {
            every { getDocuments(predicate = any()) } returns documents
            every { getDocuments(predicate = null) } returns documents
        }
    }

    /** Asserts that [processed] is reported with [verifierChain] and as trusted. */
    fun assertVerifierVerdict(processed: RequestProcessor.ProcessedRequest.Success) {
        val identity = assertIs<OpenID4VPRequesterIdentity>(processed.requester.requesterIdentities.single())
        assertEquals(X509CertChain.fromJavaX509Certificates(verifierChain), identity.certChain)
        assertNotNull(processed.trustMetadata, "Expected trust metadata")
    }

    /** Asserts that [processed] is reported with no requester identity and no trust verdict. */
    fun assertUnauthenticated(processed: RequestProcessor.ProcessedRequest.Success) {
        assertTrue(
            processed.requester.requesterIdentities.isEmpty(),
            "Expected no requester identity; got ${processed.requester.requesterIdentities}",
        )
        assertNull(processed.trustMetadata, "Expected no trust metadata")
    }

    /** Returns true when [result] failed because the reader authentication policy rejected it. */
    fun isRejectedByPolicy(result: ResponseResult): Boolean =
        result is ResponseResult.Failure &&
            generateSequence(result.throwable) { it.cause }.any { it is ReaderAuthPolicyException }

    private fun mdl(): IssuedDocument {
        val credential = mockk<SecureAreaBoundCredential> {
            coEvery { getClaims(documentTypeRepository = null) } returns listOf(
                MdocClaim(
                    displayName = "family_name",
                    attribute = null,
                    docType = MDL_DOCTYPE,
                    namespaceName = MDL_NAMESPACE,
                    dataElementName = "family_name",
                    value = Tstr("Doe"),
                ),
            )
        }
        return mockk {
            every { format } returns MsoMdocFormat(MDL_DOCTYPE)
            coEvery { findCredential(now = any()) } returns credential
        }
    }

    companion object {
        const val VERIFIER_DNS = "verifier.example"
        const val VERIFIER_ORIGIN = "https://verifier.example"

        private const val ROOT_NAME = "CN=Test Root"
        private const val ATTACKER_CA_NAME = "CN=Test Attacker"
        private const val MDL_DOCTYPE = "org.iso.18013.5.1.mDL"
        private const val MDL_NAMESPACE = "org.iso.18013.5.1"

        private fun ecKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC")
            .apply { initialize(ECGenParameterSpec("secp256r1")) }
            .generateKeyPair()

        private fun certificate(
            subject: String,
            issuer: String,
            subjectKey: PublicKey,
            signerKey: PrivateKey,
            ca: Boolean,
            dnsName: String? = null,
        ): X509Certificate {
            val now = System.currentTimeMillis()
            val builder = JcaX509v3CertificateBuilder(
                X500Name(issuer),
                BigInteger(64, SecureRandom()),
                Date(now - 86_400_000L),
                Date(now + 30 * 86_400_000L),
                X500Name(subject),
                subjectKey,
            ).apply {
                addExtension(Extension.basicConstraints, true, BasicConstraints(ca))
                if (dnsName != null) {
                    addExtension(
                        Extension.subjectAlternativeName,
                        false,
                        GeneralNames(GeneralName(GeneralName.dNSName, dnsName)),
                    )
                }
            }
            val signer = JcaContentSignerBuilder("SHA256withECDSA").build(signerKey)
            return JcaX509CertificateConverter().getCertificate(builder.build(signer))
        }
    }
}
