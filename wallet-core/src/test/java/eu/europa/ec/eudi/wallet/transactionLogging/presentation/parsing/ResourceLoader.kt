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

import java.io.File
import java.util.Base64

fun getResourceAsByteArray(resource: String): ByteArray =
    File(ClassLoader.getSystemResource(resource).path).readBytes()

fun getResourceAsByteArrayFromBase64Url(resource: String): ByteArray {
    val base64String = File(ClassLoader.getSystemResource(resource).path).readText()
    return Base64.getUrlDecoder().decode(base64String)
}

@OptIn(ExperimentalStdlibApi::class)
fun getResourceAsByteArrayFromHex(resource: String): ByteArray =
    File(ClassLoader.getSystemResource(resource).path).readText().hexToByteArray()