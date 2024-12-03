package eu.europa.ec.eudi.wallet.issue.openid4vci.transformations

import eu.europa.ec.eudi.openid4vci.Claim
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.Display
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.wallet.document.metadata.DocumentMetaData

fun CredentialConfiguration.extractDocumentMetaData(): DocumentMetaData? {
    val documentDisplay = display.map { it.toDocumentDisplay() }

    val claims: Map<DocumentMetaData.ClaimName, DocumentMetaData.Claim>? = when (this) {
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

private fun Map<String, Map<String, Claim>>.fromMsoDocToDocumentClaim(): Map<DocumentMetaData.ClaimName, DocumentMetaData.Claim> {

    return this.flatMap { (namespace, claimsMap) ->
        claimsMap.mapNotNull { (name, claim) ->

            val claimName = DocumentMetaData.MsoMdocClaimName(
                name = name,
                nameSpace = namespace
            )
            val documentClaim = claim.fromMsoDocToDocumentClaim()
            claimName to documentClaim
        }
    }.toMap()
}

private fun Map<String, Claim?>?.fromSdJwtVToDocumentClaim(): Map<DocumentMetaData.ClaimName, DocumentMetaData.Claim>? {
    if (this == null) return null

    return mapNotNull { (name, claim) ->
        val claimName = DocumentMetaData.SdJwtVcsClaimName(name = name)
        val documentClaim = claim.fromMsoDocToDocumentClaim()
        claimName to documentClaim
    }.toMap()

}

private fun Claim?.fromMsoDocToDocumentClaim(): DocumentMetaData.Claim = DocumentMetaData.Claim(
    mandatory = this?.mandatory,
    valueType = this?.valueType,
    display = this?.display?.map { display ->
        DocumentMetaData.Claim.Display(
            name = display.name,
            locale = display.locale
        )
    } ?: emptyList()
)