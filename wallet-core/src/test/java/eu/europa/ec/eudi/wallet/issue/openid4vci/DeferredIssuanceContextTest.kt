/*
 * Copyright (c) 2024-2025 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci

import eu.europa.ec.eudi.openid4vci.AccessToken
import eu.europa.ec.eudi.openid4vci.DeferredIssuanceContext
import eu.europa.ec.eudi.openid4vci.RefreshToken
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeferredIssuanceContextTest {

    @Test
    fun `when accessToken has no expiration time DeferredIssuanceContextTest_hasExpired property is false`() {
        val deferredIssuanceContext: DeferredIssuanceContext = mockk {
            every { authorizedTransaction.authorizedRequest.timestamp } returns Instant.now()
                .minusSeconds(3600)
            every { authorizedTransaction.authorizedRequest.accessToken } returns AccessToken(
                accessToken = "accessToken",
                expiresInSec = null,
                useDPoP = false
            )
            every { authorizedTransaction.authorizedRequest.refreshToken } returns null
        }

        assertFalse(deferredIssuanceContext.hasExpired)
    }

    @Test
    fun `when accessToken has expired DeferredIssuanceContextTest_hasExpired property is true`() {
        val deferredIssuanceContext: DeferredIssuanceContext = mockk {
            every { authorizedTransaction.authorizedRequest.timestamp } returns Instant.now()
                .minusSeconds(3600)
            every { authorizedTransaction.authorizedRequest.accessToken } returns AccessToken(
                accessToken = "accessToken",
                expiresInSec = 60L,
                useDPoP = false
            )
            every { authorizedTransaction.authorizedRequest.refreshToken } returns null
        }

        assertTrue(deferredIssuanceContext.hasExpired)
    }

    @Test
    fun `when accessToken has not expired and there is no refreshToken DeferredIssuanceContextTest_hasExpired property is false`() {
        val deferredIssuanceContext: DeferredIssuanceContext = mockk {
            every { authorizedTransaction.authorizedRequest.timestamp } returns Instant.now()
                .minusSeconds(3600)
            every { authorizedTransaction.authorizedRequest.accessToken } returns AccessToken(
                accessToken = "accessToken",
                expiresInSec = 6000L,
                useDPoP = false
            )
            every { authorizedTransaction.authorizedRequest.refreshToken } returns null
        }

        assertFalse(deferredIssuanceContext.hasExpired)
    }

    @Test
    fun `when accessToken has expired and there is refreshToken DeferredIssuanceContextTest_hasExpired property is false`() {
        val deferredIssuanceContext: DeferredIssuanceContext = mockk {
            every { authorizedTransaction.authorizedRequest.timestamp } returns Instant.now()
                .minusSeconds(3600)
            every { authorizedTransaction.authorizedRequest.accessToken } returns AccessToken(
                accessToken = "accessToken",
                expiresInSec = 10L,
                useDPoP = false
            )
            every { authorizedTransaction.authorizedRequest.refreshToken } returns RefreshToken(
                refreshToken = "refreshToken",
            )
        }

        assertFalse(deferredIssuanceContext.hasExpired)
    }
}