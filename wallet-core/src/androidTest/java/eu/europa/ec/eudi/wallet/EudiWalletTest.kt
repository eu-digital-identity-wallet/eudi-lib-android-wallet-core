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

package eu.europa.ec.eudi.wallet

import android.content.Context
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import kotlinx.io.bytestring.ByteString
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.multipaz.securearea.AndroidKeystoreCreateKeySettings
import org.multipaz.securearea.AndroidKeystoreSecureArea
import java.io.File

@RunWith(AndroidJUnit4::class)
class EudiWalletTest {

    companion object {

        private lateinit var context: Context

        @BeforeClass
        @JvmStatic
        fun setup() {
            context = InstrumentationRegistry.getInstrumentation().targetContext
        }
    }

    private lateinit var walletConfig: EudiWalletConfig

    private val wallet: EudiWallet
        get() = EudiWallet(context, walletConfig)

    private val sampleData: ByteArray
        get() = context.resources.openRawResource(eu.europa.ec.eudi.wallet.test.R.raw.sample_data)
            .use { Base64.decode(it.readBytes(), Base64.DEFAULT) }

    @Test
    @Ignore("Fails in CI; Needs to be fixed")
    fun testLoadSampleDocuments() {
        walletConfig = EudiWalletConfig()
            .configureDocumentManager(
                storagePath = File(context.noBackupFilesDir, "test-storage.db").absolutePath,
                identifier = "test-identifier",
            )
            .configureDocumentKeyCreation(
                userAuthenticationRequired = true,
                userAuthenticationTimeout = 30_000L,
                useStrongBoxForKeys = true,
            )

        val result = wallet.loadMdocSampleDocuments(
            sampleData = sampleData,
            createSettings = CreateDocumentSettings(
                secureAreaIdentifier = AndroidKeystoreSecureArea.IDENTIFIER,
                numberOfCredentials = 1,
                credentialPolicy = CreateDocumentSettings.CredentialPolicy.RotateUse,
                createKeySettings = AndroidKeystoreCreateKeySettings.Builder(
                    attestationChallenge = ByteString("attestationChallenge".toByteArray())
                ).build()

            )
        )

        Assert.assertTrue(result.isSuccess)

        val documents = wallet.getDocuments().filterIsInstance<IssuedDocument>()
        Assert.assertEquals(2, documents.size)
    }
}