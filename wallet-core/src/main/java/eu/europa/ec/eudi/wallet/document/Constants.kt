/*
 *  Copyright (c) 2023-2024 European Commission
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

package eu.europa.ec.eudi.wallet.document

/**
 * Shared constant values for doc types and namespaces
 *
 * @constructor Create empty Constants
 */
@Deprecated(message = "Will be removed completely in following version. Replace with actual values.")
object Constants {

    // EU PID
    const val EU_PID_DOCTYPE = "eu.europa.ec.eudi.pid.1"
    const val EU_PID_NAMESPACE = "eu.europa.ec.eudi.pid.1"

    // mDL
    const val MDL_DOCTYPE = "org.iso.18013.5.1.mDL"
    const val MDL_NAMESPACE = "org.iso.18013.5.1"

    // mdoc
    const val MDOC_FORMAT = "mso_mdoc"
    const val MDOC_FORMAT_ZKP = "mso_mdoc+zkp"

    // sdjwt
    const val SDJWT_FORMAT = "vc+sd-jwt"
    const val SDJWT_FORMAT_ZKP = "vc+sd-jwt+zkp"
}