//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.transfer.openId4vp.dcql](../index.md)/[DcqlRequestProcessor](index.md)

# DcqlRequestProcessor

[androidJvm]\
class [DcqlRequestProcessor](index.md)(documentManager: DocumentManager, var openid4VpX509CertificateTrust: [OpenId4VpReaderTrust](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-reader-trust/index.md)) : RequestProcessor

Processes OpenID4VP requests using DCQL (Digital Credentials Query Language).

This processor validates and processes credential presentation requests using the DCQL format. It matches credential requirements against locally available documents in the wallet, handling multiple document formats:

- 
   MSO mdoc (ISO 18013-5 mobile driving license format)
- 
   SD-JWT VC (Selective Disclosure JSON Web Token Verifiable Credentials)

The processor verifies the request validity, extracts the required credentials, and matches them with documents in the wallet that satisfy the requirements.

## Constructors

| | |
|---|---|
| [DcqlRequestProcessor](-dcql-request-processor.md) | [androidJvm]<br>constructor(documentManager: DocumentManager, openid4VpX509CertificateTrust: [OpenId4VpReaderTrust](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-reader-trust/index.md)) |

## Properties

| Name | Summary |
|---|---|
| [openid4VpX509CertificateTrust](openid4-vp-x509-certificate-trust.md) | [androidJvm]<br>var [openid4VpX509CertificateTrust](openid4-vp-x509-certificate-trust.md): [OpenId4VpReaderTrust](../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-reader-trust/index.md)<br>Verifies trust in the reader's certificate for secure exchange |

## Functions

| Name | Summary |
|---|---|
| [process](process.md) | [androidJvm]<br>open override fun [process](process.md)(request: Request): RequestProcessor.ProcessedRequest<br>Processes an OpenID4VP request containing DCQL queries. |
