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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.CborArray
import org.multipaz.cbor.CborMap
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.Tstr
import org.multipaz.document.NameSpacedData

/**
 * The paths of the claims that the issuer marks as mandatory.
 */
internal val IssuerMetadata?.mandatoryClaimPaths: List<List<String>>
    get() = this?.claims.orEmpty().filter { it.mandatory == true }.map { it.path }

/**
 * Returns the mandatory mso_mdoc claims that are not in this data.
 *
 * A path starts with the namespace and the data element identifier. Further components select
 * values inside the data element (OpenID4VCI 1.0, Appendix C.2). A claim is present when its path
 * selects at least one value. Paths with fewer than two components are not mso_mdoc claim paths
 * and are ignored.
 */
internal fun NameSpacedData.missingMandatoryClaims(paths: List<List<String>>): List<List<String>> =
    paths.filter { it.size >= 2 }.filterNot { path ->
        val (nameSpace, element) = path
        hasDataElement(nameSpace, element) &&
            Cbor.decode(getDataElement(nameSpace, element)).selects(path.drop(2))
    }

/**
 * Whether [components] select at least one value in this data item. A string component selects
 * the value of that key in a map. [IssuerMetadata.Claim.path] keeps only the string components of
 * a claim path, so an array selects all of its elements, as a `null` component does.
 */
private fun DataItem.selects(components: List<String>): Boolean = when {
    components.isEmpty() -> true
    this is Tagged -> taggedItem.selects(components)
    this is CborArray -> items.any { it.selects(components) }
    this is CborMap -> items[Tstr(components.first())]?.selects(components.drop(1)) ?: false
    else -> false
}

/**
 * Returns the mandatory SD-JWT VC claims that are not in these recreated claims.
 *
 * A path selects values from the top-level claim down (OpenID4VCI 1.0, Appendix C.1). A claim is
 * present when its path selects at least one value. Empty paths are ignored.
 */
internal fun JsonObject.missingMandatoryClaims(paths: List<List<String>>): List<List<String>> =
    paths.filter { it.isNotEmpty() }.filterNot { selects(it) }

/**
 * Whether [components] select at least one value in this element. A string component selects the
 * value of that key in an object. [IssuerMetadata.Claim.path] keeps only the string components of
 * a claim path, so an array selects all of its elements, as a `null` component does.
 */
private fun JsonElement.selects(components: List<String>): Boolean = when {
    components.isEmpty() -> true
    this is JsonArray -> any { it.selects(components) }
    this is JsonObject -> get(components.first())?.selects(components.drop(1)) ?: false
    else -> false
}
