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

package eu.europa.ec.eudi.wallet.internal

import eu.europa.ec.eudi.iso18013.transfer.readerauth.CrlCache
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.URI
import java.security.cert.CertificateFactory
import java.security.cert.X509CRL
import kotlin.time.Duration

/**
 * Downloads a CRL for a [CrlCache] with the client from [httpClientFactory], within [timeout].
 */
@JvmSynthetic
internal fun ktorCrlDownload(
    httpClientFactory: () -> HttpClient,
    timeout: Duration = CrlCache.DEFAULT_DOWNLOAD_TIMEOUT,
): (URI) -> X509CRL = { uri ->
    runBlocking {
        withTimeout(timeout) {
            val response = httpClientFactory().get(uri.toString())
            if (response.status != HttpStatusCode.OK) {
                throw IOException("HTTP ${response.status.value} from $uri")
            }
            CertificateFactory.getInstance("X.509").generateCRL(response.bodyAsBytes().inputStream()) as X509CRL
        }
    }
}
