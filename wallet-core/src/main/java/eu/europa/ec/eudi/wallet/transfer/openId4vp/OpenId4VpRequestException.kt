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

package eu.europa.ec.eudi.wallet.transfer.openId4vp

import eu.europa.ec.eudi.openid4vp.AuthorizationRequestError

/**
 * An OpenID4VP request that the wallet did not accept.
 *
 * [error] identifies what was wrong with the request and is the same error that is reported to the
 * verifier. Consumers match on it to choose the message they show to the user; the exception message
 * itself describes the error for logs and is not meant to be displayed.
 *
 * @property error the error that the request was rejected with
 */
class OpenId4VpRequestException internal constructor(
    val error: AuthorizationRequestError,
) : RuntimeException(error.toString())
