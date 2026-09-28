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
package eu.europa.ec.eudi.wallet.statium

import eu.europa.ec.eudi.wallet.internal.d
import eu.europa.ec.eudi.wallet.logging.Logger
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tagged
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseSign1
import org.multipaz.cose.toCoseLabel
import org.multipaz.crypto.X509CertChain

private const val TAG = "CwtParsing"

/**
 * Decodes a CWT byte array into a [CoseSign1], unwrapping any CBOR tags (tag 18 for
 * COSE_Sign1, tag 61 for CWT per RFC 8392, or both nested).
 *
 * Uses the offset-based [Cbor.decode] overload to tolerate trailing bytes, which have
 * been observed intermittently from reference endpoints. Trailing bytes are logged but
 * do not cause a failure.
 *
 * The multipaz [CoseSign1.fromDataItem] requires a bare `CborArray`, so any enclosing
 * tags must be stripped first.
 */
internal fun decodeCoseSign1(cwtBytes: ByteArray, logger: Logger? = null): CoseSign1 {
    val (consumed, decoded) = Cbor.decode(cwtBytes, 0)
    if (consumed != cwtBytes.size) {
        logger?.d(TAG, "Ignoring ${cwtBytes.size - consumed} trailing byte(s) after CWT")
    }
    var item: DataItem = decoded
    while (item is Tagged) item = item.taggedItem
    return item.asCoseSign1
}

/**
 * Extracts the x5chain (COSE label 33) from this [CoseSign1], checking the protected
 * headers first, then the unprotected headers.
 *
 * RFC 9360 §2 allows x5chain in either bucket and recommends the protected bucket for
 * integrity protection. Both observed reference issuers place x5chain in the protected
 * headers.
 */
internal fun CoseSign1.extractX5chain(): X509CertChain {
    val label = Cose.COSE_LABEL_X5CHAIN.toCoseLabel
    val x5chainDataItem = protectedHeaders[label]
        ?: unprotectedHeaders[label]
        ?: throw IllegalStateException("Missing x5chain in COSE headers")
    return x5chainDataItem.asX509CertChain
}
