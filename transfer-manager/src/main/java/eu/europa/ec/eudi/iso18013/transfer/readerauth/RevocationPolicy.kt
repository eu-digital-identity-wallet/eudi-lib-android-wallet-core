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

/**
 * Policy that controls how certificate revocation is checked during
 * reader authentication trust path validation.
 */
sealed interface RevocationPolicy {
    /** No revocation checking is performed. */
    data object NoCheck : RevocationPolicy

    /** Validation fails if a certificate is revoked OR if the CRL cannot be retrieved (default). */
    data object HardFail : RevocationPolicy

    /** Validation fails if a certificate is revoked, but tolerates CRL unavailability. */
    data object SoftFail : RevocationPolicy
}
