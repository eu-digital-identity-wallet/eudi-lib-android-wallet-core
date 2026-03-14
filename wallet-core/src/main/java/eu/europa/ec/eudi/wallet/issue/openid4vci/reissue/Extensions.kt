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

package eu.europa.ec.eudi.wallet.issue.openid4vci.reissue

import com.nimbusds.jose.jwk.JWK
import eu.europa.ec.eudi.openid4vci.BatchCredentialIssuance
import eu.europa.ec.eudi.openid4vci.BatchSignOperation
import eu.europa.ec.eudi.openid4vci.BatchSigner
import eu.europa.ec.eudi.openid4vci.ClaimPathElement
import eu.europa.ec.eudi.openid4vci.Credential
import eu.europa.ec.eudi.openid4vci.CredentialConfiguration
import eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
import eu.europa.ec.eudi.openid4vci.CredentialIssuerMetadata
import eu.europa.ec.eudi.openid4vci.CredentialOffer
import eu.europa.ec.eudi.openid4vci.HasKeyAttestationRequirement
import eu.europa.ec.eudi.openid4vci.IssuanceRequestPayload
import eu.europa.ec.eudi.openid4vci.Issuer
import eu.europa.ec.eudi.openid4vci.JwtBindingKey
import eu.europa.ec.eudi.openid4vci.KeyAttestationJWT
import eu.europa.ec.eudi.openid4vci.KeyAttestationRequirement
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.ProofsSpecification
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.openid4vci.SignOperation
import eu.europa.ec.eudi.openid4vci.Signer
import eu.europa.ec.eudi.openid4vci.SubmissionOutcome
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentManager
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.UnsignedDocument
import eu.europa.ec.eudi.wallet.document.credential.IssuerProvidedCredential
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import eu.europa.ec.eudi.wallet.issue.openid4vci.javaAlgorithm
import eu.europa.ec.eudi.wallet.provider.WalletAttestationsProvider
import org.multipaz.securearea.KeyInfo
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.SecureArea
import java.util.Base64
import eu.europa.ec.eudi.wallet.provider.WalletKeyManager
import kotlinx.coroutines.runBlocking

/**
 * Checks if the issuer supports pre-authorization.
 * @return True if the issuer has a pre-authorization code, false otherwise.
 */
fun Issuer.isPreAuthorized(): Boolean = credentialOffer.grants?.preAuthorizedCode() != null

val CredentialIssuerMetadata.batchCredentialIssuanceSize: Int
    get() = (this.batchCredentialIssuance as? BatchCredentialIssuance.Supported)
        ?.batchSize ?: 1

val CredentialOffer.credentialConfigurations: Map<CredentialConfigurationIdentifier, CredentialConfiguration>
    get() = this.credentialIssuerMetadata.credentialConfigurationsSupported
        .filterKeys { it in credentialConfigurationIdentifiers }

val CredentialOffer.batchCredentialIssuanceSize: Int
    get() = credentialIssuerMetadata.batchCredentialIssuanceSize

fun CredentialIssuerMetadata.toIssuerMetadata(credentialConfigurationIdentifier: CredentialConfigurationIdentifier) =
    runCatching {
        val credentialMetadata = checkNotNull(
            credentialConfigurationsSupported[credentialConfigurationIdentifier]
                ?.credentialMetadata
        ) { "Credential configuration $credentialConfigurationIdentifier not found in issuer metadata" }

        IssuerMetadata(
            credentialIssuerIdentifier = credentialIssuerIdentifier.value.value.toString(),
            documentConfigurationIdentifier = credentialConfigurationIdentifier.value,
            issuerDisplay = display.map {
                IssuerMetadata.IssuerDisplay(
                    name = it.name,
                    locale = it.locale,
                    logo = it.logo?.let { logo ->
                        IssuerMetadata.Logo(
                            logo.uri,
                            logo.alternativeText
                        )
                    }
                )
            },
            display = credentialMetadata.display?.map {
                IssuerMetadata.Display(
                    name = it.name,
                    locale = it.locale,
                    logo = it.logo?.let { logo ->
                        IssuerMetadata.Logo(
                            logo.uri,
                            logo.alternativeText
                        )
                    },
                    description = it.description,
                    backgroundColor = it.backgroundColor,
                    textColor = it.textColor,
                    backgroundImageUri = it.backgroundImage
                )
            } ?: emptyList(),
            claims = credentialMetadata.claims
                ?.map { claim ->
                    IssuerMetadata.Claim(
                        path = claim.path.value.filterIsInstance<ClaimPathElement.Claim>()
                            .map { p -> p.name }
                            .toList(),
                        mandatory = claim.mandatory,
                        display = claim.display.map { display ->
                            IssuerMetadata.Claim.Display(
                                name = display.name,
                                locale = display.locale
                            )
                        }
                    )
                },
        )
    }

val CredentialConfiguration.documentFormat: DocumentFormat
    get() = when (this) {
        is MsoMdocCredential -> MsoMdocFormat(docType)
        is SdJwtVcCredential -> SdJwtVcFormat(type)
        else -> throw IllegalArgumentException("Unsupported credential configuration type: ${this::class.java.name}")
    }

class PendingIssuanceDocument(
    val credentialConfigurationIdentifier: CredentialConfigurationIdentifier,
    val credentialConfiguration: CredentialConfiguration,
    val unsignedDocument: UnsignedDocument,
    val credentialOffer: CredentialOffer? = null,
) {
    suspend fun getNoKeyAttestationProofSpecification(
        keyUnlockDataProvider: suspend (SecureArea, KeyInfo) -> KeyUnlockData? = { _, _ -> null },
    ) = unsignedDocument.getNoKeyAttestationProofSpecification(keyUnlockDataProvider)

    suspend fun getWithKeyAttestationProofSpecification(
        walletAttestationsProvider: WalletAttestationsProvider,
        keyIndex: Int = 0,
        keyUnlockDataProvider: suspend (SecureArea, KeyInfo) -> KeyUnlockData? = { _, _ -> null },
    ) = unsignedDocument.getWithKeyAttestationProofSpecification(
        walletAttestationsProvider,
        keyIndex,
        keyUnlockDataProvider
    )

    val issuanceRequestPayload: IssuanceRequestPayload
        get() = IssuanceRequestPayload.ConfigurationBased(
            credentialConfigurationIdentifier = credentialConfigurationIdentifier,
        )
}

suspend fun DocumentManager.createDocument(
    credentialConfigurationIdentifier: CredentialConfigurationIdentifier,
    credentialConfiguration: CredentialConfiguration,
    createDocumentSettings: CreateDocumentSettings,
    issuerMetadata: CredentialIssuerMetadata,
): Result<PendingIssuanceDocument> = createDocument(
    format = credentialConfiguration.documentFormat,
    createSettings = createDocumentSettings,
    issuerMetadata = issuerMetadata.toIssuerMetadata(credentialConfigurationIdentifier).getOrThrow()
).kotlinResult.map { unsignedDocument ->
    PendingIssuanceDocument(
        credentialConfigurationIdentifier = credentialConfigurationIdentifier,
        credentialConfiguration = credentialConfiguration,
        unsignedDocument = unsignedDocument,
        credentialOffer = null
    )
}

suspend fun DocumentManager.createDocument(
    credentialConfigurationIdentifier: CredentialConfigurationIdentifier,
    credentialConfiguration: CredentialConfiguration,
    createDocumentSettings: CreateDocumentSettings,
    credentialOffer: CredentialOffer,
): Result<PendingIssuanceDocument> = createDocument(
    format = credentialConfiguration.documentFormat,
    createSettings = createDocumentSettings,
    issuerMetadata = credentialOffer.credentialIssuerMetadata.toIssuerMetadata(
        credentialConfigurationIdentifier
    ).getOrThrow()
).kotlinResult.map { unsignedDocument ->
    PendingIssuanceDocument(
        credentialConfigurationIdentifier = credentialConfigurationIdentifier,
        credentialConfiguration = credentialConfiguration,
        unsignedDocument = unsignedDocument,
        credentialOffer = credentialOffer
    )
}

suspend fun DocumentManager.storeIssuedDocument(
    pendingIssuanceDocument: PendingIssuanceDocument,
    outcome: SubmissionOutcome.Success,
): Result<IssuedDocument> {
    require(outcome.credentials.all { it.credential is Credential.Str }) {
        "All issued credentials must be of type Credential.Str"
    }

    val credentials = outcome.credentials.map { it.credential }.filterIsInstance<Credential.Str>()

    val expectedNumberOfCredentials =
        pendingIssuanceDocument.unsignedDocument.getPoPSigners().toList().size

    require(credentials.size == expectedNumberOfCredentials) {
        "Number of issued credentials (${credentials.size}) does not match number of credential keys ($expectedNumberOfCredentials)"
    }

    val keyAliases =
        pendingIssuanceDocument.unsignedDocument.getPoPSigners().toList().map { it.keyAlias }

    val issuerProvidedData = credentials.zip(keyAliases).map { (credential, keyAlias) ->
        IssuerProvidedCredential(
            publicKeyAlias = keyAlias,
            data = when (pendingIssuanceDocument.unsignedDocument.format) {
                is MsoMdocFormat -> Base64.getUrlDecoder().decode(credential.value)
                is SdJwtVcFormat -> credential.value.toByteArray(charset = Charsets.US_ASCII)
            }
        )
    }

    return storeIssuedDocument(
        pendingIssuanceDocument.unsignedDocument,
        issuerProvidedData
    ).kotlinResult
}

suspend fun UnsignedDocument.getNoKeyAttestationProofSpecification(
    keyUnlockDataProvider: suspend (SecureArea, KeyInfo) -> KeyUnlockData? = { _, _ -> null },
): Result<ProofsSpecification.JwtProofs.NoKeyAttestation> = runCatching {
    val javaAlgorithm =
        checkNotNull(getPoPSigners().toList().first().getKeyInfo().algorithm.javaAlgorithm) {
            "Algorithm not supported"
        }
    ProofsSpecification.JwtProofs.NoKeyAttestation(object : BatchSigner<JwtBindingKey> {
        override val javaAlgorithm: String = javaAlgorithm

        override suspend fun authenticate(): BatchSignOperation<JwtBindingKey> {
            return BatchSignOperation(getPoPSigners().toList().map { signer ->
                val jwk = JWK.parse(signer.getKeyInfo().publicKey.toJwk().toString())
                val keyUnlockData = keyUnlockDataProvider(signer.secureArea, signer.getKeyInfo())
                SignOperation(
                    function = { input -> signer.signPoP(input, keyUnlockData).toDerEncoded() },
                    publicMaterial = JwtBindingKey.Jwk(jwk)
                )
            })
        }

        override suspend fun release(signOps: BatchSignOperation<JwtBindingKey>?) {
            // Nothing to release
        }

    })
}

suspend fun UnsignedDocument.getWithKeyAttestationProofSpecification(
    walletAttestationsProvider: WalletAttestationsProvider,
    keyIndex: Int = 0,
    keyUnlockDataProvider: suspend (SecureArea, KeyInfo) -> KeyUnlockData? = { _, _ -> null },
): Result<ProofsSpecification.JwtProofs.WithKeyAttestation> =
    runCatching {
        val proofSigner = checkNotNull(getPoPSigners().toList().getOrNull(keyIndex)) {
            "No signer available at index $keyIndex"
        }
        val javaAlgorithm = checkNotNull(proofSigner.getKeyInfo().algorithm.javaAlgorithm) {
            "Algorithm not supported"
        }
        ProofsSpecification.JwtProofs.WithKeyAttestation(
            keyIndex = keyIndex,
            proofSignerProvider = { nonce ->
                object : Signer<KeyAttestationJWT> {
                    override val javaAlgorithm: String = javaAlgorithm

                    override suspend fun acquire(): SignOperation<KeyAttestationJWT> {
                        val keyAttestationJWT = walletAttestationsProvider.getKeyAttestation(
                            keys = getPoPSigners().toList().map { it.getKeyInfo() },
                            nonce = nonce
                        ).map {
                            KeyAttestationJWT(it)
                        }.getOrThrow()

                        val keyUnlockData =
                            keyUnlockDataProvider(proofSigner.secureArea, proofSigner.getKeyInfo())
                        return SignOperation(
                            publicMaterial = keyAttestationJWT,
                            function = { input ->
                                proofSigner.signPoP(input, keyUnlockData).toDerEncoded()
                            }
                        )
                    }

                    override suspend fun release(signOperation: SignOperation<KeyAttestationJWT>?) {
                        // nothing to release
                    }
                }
            }
        )
    }

fun CredentialConfiguration.requiresKeyAttestation(): Boolean {
    return proofTypesSupported.values.all {
        it is HasKeyAttestationRequirement && it.keyAttestationRequirement is KeyAttestationRequirement.Required
    }
}

private fun WalletKeyManager.createSignerAdapter(keyAlias: String): Signer<JWK> {

    val key = runBlocking {
        this@createSignerAdapter.getWalletAttestationKey(keyAlias)
    } ?: error("Key alias '$keyAlias' not found in WalletKeyManager")

    return object : Signer<JWK> {

        override val javaAlgorithm: String = key.keyInfo.algorithm.javaAlgorithm
            ?: error("Key algorithm is required for alias $keyAlias")

        override suspend fun acquire(): SignOperation<JWK> {
            return SignOperation(
                // The library calls this function when it needs a signature
                function = { inputData ->
                    key.signFunction(inputData)
                },
                // The public key to embed in the header
                publicMaterial = JWK.parse(
                    key.keyInfo.publicKey.toJwk().toString()
                )
            )
        }

        override suspend fun release(signOperation: SignOperation<JWK>?) {
            // No-op for Android Keystore
        }
    }
}
