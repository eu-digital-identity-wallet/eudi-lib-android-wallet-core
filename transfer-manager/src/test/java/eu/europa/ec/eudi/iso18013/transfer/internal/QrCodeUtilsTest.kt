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

package eu.europa.ec.eudi.iso18013.transfer.internal

import android.util.Log
import android.widget.ImageView
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import eu.europa.ec.eudi.iso18013.transfer.mockAndroidLog
import io.mockk.every
import io.mockk.mockkConstructor
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QrCodeUtilsTest {


    lateinit var mockLog: MockedStatic<Log>

    @BeforeTest
    fun setUp() {
        mockLog = mockAndroidLog()
    }

    @AfterTest
    fun tearDown() {
        mockLog.close()
    }

    @Test
    fun getQRCodeAsViewShouldReturnImageViewWithBitmap() {
        val content = "Test Content"
        val context = RuntimeEnvironment.getApplication()
        val view = QrCodeUtils.getQRCodeAsView(context, content)

        assertTrue(view is ImageView)
        assertNotNull(view.drawable)
    }

    @Test
    fun getQRCodeAsBitmapShouldReturnBitmapWithCorrectSize() {
        val content = "Test Content"
        val size = 800
        val bitmap = QrCodeUtils.getQRCodeAsBitmap(content, size)

        assertNotNull(bitmap)
        assertEquals(size, bitmap.width)
        assertEquals(size, bitmap.height)
    }

    @Test
    fun getQRCodeAsBitmapShouldHandleWriterException() {
        mockkConstructor(QRCodeWriter::class)
        every {
            QRCodeWriter().encode(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws WriterException("Test Exception")

        val content = "Test Content"
        val size = 800
        val bitmap = QrCodeUtils.getQRCodeAsBitmap(content, size)

        assertNotNull(bitmap)
        assertEquals(size, bitmap.width)
        assertEquals(size, bitmap.height)
    }

}