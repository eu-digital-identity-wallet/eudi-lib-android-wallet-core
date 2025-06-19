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

package eu.europa.ec.eudi.wallet.transactionLogging.presentation.parsing

import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transfer.openId4vp.FORMAT_MSO_MDOC
import eu.europa.ec.eudi.wallet.transfer.openId4vp.FORMAT_SD_JWT_VC
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VpParserTest {

    private val metadata = listOf(
        TransactionLog.Metadata.IndexBased(
            index = 0,
            format = FORMAT_SD_JWT_VC,
            issuerMetadata = IssuerMetadata(
                documentConfigurationIdentifier = "urn:eu.europa.ec.eudi:pid:1",
                display = emptyList(),
                claims = emptyList(),
                credentialIssuerIdentifier = "http://issuer.example.com",
                issuerDisplay = null,
            ).toJson()
        ).toJson(),
        TransactionLog.Metadata.IndexBased(
            index = 1,
            format = FORMAT_MSO_MDOC,
            issuerMetadata = IssuerMetadata(
                documentConfigurationIdentifier = "org.iso.18013.5.1.mDL",
                display = emptyList(),
                claims = emptyList(),
                credentialIssuerIdentifier = "http://issuer.example.com",
                issuerDisplay = null,
            ).toJson()
        ).toJson()

    )

    @Test
    fun `test parseVp with only one mso_mdoc response containing on document`() {
        val rawResponse = getResourceAsByteArray("vp_response_2.json")
        val result = parseVp(rawResponse, metadata)
        assertEquals(2, result.size)

        val sdJwt = result.first { it.format is SdJwtVcFormat }
        val sdJwtFormat = sdJwt.format as SdJwtVcFormat

        assertEquals("urn:eu.europa.ec.eudi:pid:1", sdJwtFormat.vct)
        assertTrue {
            sdJwt.claims.any { it.path == listOf("family_name") }
        }
        assertTrue {
            sdJwt.claims.any { it.path == listOf("given_name") }
        }

        val mdoc = result.first { it.format is MsoMdocFormat }
        val mdocFormat = mdoc.format as MsoMdocFormat

        assertEquals("org.iso.18013.5.1.mDL", mdocFormat.docType)
        assertEquals(4, mdoc.claims.size)
        assertTrue {
            mdoc.claims.any { it.path == listOf("org.iso.18013.5.1", "family_name") }
        }
        assertTrue {
            mdoc.claims.any { it.path == listOf("org.iso.18013.5.1", "given_name") }
        }
        assertTrue {
            mdoc.claims.any { it.path == listOf("org.iso.18013.5.1", "age_over_18") }
        }
        assertTrue {
            mdoc.claims.any { it.path == listOf("org.iso.18013.5.1", "driving_privileges") }
        }

    }
}