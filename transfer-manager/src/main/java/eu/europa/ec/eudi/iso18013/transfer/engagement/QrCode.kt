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
package eu.europa.ec.eudi.iso18013.transfer.engagement

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import eu.europa.ec.eudi.iso18013.transfer.internal.QrCodeUtils

/**
 * Wrapper for a QR code.
 * @property content the content of the QR code
 * @constructor Creates a QR code with the given [content]
 */
data class QrCode(val content: String) {
    /**
     * Returns the QR code as a [View] with the given [size].
     */
    fun asView(context: Context, size: Int): View =
        QrCodeUtils.getQRCodeAsView(context, content, size)

    /**
     * Returns the QR code as a [Bitmap] with the given [size].
     */
    fun asBitmap(size: Int): Bitmap =
        QrCodeUtils.getQRCodeAsBitmap(content, size)
}
