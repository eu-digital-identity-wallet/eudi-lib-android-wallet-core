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

package eu.europa.ec.eudi.wallet.issue.openid4vci.reissue

import eu.europa.ec.eudi.wallet.document.format.DocumentFormat

/**
 * Thrown when the credential configuration of a document to re-issue now has a different format
 * than the document, for example another docType or vct. The re-issuance stops before the
 * credential is requested, and the document is kept. A deferred re-issuance started before this
 * check existed is checked when its credential is issued; that credential is then not stored.
 *
 * A credential of the new format can be obtained with a new issuance.
 *
 * @property documentFormat the format of the document to re-issue
 * @property configurationFormat the format of the credential configuration, or null when the
 *   wallet does not support it
 */
class ReissuanceFormatMismatchException(
    val documentFormat: DocumentFormat,
    val configurationFormat: DocumentFormat?,
) : Exception(
    "Re-issuance would change the document format from $documentFormat to " +
        (configurationFormat?.toString() ?: "an unsupported format")
)
