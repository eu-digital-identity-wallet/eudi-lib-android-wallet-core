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

package eu.europa.ec.eudi.wallet.trustmark

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data model for the EUDI Wallet Trust Mark Information as defined in
 * EC TS01 v1.2 (2026-06) — Specification of EUDI Wallet Trust Mark.
 *
 * This object combines the information required for configuring the EUDI Wallet Trust Mark
 * in a certified Wallet Solution. It is delivered by the European Commission to the
 * Wallet Provider after certification.
 *
 * JSON schema: `http://data.europa.eu/eudi/trustmarkinformation.json`
 *
 * @property trustMarkResourceURL URL of the official EUDI Wallet Trust Mark graphics and
 *   user info resources for rendering in the Wallet user interface.
 * @property listOfCertifiedWalletsURL URL of the public list of certified EUDI Wallet Solutions in EU.
 * @property walletSolutionInfoPageURL URL to the certified Wallet Solution's own information page
 *   under the list of certified EUDI Wallet Solutions page. Constructed from the
 *   [listOfCertifiedWalletsURL] appended with `?` + the WalletSolutionID identifier.
 * @property listOfCertifiedWalletsQRCode Base64-encoded QR code containing the
 *   [listOfCertifiedWalletsURL] (ISO-8859-1 byte mode). Optional.
 * @property walletSolutionInfoPageQRCode Base64-encoded QR code containing the
 *   [walletSolutionInfoPageURL] (ISO-8859-1 byte mode). Optional.
 * @property walletVerifierToolURL URL pointing to the official EUDI Wallet Verification Tool's
 *   `/.well-known/openid-credential-issuer` endpoint. Optional; verification tool
 *   functionality may be specified in a future version of the specification.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class TrustMarkInformation(
    @SerialName("TrustMarkResourceURL")
    val trustMarkResourceURL: String,

    @SerialName("ListOfCertifiedWalletsURL")
    val listOfCertifiedWalletsURL: String,

    @SerialName("WalletSolutionInfoPageURL")
    val walletSolutionInfoPageURL: String,

    @SerialName("ListOfCertifiedWalletsQRCode")
    val listOfCertifiedWalletsQRCode: String? = null,

    @SerialName("WalletSolutionInfoPageQRCode")
    val walletSolutionInfoPageQRCode: String? = null,

    @SerialName("WalletVerifierToolURL")
    val walletVerifierToolURL: String? = null,
)
