package eu.europa.ec.eudi.wallet.issue.openid4vci.transformations

import eu.europa.ec.eudi.openid4vci.Claim
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.wallet.document.metadata.DocumentMetaData

fun CredentialConfiguration.extractDocumentMetaData(): DocumentMetaData {
    val documentDisplay = display.map { it.toDocumentDisplay() }

    val claims = when (this) {
        is MsoMdocCredential -> claims.fromMsoDocToDocumentClaim()
        is SdJwtVcCredential -> claims.fromSdJwtVToDocumentClaim()
        else -> null
    }

    return DocumentMetaData(
        display = documentDisplay,
        claims = claims
    )
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
        DocumentMetaData.Display.Logo =
    DocumentMetaData.Display.Logo(uri, alternativeText)

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