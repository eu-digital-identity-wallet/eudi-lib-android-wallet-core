/*
 * Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.iso18013.transfer

import android.content.Context
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.iso18013.transfer.sample.SampleDocumentManagerImpl
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.DocumentManagerImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.multipaz.securearea.PassphraseConstraints
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.securearea.software.SoftwareCreateKeySettings
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.ephemeral.EphemeralStorage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.ExperimentalTime

val Context: Context = mockk {
    every { applicationContext } returns this
    every { mainLooper } returns mockk(relaxed = true)
}
const val KeyLockPassphrase = "1234"

@OptIn(ExperimentalEncodingApi::class, ExperimentalTime::class)
fun createDocumentManager(keyLockPassphrase: String?): DocumentManager {
    val storage = EphemeralStorage()
    val secureArea = runBlocking { SoftwareSecureArea.create(storage) }
    val secureAreaRep = SecureAreaRepository.Builder()
        .add(secureArea)
        .build()
    return SampleDocumentManagerImpl(
        DocumentManagerImpl(
            identifier = "DocumentManager",
            storage = storage,
            secureAreaRepository = secureAreaRep,
            ktorHttpClientFactory = null,
        )
    ).apply {
        loadMdocSampleDocuments(
            sampleData = Base64.decode(getResourceAsText("sample_documents.txt")),
            createSettings = CreateDocumentSettings(
                secureAreaIdentifier = secureArea.identifier,
                createKeySettings = keyLockPassphrase?.let { p ->
                    SoftwareCreateKeySettings.Builder()
                        .setPassphraseRequired(
                            true,
                            p,
                            PassphraseConstraints.PIN_FOUR_DIGITS
                        )
                        .build()
                } ?: SoftwareCreateKeySettings.Builder().build(),
            ),
            documentNamesMap = mapOf(
                "eu.europa.ec.eudi.pid.1" to "EU PID",
                "org.iso.18013.5.1.mDL" to "mDL"
            )
        ).getOrThrow()
    }
}

/**
 * namespace = "org.iso.18013.5.1", elementIdentifier = "given_name", intentToRetail = true
 * namespace = "org.iso.18013.5.1", elementIdentifier = "birth_date", intentToRetail = true
 * namespace = "org.iso.18013.5.1", elementIdentifier = "issue_date", intentToRetail = true
 * namespace = "org.iso.18013.5.1", elementIdentifier = "portrait", intentToRetail = false
 * namespace = "org.iso.18013.5.1.Utopia", elementIdentifier = "UtopiaID", intentToRetail = true
 */
@OptIn(ExperimentalStdlibApi::class)
val DeviceRequest: DeviceRequest = DeviceRequest(
    sessionTranscriptBytes = byteArrayOf(0),
    deviceRequestBytes = getResourceAsText("device_request.hex").hexToByteArray()
)

