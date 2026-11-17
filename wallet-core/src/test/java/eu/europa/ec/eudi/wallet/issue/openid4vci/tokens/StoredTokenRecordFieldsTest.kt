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

package eu.europa.ec.eudi.wallet.issue.openid4vci.tokens

import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.DeferredTokens
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.IssuanceMetadata
import eu.europa.ec.eudi.wallet.issue.openid4vci.reissue.StoredDeferredContext
import kotlin.test.assertTrue
import kotlinx.serialization.descriptors.elementNames
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The encrypted tokens are bound to every field of a stored record but the ones listed as unbound,
 * which must be the tokens and the fields kept for the transaction log, and must be fields of the record.
 */
class StoredTokenRecordFieldsTest {

    @Test
    fun `a stored deferred context leaves unbound only its tokens and the fields kept for the transaction log`() {
        // The fields that hold the transaction id and the tokens in a context stored in plain text, and the
        // field of the encrypted ones
        val tokens = DeferredTokens.serializer().descriptor.elementNames.toSet() + "sealedTokens"
        val keptForTheTransactionLog = setOf("interactingParty", "isUserTriggered")

        assertEquals(tokens + keptForTheTransactionLog, StoredDeferredContext.UNBOUND_FIELDS)
        assertTrue(StoredDeferredContext.serializer().descriptor.elementNames.toSet().containsAll(StoredDeferredContext.UNBOUND_FIELDS))
    }

    @Test
    fun `stored issuance metadata leave unbound only their tokens and the fields kept for the transaction log`() {
        val keptForTheTransactionLog = setOf("issuerIdentifier", "issuerName")

        assertEquals(
            IssuanceMetadataStore.TOKEN_FIELDS + "sealedTokens" + keptForTheTransactionLog,
            IssuanceMetadataStore.UNBOUND_FIELDS
        )
        assertTrue(
            IssuanceMetadata.serializer().descriptor.elementNames.toSet()
                .containsAll(IssuanceMetadataStore.UNBOUND_FIELDS - "sealedTokens")
        )
    }
}
