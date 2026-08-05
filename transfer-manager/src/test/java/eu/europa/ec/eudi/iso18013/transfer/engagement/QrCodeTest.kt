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

package eu.europa.ec.eudi.iso18013.transfer.engagement

import eu.europa.ec.eudi.iso18013.transfer.internal.QrCodeUtils
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QrCodeTest {

    @BeforeTest
    fun setUp() {
        mockkObject(QrCodeUtils)
    }

    @AfterTest
    fun tearDown() {
        // clear all mocks
        unmockkAll()
    }

    @Test
    fun `asView method should call QrCodeUtils getQRCodeAsView`() {
        val context = RuntimeEnvironment.getApplication()
        val qrCodeContent = "test content"
        QrCode(qrCodeContent).asView(context, 800)
        verify(exactly = 1) { QrCodeUtils.getQRCodeAsView(context, qrCodeContent, 800) }
    }

    @Test
    fun `asBitmap method should call QrCodeUtils getQRCodeAsBitmap`() {
        val qrCodeContent = "test content"
        QrCode(qrCodeContent).asBitmap(800)
        verify(exactly = 1) { QrCodeUtils.getQRCodeAsBitmap(qrCodeContent, 800) }
    }
}