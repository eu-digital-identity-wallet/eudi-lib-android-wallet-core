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
package eu.europa.ec.eudi.wallet.registration

/**
 * An element of a claim path, following the OpenID4VP claims path pointer syntax (clause 7). A path is
 * a list of these elements and selects a claim, or a set of claims, within an attestation.
 *
 * - [Claim] selects the member with the given name.
 * - [ArrayElement] selects the given index of an array.
 * - [AllArrayElements] selects every element of an array.
 *
 * In JSON a path is an array whose elements are a string, a non-negative integer, or `null`
 * respectively. A path into an mdoc is always two [Claim] elements, the namespace and the data element
 * identifier (clause 7.2).
 */
sealed interface ClaimPathElement {

    /** Selects the member named [name]. */
    data class Claim(val name: String) : ClaimPathElement {
        override fun toString(): String = name
    }

    /** Selects the element at [index] of an array. */
    data class ArrayElement(val index: Int) : ClaimPathElement {
        init {
            require(index >= 0) { "index must be non-negative" }
        }

        override fun toString(): String = index.toString()
    }

    /** Selects every element of an array. */
    data object AllArrayElements : ClaimPathElement {
        override fun toString(): String = "null"
    }

    /**
     * Whether this element selects everything [that] selects. An element selects what an equal element
     * selects, and [AllArrayElements] also selects what any [ArrayElement] selects. It does not select
     * what a [Claim] selects: the wildcard applies to arrays only, and OpenID4VP clause 7.1.1 makes it
     * an error against anything else.
     */
    fun covers(that: ClaimPathElement): Boolean = when (this) {
        AllArrayElements -> when (that) {
            AllArrayElements, is ArrayElement -> true
            is Claim -> false
        }

        is ArrayElement, is Claim -> this == that
    }
}

/**
 * Whether this path selects everything [that] selects: it is not empty, it is no longer than [that],
 * and each of its elements selects the element at the same position. A shorter path therefore selects
 * the whole subtree below it, while an empty path selects nothing.
 */
internal fun List<ClaimPathElement>.covers(that: List<ClaimPathElement>): Boolean =
    isNotEmpty() && size <= that.size && withIndex().all { (index, element) -> element.covers(that[index]) }
