/*
 * Copyright (c) 2023-2024 European Commission
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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.CharacterSetECI
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

/**
 * Qr code class used to create a QR code
 *
 * @constructor Create empty Qr code
 */
internal object QrCodeUtils {

    /**
     * Get the generated QR code as a [View]
     *
     * @param context context instance
     * @param str the content of the QR code as a [String]
     * @param size the required size of the QR code view in Pixels (optional). By default, the size is 800.
     * @return the QR code as a [View]
     */
    fun getQRCodeAsView(context: Context, str: String, size: Int = 800): View {
        return ImageView(context).apply {
            setImageBitmap(getQRCodeAsBitmap(str, size))
        }
    }

    /**
     * Creates a QR code as [Bitmap]
     * @param content is the required content of the QR code as string
     * @param size is the required size of the QR code as pixels
     * @return returns a [Bitmap] of the QR code
     */
    fun getQRCodeAsBitmap(content: String, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = CharacterSetECI.UTF8
            val bitMapMatrix =
                QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            for (i in 0 until size) {
                for (j in 0 until size) {
                    bitmap.setPixel(
                        i,
                        j,
                        if (bitMapMatrix[i, j]) Color.BLACK else Color.WHITE,
                    )
                }
            }
        } catch (e: WriterException) {
            Log.e(this.TAG, "Error creating QR code", e)
        }
        return bitmap
    }
}
