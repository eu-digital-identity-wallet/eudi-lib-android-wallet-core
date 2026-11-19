/*
 *  Copyright (c) 2026 European Commission
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package eu.europa.ec.eudi.iso18013.transfer.readerauth

import java.net.URI
import java.security.cert.X509CRL
import java.time.Instant

/**
 * Keeps the CRLs of a [CrlCache] across restarts.
 *
 * Which CRLs are kept, and when they were downloaded, tells which relying parties and issuers the
 * wallet dealt with and when, so an implementation should not keep them readable.
 */
interface CrlStore {

    /** A CRL and when it was downloaded. */
    class StoredCrl(val crl: X509CRL, val fetchedAt: Instant)

    /** The CRL kept for [uri], or `null` if there is none. */
    fun load(uri: URI): StoredCrl?

    /** Keeps [stored] for [uri], replacing the CRL kept before. */
    fun save(uri: URI, stored: StoredCrl)
}
