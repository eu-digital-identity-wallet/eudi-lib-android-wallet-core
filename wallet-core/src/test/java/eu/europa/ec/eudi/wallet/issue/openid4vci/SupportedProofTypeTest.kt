/*
 *  Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import com.nimbusds.jose.JWSAlgorithm
import eu.europa.ec.eudi.openid4vci.*
import eu.europa.ec.eudi.wallet.document.Algorithm
import eu.europa.ec.eudi.wallet.issue.openid4vci.SupportedProofType.ProofAlgorithm
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class SupportedProofTypeTest {

    @Test
    fun `selectProofType returns null for unsupported types`() {
        assertThrows<UnsupportedProofTypeException> {
            val credentialConfiguration = mockk<MsoMdocCredential>(relaxed = true)
            every { credentialConfiguration.proofTypesSupported } returns ProofTypesSupported(
                setOf(
                    ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES384)),
                    ProofTypeMeta.Cwt(listOf(CoseAlgorithm.ES384), listOf(CoseCurve.P_384))
                )
            )

            SupportedProofType.select(credentialConfiguration)
        }
    }

    @Test
    fun `selectProofType returns a SupportedProofType when all types are supported but only one has supported algorithms`() {
        val credentialConfiguration = mockk<MsoMdocCredential>(relaxed = true)
        every { credentialConfiguration.proofTypesSupported } returns ProofTypesSupported(
            setOf(
                ProofTypeMeta.Cwt(listOf(CoseAlgorithm.ES384), listOf(CoseCurve.P_384)),
                ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES256))
            )
        )
        val expected = SelectedProofType.Jwt(ProofAlgorithm.Jws(JWSAlgorithm.ES256, Algorithm.SHA256withECDSA))

        val selected = SupportedProofType.select(credentialConfiguration)
        assertEquals(expected, selected)
    }

    @Test
    fun `selectProofType returns a null when only one type is supported but has unsupported algorithms`() {
        assertThrows<UnsupportedProofTypeException> {
            val credentialConfiguration = mockk<MsoMdocCredential>(relaxed = true)
            every { credentialConfiguration.proofTypesSupported } returns ProofTypesSupported(
                setOf(
                    ProofTypeMeta.LdpVp,
                    ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES384))
                )
            )

            SupportedProofType.select(credentialConfiguration)
        }
    }

    @Test
    fun `selectProofType prefers jwt over cwt when both types supported`() {
        val credentialConfiguration = mockk<MsoMdocCredential>(relaxed = true)
        every { credentialConfiguration.proofTypesSupported } returns ProofTypesSupported(
            setOf(
                ProofTypeMeta.Cwt(listOf(CoseAlgorithm.ES256), listOf(CoseCurve.P_256)),
                ProofTypeMeta.Jwt(listOf(JWSAlgorithm.ES256))
            )
        )
        val expected = SelectedProofType.Jwt(ProofAlgorithm.Jws(JWSAlgorithm.ES256, Algorithm.SHA256withECDSA))

        val selected = SupportedProofType.select(credentialConfiguration)
        assertEquals(expected, selected)
    }
}