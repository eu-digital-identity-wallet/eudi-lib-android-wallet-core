/*
 * Copyright (c) 2025-2026 European Commission
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

package eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql

import eu.europa.ec.eudi.openid4vp.Format
import eu.europa.ec.eudi.openid4vp.dcql.ClaimId
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPath
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.openid4vp.dcql.ClaimSet
import eu.europa.ec.eudi.openid4vp.dcql.ClaimsQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.DCQLMetaMsoMdocExtensions
import eu.europa.ec.eudi.openid4vp.dcql.DCQLMetaSdJwtVcExtensions
import eu.europa.ec.eudi.openid4vp.dcql.MsoMdocDocType
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import org.junit.Test
import org.multipaz.request.JsonRequestedClaim
import org.multipaz.request.MdocRequestedClaim
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the pure-function adapters in [DcqlQueryAdapter] that bridge DCQL
 * query models to the matching pipeline's request models.
 *
 *  - [ClaimPath.toJsonArray] is exercised for every [ClaimPathElement] kind, including
 *    paths that mix kinds in a single sequence.
 *  - [CredentialQuery.toDcqlCredentialQuery] is exercised across both supported
 *    formats (mso_mdoc, dc+sd-jwt), exposing how `meta` / `claims` / `claim_sets` map
 *    onto the [DcqlCredentialQuery] shape — including error paths when required
 *    metadata is missing or malformed.
 */
class DcqlQueryAdapterTest {

    /**
     * [ClaimPathElement.Claim] serializes as a string [JsonPrimitive] — this is the
     * common case for object property navigation (`address.country`, `given_name`, …).
     */
    @Test
    fun `toJsonArray maps Claim element to JsonPrimitive string`() {
        val path = ClaimPath(listOf(ClaimPathElement.Claim("given_name")))

        val json = path.toJsonArray()

        assertEquals(1, json.size)
        val primitive = assertIs<JsonPrimitive>(json[0])
        assertTrue(primitive.isString, "Claim must serialize as a string-typed primitive")
        assertEquals("given_name", primitive.content)
    }

    /**
     * [ClaimPathElement.ArrayElement] serializes as an integer [JsonPrimitive] — used to
     * address a specific array index in a claim path, e.g. `nationalities[0]`.
     */
    @Test
    fun `toJsonArray maps ArrayElement to JsonPrimitive integer`() {
        val path = ClaimPath(listOf(ClaimPathElement.ArrayElement(2)))

        val json = path.toJsonArray()

        assertEquals(1, json.size)
        val primitive = assertIs<JsonPrimitive>(json[0])
        assertEquals(false, primitive.isString, "ArrayElement must NOT serialize as a string")
        assertEquals(2, primitive.intOrNull)
    }

    /**
     * [ClaimPathElement.AllArrayElements] serializes as [JsonNull] — the wildcard "fan
     * over every element of an array" semantic from OpenID4VP §6.4.
     */
    @Test
    fun `toJsonArray maps AllArrayElements to JsonNull`() {
        val path = ClaimPath(listOf(ClaimPathElement.AllArrayElements))

        val json = path.toJsonArray()

        assertEquals(1, json.size)
        assertEquals(JsonNull, json[0])
    }

    /**
     * A path mixing all three kinds must round-trip without losing the kind at each
     * position. This is the key property for nested array navigation
     * (`address.streetLines[2]`, `nationalities[*]`).
     */
    @Test
    fun `toJsonArray preserves element kinds and order in a mixed path`() {
        val path = ClaimPath(
            listOf(
                ClaimPathElement.Claim("address"),
                ClaimPathElement.Claim("street_lines"),
                ClaimPathElement.ArrayElement(2),
                ClaimPathElement.AllArrayElements,
                ClaimPathElement.Claim("city"),
            ),
        )

        val json = path.toJsonArray()

        assertEquals(5, json.size)
        assertEquals(JsonPrimitive("address"), json[0])
        assertEquals(JsonPrimitive("street_lines"), json[1])
        assertEquals(JsonPrimitive(2), json[2])
        assertEquals(JsonNull, json[3])
        assertEquals(JsonPrimitive("city"), json[4])
    }

    /**
     * `mso_mdoc` happy path: doctype + a two-element claim path (namespace + element) is
     * converted into an [MdocRequestedClaim] with the doctype propagated, the namespace/
     * dataElementName broken out from the path, and `intentToRetain` defaulted to `false`
     * when the verifier didn't set it.
     */
    @Test
    fun `toDcqlCredentialQuery for mso_mdoc maps claim path to MdocRequestedClaim`() {
        val docType = "eu.europa.ec.eudi.pid.1"
        val namespace = "eu.europa.ec.eudi.pid.1"
        val dataElement = "given_name"
        val query = CredentialQuery.mdoc(
            id = QueryId("q1"),
            msoMdocMeta = DCQLMetaMsoMdocExtensions(doctypeValue = MsoMdocDocType(docType)),
            claims = listOf(
                ClaimsQuery.mdoc(
                    id = ClaimId("c1"),
                    namespace = namespace,
                    claimName = dataElement,
                    intentToRetain = null,
                ),
            ),
        )

        val dcqlQuery =query.toDcqlCredentialQuery()

        assertEquals("q1", dcqlQuery.id)
        assertEquals(Format.MsoMdoc.value, dcqlQuery.format)
        assertEquals(docType, dcqlQuery.mdocDocType)
        assertNull(dcqlQuery.vctValues, "mso_mdoc must not surface vctValues")
        assertEquals(1, dcqlQuery.claims.size)
        val mdocClaim = assertIs<MdocRequestedClaim>(dcqlQuery.claims.single())
        assertEquals("c1", mdocClaim.id)
        assertEquals(docType, mdocClaim.docType)
        assertEquals(namespace, mdocClaim.namespaceName)
        assertEquals(dataElement, mdocClaim.dataElementName)
        assertEquals(false, mdocClaim.intentToRetain, "Null intentToRetain must default to false")
    }

    /**
     * `intent_to_retain = true` set by the verifier must propagate verbatim — the wallet
     * relies on this flag for downstream consent UX ("the verifier will store this").
     */
    @Test
    fun `toDcqlCredentialQuery propagates explicit intentToRetain true`() {
        val query = CredentialQuery.mdoc(
            id = QueryId("q1"),
            msoMdocMeta = DCQLMetaMsoMdocExtensions(doctypeValue = MsoMdocDocType("doc.type")),
            claims = listOf(
                ClaimsQuery.mdoc(
                    namespace = "ns",
                    claimName = "el",
                    intentToRetain = true,
                ),
            ),
        )

        val mdocClaim = assertIs<MdocRequestedClaim>(query.toDcqlCredentialQuery().claims.single())

        assertEquals(true, mdocClaim.intentToRetain)
    }

    /**
     * mso_mdoc meta is required to expose a non-null `doctype_value`. When it's missing
     * (i.e. the verifier built a malformed query), the adapter must surface this as an
     * [IllegalArgumentException] — matching the validation contract of
     * [DcqlRequestProcessor.candidateDocumentsForQuery].
     */
    @Test
    fun `toDcqlCredentialQuery fails when mso_mdoc meta lacks doctype_value`() {
        val query = CredentialQuery(
            id = QueryId("q1"),
            format = Format.MsoMdoc,
            // Empty JsonObject — no doctype_value reachable via metaMsoMdoc.
            meta = JsonObject(emptyMap()),
            claims = null,
        )

        assertFailsWith<IllegalArgumentException> { query.toDcqlCredentialQuery() }
    }

    /**
     * `dc+sd-jwt` happy path: vct_values + a multi-element claim path is converted into a
     * [JsonRequestedClaim] with the `claimPath` JsonArray preserving the path kinds
     * (and `mdocDocType` left null).
     */
    @Test
    fun `toDcqlCredentialQuery for dc+sd-jwt maps claim path to JsonRequestedClaim`() {
        val vctValues = listOf("urn:eudi:pid:1", "urn:eudi:pid:2")
        val query = CredentialQuery.sdJwtVc(
            id = QueryId("q1"),
            sdJwtVcMeta = DCQLMetaSdJwtVcExtensions(vctValues = vctValues),
            claims = listOf(
                ClaimsQuery.sdJwtVc(
                    id = ClaimId("c1"),
                    path = ClaimPath(
                        listOf(
                            ClaimPathElement.Claim("address"),
                            ClaimPathElement.ArrayElement(0),
                            ClaimPathElement.Claim("city"),
                        ),
                    ),
                ),
            ),
        )

        val dcqlQuery =query.toDcqlCredentialQuery()

        assertEquals("q1", dcqlQuery.id)
        assertEquals(Format.SdJwtVc.value, dcqlQuery.format)
        assertNull(dcqlQuery.mdocDocType, "dc+sd-jwt must not surface mdocDocType")
        assertEquals(vctValues, dcqlQuery.vctValues)
        assertEquals(1, dcqlQuery.claims.size)
        val jsonClaim = assertIs<JsonRequestedClaim>(dcqlQuery.claims.single())
        assertEquals("c1", jsonClaim.id)
        assertEquals(vctValues, jsonClaim.vctValues)
        assertEquals(
            JsonArray(
                listOf(
                    JsonPrimitive("address"),
                    JsonPrimitive(0),
                    JsonPrimitive("city"),
                ),
            ),
            jsonClaim.claimPath,
        )
    }

    /**
     * `vct_values` is mandatory for `dc+sd-jwt`. Omitting it altogether (an opaque empty
     * meta object) must produce a clear failure rather than a silent default — the
     * wallet would otherwise have no way to filter candidate credentials.
     *
     * The exception type is intentionally relaxed to [Exception]: depending on where the
     * gap is detected, either the upstream deserializer ([kotlinx.serialization.MissingFieldException])
     * or our adapter's own `error("vct_values is missing")` can fire. What matters is that
     * the adapter never silently succeeds with an empty vct list.
     */
    @Test
    fun `toDcqlCredentialQuery fails when dc+sd-jwt meta lacks vct_values`() {
        val query = CredentialQuery(
            id = QueryId("q1"),
            format = Format.SdJwtVc,
            // Empty meta — vctValues unreachable.
            meta = JsonObject(emptyMap()),
            claims = null,
        )

        assertFailsWith<Exception> { query.toDcqlCredentialQuery() }
    }

    /**
     * `claims = null` is spec-legal (OpenID4VP §6.4.1 "mandatory disclosure only") and must
     * surface as an empty [DcqlCredentialQuery.claims] list — not a NullPointerException
     * and not a synthesized "disclose everything" claim list.
     */
    @Test
    fun `toDcqlCredentialQuery treats null claims as empty list, not NPE`() {
        val query = CredentialQuery.mdoc(
            id = QueryId("q1"),
            msoMdocMeta = DCQLMetaMsoMdocExtensions(doctypeValue = MsoMdocDocType("doc.type")),
            claims = null,
        )

        val dcqlQuery =query.toDcqlCredentialQuery()

        assertTrue(
            dcqlQuery.claims.isEmpty(),
            "null claims (mandatory disclosure only) must produce an empty claims list",
        )
    }

    /**
     * `claim_sets` (combinations of [ClaimId]s the verifier accepts as alternatives) are
     * carried verbatim into the [DcqlCredentialQuery.claimSets] field — the matcher
     * downstream needs them to enforce DCQL §6.4.2 set semantics.
     */
    @Test
    fun `toDcqlCredentialQuery preserves claim_sets`() {
        val query = CredentialQuery.mdoc(
            id = QueryId("q1"),
            msoMdocMeta = DCQLMetaMsoMdocExtensions(doctypeValue = MsoMdocDocType("doc.type")),
            claims = listOf(
                ClaimsQuery.mdoc(id = ClaimId("c1"), namespace = "ns", claimName = "el1"),
                ClaimsQuery.mdoc(id = ClaimId("c2"), namespace = "ns", claimName = "el2"),
            ),
            claimSets = listOf(
                ClaimSet(listOf(ClaimId("c1"))),
                ClaimSet(listOf(ClaimId("c1"), ClaimId("c2"))),
            ),
        )

        val dcqlQuery =query.toDcqlCredentialQuery()

        assertEquals(2, dcqlQuery.claimSets.size)
        assertEquals(listOf("c1"), dcqlQuery.claimSets[0].claimIdentifiers)
        assertEquals(listOf("c1", "c2"), dcqlQuery.claimSets[1].claimIdentifiers)
    }

    /**
     * `ClaimId` is optional per spec: claims that are not referenced from any
     * `claim_sets` combination don't need an id. The adapter must propagate `null` ids
     * verbatim onto the [MdocRequestedClaim.id] field (the field is nullable for
     * exactly this reason).
     */
    @Test
    fun `toDcqlCredentialQuery preserves null claim id`() {
        val query = CredentialQuery.mdoc(
            id = QueryId("q1"),
            msoMdocMeta = DCQLMetaMsoMdocExtensions(doctypeValue = MsoMdocDocType("doc.type")),
            claims = listOf(
                ClaimsQuery.mdoc(id = ClaimId("c1"), namespace = "ns", claimName = "el1"),
                ClaimsQuery.mdoc(id = null, namespace = "ns", claimName = "el2"),
            ),
        )

        val dcqlQuery =query.toDcqlCredentialQuery()

        val ids = dcqlQuery.claims.filterIsInstance<MdocRequestedClaim>().map { it.id }
        assertEquals(listOf("c1", null), ids)
    }

    /**
     * Unsupported formats must throw. Today only `mso_mdoc` and `dc+sd-jwt` are
     * handled — anything else (e.g. a future `w3c_jwt` format) must surface a clear
     * error rather than producing an empty/half-built [DcqlCredentialQuery].
     */
    @Test
    fun `toDcqlCredentialQuery fails for unsupported format`() {
        val query = CredentialQuery(
            id = QueryId("q1"),
            format = Format("unsupported_format"),
            // Match the format with a syntactically plausible (but ignored) meta blob.
            meta = buildJsonObject { put("dummy", JsonPrimitive("v")) },
            claims = null,
        )

        assertFailsWith<IllegalArgumentException> { query.toDcqlCredentialQuery() }
    }
}
