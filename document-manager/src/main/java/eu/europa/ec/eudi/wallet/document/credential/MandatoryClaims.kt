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

import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import kotlinx.serialization.json.JsonObject
import org.multipaz.document.NameSpacedData

/**
 * The paths of the claims that the issuer marks as mandatory.
 */
internal val IssuerMetadata?.mandatoryClaimPaths: List<List<String>>
    get() = this?.claims.orEmpty().filter { it.mandatory == true }.map { it.path }

/**
 * Returns the mandatory mso_mdoc claims, given as namespace and element identifier, that are not
 * in this data. Paths of another length are ignored.
 */
internal fun NameSpacedData.missingMandatoryClaims(paths: List<List<String>>): List<List<String>> =
    paths.filter { it.size == 2 }.filterNot { (nameSpace, element) ->
        nameSpace in nameSpaceNames && element in getDataElementNames(nameSpace)
    }

/**
 * Returns the mandatory SD-JWT VC claims, given as a top-level claim name, that are not in these
 * recreated claims. Paths of another length are ignored.
 */
internal fun JsonObject.missingMandatoryClaims(paths: List<List<String>>): List<List<String>> =
    paths.filter { it.size == 1 }.filterNot { it.single() in this }
