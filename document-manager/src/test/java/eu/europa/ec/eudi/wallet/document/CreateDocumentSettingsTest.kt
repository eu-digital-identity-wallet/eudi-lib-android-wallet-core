/*
 * Copyright (c) 2024-2025 European Commission
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

package eu.europa.ec.eudi.wallet.document

import io.mockk.mockk
import org.multipaz.securearea.CreateKeySettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class CreateDocumentSettingsTest {

    private val secureAreaIdentifier = "secureAreaIdentifier"
    private val createKeySettings = mockk<CreateKeySettings>()

    @Test
    fun `test operator invoke returns an instance of CreateDocumentSettingsImpl`() {
        val createDocumentSettings = CreateDocumentSettings(
            secureAreaIdentifier = secureAreaIdentifier,
            createKeySettings = createKeySettings,
        )
        assertIs<CreateDocumentSettingsImpl>(createDocumentSettings)
    }

    @Test
    fun `test all parameters are properly set`() {
        val credentialPolicy = CreateDocumentSettings.CredentialPolicy.OnceOnly(numberOfCredentials = 3)
        val createDocumentSettings = CreateDocumentSettings(
            secureAreaIdentifier = secureAreaIdentifier,
            createKeySettings = createKeySettings,
            credentialPolicy = credentialPolicy
        )

        assertEquals(secureAreaIdentifier, createDocumentSettings.secureAreaIdentifier)
        assertSame(createKeySettings, createDocumentSettings.createKeySettings)
        assertEquals(3, createDocumentSettings.credentialPolicy.numberOfCredentials)
        assertSame(credentialPolicy, createDocumentSettings.credentialPolicy)
    }

    @Test
    fun `test default values are applied when not specified`() {
        val createDocumentSettings = CreateDocumentSettings(
            secureAreaIdentifier = secureAreaIdentifier,
            createKeySettings = createKeySettings
        )

        assertEquals(
            1,
            createDocumentSettings.credentialPolicy.numberOfCredentials,
            "Default numberOfCredentials should be 1"
        )
        assertIs<CreateDocumentSettings.CredentialPolicy.RotatingBatch>(
            createDocumentSettings.credentialPolicy,
            "Default credentialPolicy should be RotatingBatch"
        )
    }

    @Test
    fun `test CredentialPolicy numberOfCredentials is accessible on each variant`() {
        assertEquals(1, CreateDocumentSettings.CredentialPolicy.OnceOnly().numberOfCredentials)
        assertEquals(5, CreateDocumentSettings.CredentialPolicy.OnceOnly(numberOfCredentials = 5).numberOfCredentials)
        assertEquals(1, CreateDocumentSettings.CredentialPolicy.RotatingBatch().numberOfCredentials)
        assertEquals(3, CreateDocumentSettings.CredentialPolicy.RotatingBatch(numberOfCredentials = 3).numberOfCredentials)
        assertEquals(1, CreateDocumentSettings.CredentialPolicy.LimitedTime().numberOfCredentials)
        assertEquals(1, CreateDocumentSettings.CredentialPolicy.LimitedTime(
            reissueTriggerLifetimeLeft = kotlin.time.Duration.parse("90d")
        ).numberOfCredentials)
    }

    @Test
    fun `test throws IllegalArgumentException when numberOfCredentials is zero`() {
        assertFailsWith<IllegalArgumentException> {
            CreateDocumentSettings.CredentialPolicy.OnceOnly(numberOfCredentials = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            CreateDocumentSettings.CredentialPolicy.RotatingBatch(numberOfCredentials = 0)
        }
    }

    @Test
    fun `test throws IllegalArgumentException when numberOfCredentials is negative`() {
        assertFailsWith<IllegalArgumentException> {
            CreateDocumentSettings.CredentialPolicy.OnceOnly(numberOfCredentials = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            CreateDocumentSettings.CredentialPolicy.RotatingBatch(numberOfCredentials = -1)
        }
    }
}

