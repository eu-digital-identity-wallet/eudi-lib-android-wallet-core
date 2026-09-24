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

import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Tagged
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseSign1
import org.multipaz.cose.toCoseLabel
import org.multipaz.crypto.X509CertChain

/**
 * Decodes a CWT byte array into a [CoseSign1], unwrapping CBOR tag 18 if present.
 *
 * Conformant CWTs (per RFC 8392 / draft-ietf-oauth-status-list-10 §5.2) wrap the
 * COSE_Sign1 array in CBOR tag 18 (`d2`). The multipaz [CoseSign1.fromDataItem]
 * requires a bare [CborArray], so any enclosing tags must be stripped first.
 */
internal fun decodeCoseSign1(cwtBytes: ByteArray): CoseSign1 {
    var item = Cbor.decode(cwtBytes)
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
