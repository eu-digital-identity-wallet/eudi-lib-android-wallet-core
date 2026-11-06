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

import java.security.cert.X509Certificate

/**
 * How the verifier of an OpenID4VP request is authenticated.
 */
internal sealed interface OpenId4VpReaderAuth {

    /**
     * The verifier is not authenticated with an X.509 certificate chain, for example the verifier
     * of a redirect_uri, pre-registered or origin request. An x5c in such a request is not
     * validated.
     */
    data object Absent : OpenId4VpReaderAuth

    /**
     * The verifier is identified by the X.509 certificate chain of the request.
     *
     * @property chain the certificate chain of the request
     * @property isTrusted whether the reader trust store validated [chain]
     */
    data class X509(
        val chain: List<X509Certificate>,
        val isTrusted: Boolean
    ) : OpenId4VpReaderAuth
}
