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

package eu.europa.ec.eudi.wallet.internal

import android.content.Context
import android.os.Build
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.Executor

@JvmSynthetic
internal fun Context.getCertificate(@RawRes resId: Int): X509Certificate =
    resources.openRawResource(resId).use {
        CertificateFactory.getInstance("X509")
            .generateCertificate(it) as X509Certificate

    }

@JvmSynthetic
internal fun Context.mainExecutor(): Executor {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        mainExecutor
    } else {
        ContextCompat.getMainExecutor(applicationContext)
    }
}