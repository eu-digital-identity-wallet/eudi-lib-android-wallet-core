/*
 * Copyright (c) 2023 European Commission
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

package eu.europa.ec.eudi.wallet.document

/**
 * Shared constant values for doc types and namespaces
 *
 * @constructor Create empty Constants
 */
object Constants {

    // EU PID
    const val EU_PID_DOCTYPE = "eu.europa.ec.eudiw.pid.1"
    const val EU_PID_NAMESPACE = "eu.europa.ec.eudiw.pid.1"

    // mDL
    const val MDL_DOCTYPE = "org.iso.18013.5.1.mDL"
    const val MDL_NAMESPACE = "org.iso.18013.5.1"

    // AAMVA NAMESPACE
    const val AAMVA_NAMESPACE = "org.iso.18013.5.1.aamva"

    // Vehicle Registration
    const val MVR_DOCTYPE = "nl.rdw.mekb.1"
    const val MVR_NAMESPACE = "nl.rdw.mekb.1"

    // COVID
    const val MICOV_DOCTYPE = "org.micov.1"
    const val MICOV_VTR_NAMESPACE = "org.micov.vtr.1"
    const val MICOV_ATT_NAMESPACE = "org.micov.attestation.1"
}