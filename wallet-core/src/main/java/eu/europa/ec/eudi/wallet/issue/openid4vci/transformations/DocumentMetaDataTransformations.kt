/*
 * Copyright (c) 2024 European Commission
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

package eu.europa.ec.eudi.wallet.issue.openid4vci.transformations

import eu.europa.ec.eudi.openid4vci.Claim
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.wallet.document.metadata.DocumentMetaData
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer.OfferedDocument
import java.util.Locale

fun OfferedDocument.extractDocumentMetaData(): DocumentMetaData {

    val documentDisplay = this.configuration.display.map { it.toDocumentDisplay() }

    val claims = when (val config = this.configuration) {
        is MsoMdocCredential -> config.claims.fromMsoDocToDocumentClaim()
        is SdJwtVcCredential -> config.claims.fromSdJwtVToDocumentClaim()
        else -> null
    }

    return DocumentMetaData(
        credentialIssuerIdentifier = offer.credentialOffer.credentialIssuerIdentifier.value.value.toString(),
        documentConfigurationIdentifier = configurationIdentifier.value,
        display = documentDisplay,
        claims = claims,
        issuerDisplay = this.offer.issuerMetadata.display.map {
            DocumentMetaData.IssuerDisplay(
                name = it.name ?: "",
                locale = Locale(it.locale ?: ""),
                logo = it.logo?.toDocumentLogo()
            )
        }
    )
}

private fun CredentialIssuerMetadata.Display.Logo?.toDocumentLogo(): DocumentMetaData.Logo? {
    if (this?.uri == null && this?.alternativeText == null) return null

    return DocumentMetaData.Logo(uri, alternativeText)
}

private fun Display.toDocumentDisplay(): DocumentMetaData.Display = DocumentMetaData.Display(
    name = name,
    locale = locale,
    logo = logo?.toDocumentLogo(),
    description = description,
    backgroundColor = backgroundColor,
    textColor = textColor
)

private fun Display.Logo.toDocumentLogo():
        DocumentMetaData.Logo =
    DocumentMetaData.Logo(uri, alternativeText)

private fun Map<String, Map<String, Claim>>.fromMsoDocToDocumentClaim(): List<DocumentMetaData.Claim> {

    return this.flatMap { (namespace, claimsMap) ->
        claimsMap.mapNotNull { (name, claim) ->
            val claimName = DocumentMetaData.Claim.Name.MsoMdoc(
                name = name,
                nameSpace = namespace
            )
            claim.fromMsoDocToDocumentClaim(claimName)
        }
    }
}

private fun Map<String, Claim?>?.fromSdJwtVToDocumentClaim(): List<DocumentMetaData.Claim>? {
    return this?.mapNotNull { (name, claim) ->
        val claimName = DocumentMetaData.Claim.Name.SdJwtVc(name = name)
        claim.fromMsoDocToDocumentClaim(claimName)
    }
}

private fun Claim?.fromMsoDocToDocumentClaim(name: DocumentMetaData.Claim.Name): DocumentMetaData.Claim =
    DocumentMetaData.Claim(
        name = name,
        mandatory = this?.mandatory,
        valueType = this?.valueType,
        display = this?.display?.map { display ->
            DocumentMetaData.Claim.Display(
                name = display.name,
                locale = display.locale
            )
        } ?: emptyList()
    )