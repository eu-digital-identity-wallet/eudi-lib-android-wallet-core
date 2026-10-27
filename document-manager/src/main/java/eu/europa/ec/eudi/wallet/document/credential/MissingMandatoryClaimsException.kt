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

package eu.europa.ec.eudi.wallet.document.credential

/**
 * Thrown when an issued credential does not contain claims that the issuer marks as mandatory
 * in the credential configuration (OpenID4VCI 1.0, Appendix B.2). The credential is not stored.
 *
 * @property claims the paths of the missing mandatory claims
 */
class MissingMandatoryClaimsException(
    val claims: List<List<String>>,
) : IllegalArgumentException(
    "Issued credential is missing mandatory claims: ${claims.joinToString { it.joinToString(".") }}"
)
