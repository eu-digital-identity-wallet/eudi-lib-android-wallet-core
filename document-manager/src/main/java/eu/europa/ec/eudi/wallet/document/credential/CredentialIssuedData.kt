/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.eudi.wallet.document.credential

import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import org.multipaz.cbor.Cbor
import org.multipaz.credential.SecureAreaBoundCredential
import org.multipaz.document.NameSpacedData
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.mdoc.mso.StaticAuthDataParser
import org.multipaz.sdjwt.credential.KeyBoundSdJwtVcCredential

/**
 * Sealed interface representing different types of credential data provided by issuers.
 * This interface acts as a common type for various credential formats used within the EUDI Wallet.
 */
sealed interface CredentialIssuedData {

    /**
     * Represents Mobile Security Object (MSO) data for Mobile Driving License (mDL) credential format.
     *
     * @property nameSpacedData The structured data containing namespace-organized credential attributes
     * @property staticAuthData Authentication data provided by the issuer for verification purposes
     */
    data class MsoMdoc(
        val nameSpacedData: NameSpacedData,
        val staticAuthData: StaticAuthDataParser.StaticAuthData
    ) : CredentialIssuedData

    /**
     * Represents a Selective Disclosure JWT Verifiable Credential format.
     *
     * @property issuedSdJwt The SD-JWT with claims as issued by the credential provider
     */
    data class SdJwtVc(val issuedSdJwt: SdJwt<JwtAndClaims>) : CredentialIssuedData
}

/**
 * Extension function to extract issuer provided data from a [SecureAreaBoundCredential].
 *
 * This function processes different types of secure area bound credentials and extracts
 * their corresponding [CredentialIssuedData] representation.
 *
 * @param D The specific type of [CredentialIssuedData] expected to be returned
 * @return A [Result] containing the extracted data of type [D] or a failure if the credential type is unsupported
 *         or if the conversion to the requested type fails
 * @throws IllegalArgumentException if the credential type is not supported
 */
inline fun <reified D : CredentialIssuedData> SecureAreaBoundCredential.getIssuedData(): Result<D> {
    return when (this) {
        is MdocCredential -> runCatching {
            CredentialIssuedData.MsoMdoc(
                nameSpacedData = NameSpacedData.fromIssuerProvidedData(issuerProvidedData.toByteArray()),
                staticAuthData = StaticAuthDataParser(issuerProvidedData.toByteArray()).parse()
            ) as D
        }

        is KeyBoundSdJwtVcCredential -> DefaultSdJwtOps.unverifiedIssuanceFrom(
            String(issuerProvidedData.toByteArray())
        ).map {
            CredentialIssuedData.SdJwtVc(
                issuedSdJwt = it
            ) as D
        }

        else -> Result.failure(IllegalArgumentException("Unsupported credential type"))
    }
}

/**
 * Creates a [NameSpacedData] object from raw issuer provided data in CBOR format.
 *
 * This function parses the CBOR-encoded issuer data and organizes it into a structured
 * namespace-based format that can be used by the wallet.
 *
 * @param issuerProvidedData Raw CBOR-encoded data from the credential issuer
 * @return A structured [NameSpacedData] object containing the parsed credential data
 */
fun NameSpacedData.Companion.fromIssuerProvidedData(issuerProvidedData: ByteArray): NameSpacedData {
    return NameSpacedData.Builder()
        .let { builder ->
            Cbor.decode(issuerProvidedData)["nameSpaces"].asMap
                .forEach { (nameSpace, dataElements) ->
                    dataElements.asArray.forEach { dataElement ->
                        val d =
                            dataElement.asTaggedEncodedCbor.asMap.mapKeys { it.key.asTstr }
                        d["elementIdentifier"]?.let { dataElementName ->
                            d["elementValue"]?.let { value ->
                                builder.putEntry(
                                    nameSpaceName = nameSpace.asTstr,
                                    dataElementName = dataElementName.asTstr,
                                    value = Cbor.encode(value)
                                )
                            }
                        }

                    }
                }

            builder.build()
        }
}