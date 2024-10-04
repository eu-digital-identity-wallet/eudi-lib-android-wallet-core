package eu.europa.ec.eudi.wallet.issue.openidvci

import java.net.URL

data class PARResponse (
    val authorizationCodeURL: URL,
    val pkceVerifier: PKCEVerifier,
    val state: String,
)

data class PKCEVerifier(
    val codeVerifier: String,
    val codeVerifierMethod: String,
)