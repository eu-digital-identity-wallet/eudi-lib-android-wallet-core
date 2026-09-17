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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.transactionData

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.SerializationException
import org.multipaz.crypto.Algorithm
import org.multipaz.presentment.TransactionProtocol
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Tests for the QES transaction data model and for the transaction data types that parse it.
 */
class QesTransactionDataTest {

    @Test
    fun `a qes-approval request is parsed into its payload`() {
        val approval = QesApprovalTransactionType.parseOpenId4VpRequest(QES_APPROVAL)

        assertEquals(QesApprovalRequest.TYPE, approval.type)
        assertEquals(listOf("pid"), approval.credentialIds)
        assertEquals(listOf("sha-256"), approval.hashAlgorithms)
        assertEquals("GX0112348", approval.credentialId)
        assertEquals("eu_eidas_qes", approval.signatureQualifier)
        assertEquals(1, approval.numSignatures)
        assertEquals("2.16.840.1.101.3.4.2.1", approval.hashAlgorithmOid)

        val digest = approval.documentDigests.single()
        assertEquals("Service Agreement 2026-09", digest.label)
        assertEquals("sTOgwOm+474gFj0q0x1iSNspKqbcse4IeiqlDg/HWuI=", digest.hash)
        assertEquals(DocumentDigest.HASH_TYPE_SODR, digest.hashType)
        assertEquals("https://qtsp.example/docs/contract-2026-09.pdf", digest.href)
        assertEquals(AccessControlMethod.ACCESS_MODE_PUBLIC, digest.access?.accessMode)
        assertEquals("HZQzZmMAIWekfGH0/ZKW1nsdt0xg3H6bZYztgsMTLw0=", digest.checksum?.value)
        assertEquals("2.16.840.1.101.3.4.2.1", digest.checksum?.algorithmOid)
    }

    @Test
    fun `parsing keeps the transaction data as it was received`() {
        val serialized = base64Url(QES_APPROVAL)

        val transactionData = QesApprovalTransactionType.parseJson(serialized)

        assertSame(QesApprovalTransactionType, transactionData.type)
        assertEquals(TransactionProtocol.OPENID4VP, transactionData.protocol)
        assertEquals(serialized, transactionData.rawBytes)
        assertEquals("GX0112348", transactionData.payload.credentialId)
    }

    @Test
    fun `the hash algorithms the relying party accepts are read from the request`() {
        val transactionData = QesApprovalTransactionType.parseJson(base64Url(QES_APPROVAL))

        assertEquals(listOf(Algorithm.SHA256), transactionData.hashAlgorithms)
    }

    @Test
    fun `no hash algorithms are read when the request states none`() {
        val transactionData = QesApprovalTransactionType.parseJson(
            base64Url(QES_APPROVAL.withoutField("\"transaction_data_hashes_alg\": [\"sha-256\"],"))
        )

        assertNull(transactionData.hashAlgorithms)
    }

    @Test
    fun `a document digest without a hash type is a data to be signed representation`() {
        val approval = QesApprovalTransactionType.parseOpenId4VpRequest(
            QES_APPROVAL.withoutField("\"hashType\": \"sodr\",")
        )

        assertEquals(DocumentDigest.HASH_TYPE_DTBSR, approval.documentDigests.single().hashType)
    }

    @Test
    fun `a request with an unknown field is rejected`() {
        assertFailsWith<SerializationException> {
            QesApprovalTransactionType.parseOpenId4VpRequest(
                QES_APPROVAL.withField("\"documentLocations\": []")
            )
        }
    }

    @Test
    fun `a request without a required field is rejected`() {
        val error = assertFailsWith<SerializationException> {
            QesApprovalTransactionType.parseOpenId4VpRequest(QES_APPROVAL_WITHOUT_HASH_ALGORITHM)
        }

        assertContains(error.message.orEmpty(), "hashAlgorithmOID")
    }

    @Test
    fun `a request of another type is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            QesApprovalTransactionType.parseOpenId4VpRequest(
                QES_APPROVAL.replace(QesApprovalRequest.TYPE, QesRequest.TYPE)
            )
        }
    }

    @Test
    fun `a request that authorizes no signature is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            QesApprovalTransactionType.parseOpenId4VpRequest(
                QES_APPROVAL.replace("\"numSignatures\": 1", "\"numSignatures\": 0")
            )
        }
    }

    @Test
    fun `a request that names neither a credential nor a signature qualifier is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            QesApprovalTransactionType.parseOpenId4VpRequest(
                QES_APPROVAL.withoutField("\"credentialID\": \"GX0112348\",")
                    .withoutField("\"signatureQualifier\": \"eu_eidas_qes\",")
            )
        }
    }

    @Test
    fun `a request without a document is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            QesApprovalTransactionType.parseOpenId4VpRequest(QES_APPROVAL_WITHOUT_DOCUMENTS)
        }
    }

    @Test
    fun `a document digest with an unknown hash type is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            QesApprovalTransactionType.parseOpenId4VpRequest(
                QES_APPROVAL.replace("\"hashType\": \"sodr\"", "\"hashType\": \"sha256\"")
            )
        }
    }

    @Test
    fun `a document that needs a password but carries none is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            QesApprovalTransactionType.parseOpenId4VpRequest(
                QES_APPROVAL.replace("\"type\": \"public\"", "\"type\": \"OTP\"")
            )
        }
    }

    @Test
    fun `a qes request that carries the document is parsed into its payload`() {
        val request = QesRequestTransactionType.parseOpenId4VpRequest(QES_WITH_DOCUMENT)

        assertEquals(QesRequest.TYPE, request.type)
        assertEquals(listOf("pid"), request.credentialIds)

        val signatureRequest = assertIs<SignatureRequest.WithDocument>(
            request.signatureRequests.single()
        )
        assertEquals("eu_eidas_qes", signatureRequest.signatureQualifier)
        assertEquals("https://rp.example/signature", signatureRequest.responseUri)
        assertEquals(SignatureRequest.SIGNATURE_FORMAT_PADES, signatureRequest.signatureFormat)
        assertEquals("AdES-B-B", signatureRequest.conformanceLevel)
        assertEquals("JVBERi0xLjcK", signatureRequest.document)
        assertEquals(SignatureRequest.DOCUMENT_TYPE_ORIGINAL, signatureRequest.documentType)
        assertEquals("1.2.840.113549.1.1.11", signatureRequest.signAlgo)
        assertEquals(listOf(Attribute("commitmentType", "proof-of-origin")),
            signatureRequest.signedProperties)
    }

    @Test
    fun `a qes request that references the document is parsed into its payload`() {
        val request = QesRequestTransactionType.parseOpenId4VpRequest(QES_WITH_REFERENCE)

        val signatureRequest = assertIs<SignatureRequest.WithDocumentReference>(
            request.signatureRequests.single()
        )
        assertEquals("https://rp.example/docs/contract.pdf", signatureRequest.href)
        assertEquals(AccessControlMethod.ACCESS_MODE_ONE_TIME_PASSWORD,
            signatureRequest.access?.accessMode)
        assertEquals("123456", signatureRequest.access?.oneTimePassword)
        assertEquals("HZQzZmMAIWekfGH0/ZKW1nsdt0xg3H6bZYztgsMTLw0=",
            signatureRequest.checksum?.value)
    }

    @Test
    fun `a signature request that both carries and references a document is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            QesRequestTransactionType.parseOpenId4VpRequest(
                QES_WITH_DOCUMENT.replace(
                    "\"document\": \"JVBERi0xLjcK\",",
                    "\"document\": \"JVBERi0xLjcK\",\n" +
                            "        \"href\": \"https://rp.example/docs/contract.pdf\","
                )
            )
        }
    }

    @Test
    fun `a signature request without a document is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            QesRequestTransactionType.parseOpenId4VpRequest(
                QES_WITH_DOCUMENT.withoutField("\"document\": \"JVBERi0xLjcK\",")
            )
        }
    }

    @Test
    fun `a qes request without a signature request is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            QesRequestTransactionType.parseOpenId4VpRequest(QES_WITHOUT_SIGNATURE_REQUESTS)
        }
    }

    @Test
    fun `a signature request with an unknown signature format is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            QesRequestTransactionType.parseOpenId4VpRequest(
                QES_WITH_DOCUMENT.replace("\"signature_format\": \"P\"", "\"signature_format\": \"PAdES\"")
            )
        }
    }

    @Test
    fun `the transaction data of a reference verifier request is parsed`() {
        val serialized = ByteString(FROM_REFERENCE_VERIFIER.toByteArray(Charsets.US_ASCII))

        val transactionData = QesApprovalTransactionType.parseJson(serialized)

        assertEquals(serialized, transactionData.rawBytes)
        assertEquals(listOf(Algorithm.SHA256), transactionData.hashAlgorithms)

        val approval = transactionData.payload
        assertEquals(listOf("query_0"), approval.credentialIds)
        assertEquals("GX0112348", approval.credentialId)
        assertEquals("eu_eidas_qes", approval.signatureQualifier)
        assertEquals(1, approval.numSignatures)
        assertEquals("2.16.840.1.101.3.4.2.1", approval.hashAlgorithmOid)

        val digest = approval.documentDigests.single()
        assertEquals("Service Agreement 2026-09", digest.label)
        assertEquals(DocumentDigest.HASH_TYPE_SODR, digest.hashType)
        assertEquals(
            "https://public.rp-cdn.example/terms-and-conditions.pdf",
            digest.href,
        )
        assertEquals(AccessControlMethod.ACCESS_MODE_PUBLIC, digest.access?.accessMode)
        assertEquals("2.16.840.1.101.3.4.2.1", digest.checksum?.algorithmOid)
    }

    @Test
    fun `the qes transaction data of a reference verifier request is parsed`() {
        val serialized = ByteString(QES_FROM_REFERENCE_VERIFIER.toByteArray(Charsets.US_ASCII))

        val transactionData = QesRequestTransactionType.parseJson(serialized)

        assertEquals(serialized, transactionData.rawBytes)
        assertEquals(listOf(Algorithm.SHA256), transactionData.hashAlgorithms)

        val (carried, referenced) = transactionData.payload.signatureRequests
        val withDocument = assertIs<SignatureRequest.WithDocument>(carried)
        assertEquals("Contract", withDocument.label)
        assertEquals("JVBERi0xLjcK", withDocument.document)
        assertEquals(SignatureRequest.DOCUMENT_TYPE_ORIGINAL, withDocument.documentType)
        assertEquals("https://rp.example/signature", withDocument.responseUri)

        val withReference = assertIs<SignatureRequest.WithDocumentReference>(referenced)
        assertEquals("Annex", withReference.label)
        assertEquals("https://public.rp-cdn.example/annex.pdf", withReference.href)
        assertEquals(AccessControlMethod.ACCESS_MODE_PUBLIC, withReference.access?.accessMode)
    }

    private fun base64Url(json: String): ByteString =
        ByteString(
            Base64.getUrlEncoder().withoutPadding().encode(json.toByteArray(Charsets.UTF_8))
        )

    private fun String.withoutField(field: String): String = replace(field, "").also {
        check(it != this) { "'$field' is not present" }
    }

    private fun String.withField(field: String): String =
        replaceFirst("{", "{\n    $field,")

    private companion object {
        /** The transaction data of a request produced by the EC reference verifier. */
        /** The qes transaction data of a request produced by the EC reference verifier. */
        const val QES_FROM_REFERENCE_VERIFIER = "eyJ0eXBlIjoiaHR0cHM6Ly9jbG91ZHNpZ25hdHVyZWNvbnNvcnRpdW0ub3JnLzIwMjUvcWVzIiwiY3JlZGVudGlhbF9pZHMiOlsicXVlcnlfMCJdLCJzaWduYXR1cmVSZXF1ZXN0cyI6W3sic2lnbmF0dXJlUXVhbGlmaWVyIjoiZXVfZWlkYXNfcWVzIiwicmVzcG9uc2VVUkkiOiJodHRwczovL3JwLmV4YW1wbGUvc2lnbmF0dXJlIiwic2lnbmF0dXJlX2Zvcm1hdCI6IlAiLCJjb25mb3JtYW5jZV9sZXZlbCI6IkFkRVMtQi1CIiwic2lnbmVkX2VudmVsb3BlX3Byb3BlcnR5IjoiQ2VydGlmaWNhdGlvbiIsImxhYmVsIjoiQ29udHJhY3QiLCJkb2N1bWVudCI6IkpWQkVSaTB4TGpjSyIsImRvY3VtZW50VHlwZSI6InNvZCIsInNpZ25BbGdvIjoiMS4yLjg0MC4xMTM1NDkuMS4xLjExIn0seyJzaWduYXR1cmVRdWFsaWZpZXIiOiJldV9laWRhc19xZXMiLCJsYWJlbCI6IkFubmV4IiwiaHJlZiI6Imh0dHBzOi8vcHVibGljLnJwLWNkbi5leGFtcGxlL2FubmV4LnBkZiIsImFjY2VzcyI6eyJ0eXBlIjoicHVibGljIn0sImNoZWNrc3VtIjp7InZhbHVlIjoiSFpRelptTUFJV2VrZkdIMC9aS1cxbnNkdDB4ZzNINmJaWXp0Z3NNVEx3MD0iLCJhbGdvcml0aG1PSUQiOiIyLjE2Ljg0MC4xLjEwMS4zLjQuMi4xIn0sInNpZ25BbGdvIjoiMS4yLjg0MC4xMTM1NDkuMS4xLjExIn1dLCJ0cmFuc2FjdGlvbl9kYXRhX2hhc2hlc19hbGciOlsic2hhLTI1NiJdfQ"

        const val FROM_REFERENCE_VERIFIER = "eyJ0eXBlIjoiaHR0cHM6Ly9jbG91ZHNpZ25hdHVyZWNvbnNvcnRpdW0ub3JnLzIwMjUvcWVzLWFwcHJvdmFsIiwiY3JlZGVudGlhbF9pZHMiOlsicXVlcnlfMCJdLCJjcmVkZW50aWFsSUQiOiJHWDAxMTIzNDgiLCJzaWduYXR1cmVRdWFsaWZpZXIiOiJldV9laWRhc19xZXMiLCJudW1TaWduYXR1cmVzIjoxLCJkb2N1bWVudERpZ2VzdHMiOlt7ImxhYmVsIjoiU2VydmljZSBBZ3JlZW1lbnQgMjAyNi0wOSIsImhhc2giOiJzVE9nd09tKzQ3NGdGajBxMHgxaVNOc3BLcWJjc2U0SWVpcWxEZy9IV3VJPSIsImhhc2hUeXBlIjoic29kciIsImhyZWYiOiJodHRwczovL3B1YmxpYy5ycC1jZG4uZXhhbXBsZS90ZXJtcy1hbmQtY29uZGl0aW9ucy5wZGYiLCJhY2Nlc3MiOnsidHlwZSI6InB1YmxpYyJ9LCJjaGVja3N1bSI6eyJ2YWx1ZSI6IkhaUXpabU1BSVdla2ZHSDAvWktXMW5zZHQweGczSDZiWll6dGdzTVRMdzA9IiwiYWxnb3JpdGhtT0lEIjoiMi4xNi44NDAuMS4xMDEuMy40LjIuMSJ9fV0sImhhc2hBbGdvcml0aG1PSUQiOiIyLjE2Ljg0MC4xLjEwMS4zLjQuMi4xIiwidHJhbnNhY3Rpb25fZGF0YV9oYXNoZXNfYWxnIjpbInNoYS0yNTYiXX0"

        const val QES_APPROVAL = """{
    "type": "https://cloudsignatureconsortium.org/2025/qes-approval",
    "credential_ids": ["pid"],
    "transaction_data_hashes_alg": ["sha-256"],
    "credentialID": "GX0112348",
    "signatureQualifier": "eu_eidas_qes",
    "numSignatures": 1,
    "documentDigests": [
        {
            "label": "Service Agreement 2026-09",
            "hash": "sTOgwOm+474gFj0q0x1iSNspKqbcse4IeiqlDg/HWuI=",
            "hashType": "sodr",
            "href": "https://qtsp.example/docs/contract-2026-09.pdf",
            "access": {"type": "public"},
            "checksum": {
                "value": "HZQzZmMAIWekfGH0/ZKW1nsdt0xg3H6bZYztgsMTLw0=",
                "algorithmOID": "2.16.840.1.101.3.4.2.1"
            }
        }
    ],
    "hashAlgorithmOID": "2.16.840.1.101.3.4.2.1"
}"""

        const val QES_APPROVAL_WITHOUT_HASH_ALGORITHM = """{
    "type": "https://cloudsignatureconsortium.org/2025/qes-approval",
    "credential_ids": ["pid"],
    "credentialID": "GX0112348",
    "numSignatures": 1,
    "documentDigests": [
        {"hash": "sTOgwOm+474gFj0q0x1iSNspKqbcse4IeiqlDg/HWuI="}
    ]
}"""

        const val QES_APPROVAL_WITHOUT_DOCUMENTS = """{
    "type": "https://cloudsignatureconsortium.org/2025/qes-approval",
    "credential_ids": ["pid"],
    "credentialID": "GX0112348",
    "numSignatures": 1,
    "documentDigests": [],
    "hashAlgorithmOID": "2.16.840.1.101.3.4.2.1"
}"""

        const val QES_WITH_DOCUMENT = """{
    "type": "https://cloudsignatureconsortium.org/2025/qes",
    "credential_ids": ["pid"],
    "transaction_data_hashes_alg": ["sha-256"],
    "signatureRequests": [
        {
            "signatureQualifier": "eu_eidas_qes",
            "responseURI": "https://rp.example/signature",
            "signature_format": "P",
            "conformance_level": "AdES-B-B",
            "signed_envelope_property": "Certification",
            "signed_props": [{"attribute_name": "commitmentType", "attribute_value": "proof-of-origin"}],
            "label": "Service Agreement 2026-09",
            "document": "JVBERi0xLjcK",
            "documentType": "sod",
            "signAlgo": "1.2.840.113549.1.1.11"
        }
    ]
}"""

        const val QES_WITH_REFERENCE = """{
    "type": "https://cloudsignatureconsortium.org/2025/qes",
    "credential_ids": ["pid"],
    "signatureRequests": [
        {
            "signatureQualifier": "eu_eidas_qes",
            "label": "Service Agreement 2026-09",
            "href": "https://rp.example/docs/contract.pdf",
            "access": {"type": "OTP", "oneTimePassword": "123456"},
            "checksum": {
                "value": "HZQzZmMAIWekfGH0/ZKW1nsdt0xg3H6bZYztgsMTLw0=",
                "algorithmOID": "2.16.840.1.101.3.4.2.1"
            },
            "signAlgo": "1.2.840.113549.1.1.11"
        }
    ]
}"""

        const val QES_WITHOUT_SIGNATURE_REQUESTS = """{
    "type": "https://cloudsignatureconsortium.org/2025/qes",
    "credential_ids": ["pid"],
    "signatureRequests": []
}"""
    }
}
