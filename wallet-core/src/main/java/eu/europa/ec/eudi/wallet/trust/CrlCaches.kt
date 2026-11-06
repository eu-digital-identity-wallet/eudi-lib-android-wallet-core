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

package eu.europa.ec.eudi.wallet.trust

import android.content.Context
import eu.europa.ec.eudi.iso18013.transfer.readerauth.CrlCache
import eu.europa.ec.eudi.wallet.internal.ktorCrlDownload
import io.ktor.client.HttpClient
import java.io.File
import kotlin.time.Duration

private const val CRL_CACHE_DIRECTORY = "eudi-crl"

/**
 * A [CrlCache] kept in the app's cache directory, so that the CRLs survive a restart. The CRLs are
 * encrypted with a key in the Android Keystore, see [EncryptedCrlStore].
 *
 * Create one and hand it to [eu.europa.ec.eudi.wallet.EudiWalletConfig.configureCrlCache].
 *
 * @param context the context whose cache directory keeps the CRLs
 * @param refreshInterval how long a CRL is used before it is downloaded again, must be positive
 * @param httpClientFactory the Ktor client that downloads the CRLs, or `null` for [java.net.HttpURLConnection]
 */
fun CrlCache.Companion.inCacheDirectory(
    context: Context,
    refreshInterval: Duration = CrlCache.DEFAULT_REFRESH_INTERVAL,
    httpClientFactory: (() -> HttpClient)? = null,
): CrlCache {
    val store = EncryptedCrlStore.inAndroidKeyStore(File(context.cacheDir, CRL_CACHE_DIRECTORY))
    return if (httpClientFactory == null) {
        CrlCache(store, refreshInterval)
    } else {
        CrlCache(store, refreshInterval, fetch = ktorCrlDownload(httpClientFactory))
    }
}
