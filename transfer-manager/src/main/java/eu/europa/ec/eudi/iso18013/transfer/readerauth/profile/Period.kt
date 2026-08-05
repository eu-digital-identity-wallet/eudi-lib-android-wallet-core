/*
 * Copyright (c) 2023-2026 European Commission
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
package eu.europa.ec.eudi.iso18013.transfer.readerauth.profile

import android.util.Log
import eu.europa.ec.eudi.iso18013.transfer.internal.TAG
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.TimeUnit

class Period : ProfileValidation {

    companion object {
        internal const val MAX_VALIDITY_PERIOD_DAYS = 1187
    }

    override fun validate(
        chain: List<X509Certificate>,
        trustCA: X509Certificate,
    ): Boolean {
        require(chain.isNotEmpty())
        val readerAuthCertificate = chain.first()
        val expireDate = readerAuthCertificate.notAfter
        val fromDate = readerAuthCertificate.notBefore
        val diff = expireDate.time - fromDate.time

        val durationWithinLimit = TimeUnit.DAYS.convert(
            diff,
            TimeUnit.MILLISECONDS,
        ) <= MAX_VALIDITY_PERIOD_DAYS

        val now = Date()
        val currentlyWithinValidity = !now.before(fromDate) && !now.after(expireDate)

        return (durationWithinLimit && currentlyWithinValidity).also {
            Log.d(this.TAG, "ValidityPeriod: $it")
        }
    }
}
