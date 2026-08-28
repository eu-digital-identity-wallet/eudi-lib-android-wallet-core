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
package eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql

import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificate
import eu.europa.ec.eudi.wallet.registration.RegistrationCertificateResult
import io.mockk.mockk
import org.junit.Assert.assertSame
import org.junit.Test
import org.multipaz.presentment.CredentialPresentmentData

class ProcessedDcqlRequestTest {

    @Test
    fun `withPresentmentData preserves the resolved relying party registration`() {
        val registration = RegistrationCertificateResult.Verified(
            registration = RegistrationCertificate(name = "Nordic Bank"),
        )
        val request = ProcessedDcqlRequest(
            resolvedRequestObject = mockk(relaxed = true),
            documentManager = mockk(relaxed = true),
            presentmentData = mockk(relaxed = true),
            requester = mockk(relaxed = true),
            trustMetadata = null,
            msoMdocNonce = "nonce",
            wrpRegistration = registration,
            readerAuthPolicy = ReaderAuthPolicy.DoNotEnforce()
        )

        val copy = request.withPresentmentData(mockk<CredentialPresentmentData>(relaxed = true))

        assertSame(registration, copy.wrpRegistration)
    }
}
