//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp](../index.md)/[OpenId4VpRequestProcessor](index.md)

# OpenId4VpRequestProcessor

class [OpenId4VpRequestProcessor](index.md)(documentManager: DocumentManager, var openid4VpX509CertificateTrust: [OpenId4VpReaderTrust](../-open-id4-vp-reader-trust/index.md)) : RequestProcessor

Processes OpenID4VP (OpenID for Verifiable Presentations) requests using Presentation Exchange.

This processor implements the RequestProcessor interface to handle credential presentation requests conforming to the OpenID4VP protocol specification. The processor supports multiple document formats simultaneously, with special handling for:

- 
   MSO_MDOC: Mobile Security Object/Mobile Driving License format (ISO 18013-5)
- 
   SD-JWT VC: Selective Disclosure JWT Verifiable Credentials format

The processor analyzes presentation definition requirements from verifiers, matches them with available credentials in the wallet, and prepares disclosure requests for user approval. It integrates with the wallet's security architecture through reader authentication and trust verification.

#### Parameters

androidJvm

| | |
|---|---|
| documentManager | Provides access to credentials stored in the wallet |

## Constructors

| | |
|---|---|
| [OpenId4VpRequestProcessor](-open-id4-vp-request-processor.md) | [androidJvm]<br>constructor(documentManager: DocumentManager, openid4VpX509CertificateTrust: [OpenId4VpReaderTrust](../-open-id4-vp-reader-trust/index.md)) |

## Properties

| Name | Summary |
|---|---|
| [openid4VpX509CertificateTrust](openid4-vp-x509-certificate-trust.md) | [androidJvm]<br>var [openid4VpX509CertificateTrust](openid4-vp-x509-certificate-trust.md): [OpenId4VpReaderTrust](../-open-id4-vp-reader-trust/index.md)<br>Handles verification of relying party X.509 certificates |

## Functions

| Name | Summary |
|---|---|
| [process](process.md) | [androidJvm]<br>open override fun [process](process.md)(request: Request): RequestProcessor.ProcessedRequest<br>Processes an OpenID4VP request and generates an appropriate response processor. |
