/*
 *  Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci

/**
 * The result of an offer operation.
 */
sealed interface OfferResult : OpenId4VciResult {
    /**
     * The offer was successful.
     * @property offer the offer
     */
    data class Success(val offer: Offer) : OfferResult

    /**
     * The offer failed.
     * @property cause the error that caused the failure
     */
    data class Failure(override val cause: Throwable) : OfferResult, OpenId4VciResult.Erroneous
}
