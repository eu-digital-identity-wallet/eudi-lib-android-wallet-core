# Changelog

See release notes in project's github repository.

## [0.11.0-SNAPSHOT]

__ 25 Jul 2024__

- Deprecate OpenId4Vci methods through EudiWallet object
- Add Eudi.createOpenId4VciManager method to create OpenId4VciManager object
- Bumb eudi-lib-andorid-wallet-document-manager to v0.4.2

## [0.10.3-SNAPSHOT]

__ 23 Jul 2024__

- Support custom logger for OpenId4VCI and OpenId4VP via OpenId4VCIConfig
- Fix OpenId4VCI resume authorization
- Return DocumentExpired data class from issueDeferredDocument method when the document is expired

## [0.10.2]

__ 17 Jul 2024__

- Release version 0.10.2

## [0.10.2-SNAPSHOT]

__15 Jul 2024__

- Fix overriding KtorHttpClientFactory for OpenId4VCI and OpenId4VP
- Fix Deferred Issuance for OpenId4VCI by not deleting the deferred document after issuance failure

## [0.10.1-SNAPSHOT]

__3 Jul 2024__

- Bump eudi-lib-android-wallet-document-manager to v0.4.1-SNAPSHOT

## [0.10.0-SNAPSHOT]

__27 Jun 2024__

- Bump eudi-lib-jvm-openid4vci-kt to v0.3.2
- Bump eudi-lib-android-wallet-document-manager to v0.4.0-SNAPSHOT
- Support for OpenId4VCI pre-authorization flow
- Support for OpenId4VCI deferred issuance
- Extensive logging for OpenId4VCI and OpenId4VP
- Allow overriding ktor client for OpenId4VCI and OpenId4VP
- Bug fixes in OpenId4VCI

## [0.9.5-SNAPSHOT]

__14 Jun 2024__

- Bump eudi-lib-jvm-openid4vci-kt to v0.3.1
- Configurable debug logging with level support for OpenId4VCI
- Support for overriding ktor client for OpenId4VCI
- Bug fixes in OpenId4VCI

## [0.9.4-SNAPSHOT]

__10 Jun 2024__

- Security improvements for dPoP Signer for OpendI4VCI

## [0.9.3-SNAPSHOT]

_10 Jun 2024_

- Bump eudi-lib-jvm-openid4vci-kt to v0.3.0
- Support CWT proofs for OpenId4VCI
- Configure the usage of PAR for OpendId4VCI
- Improvements for proof type selection

## [0.9.2-SNAPSHOT]

_04 Jun 2024_

- Support multiple URL schemes for OpenId4Vp

## [0.9.1-SNAPSHOT]

_31 May 2024_

- Bump eudi-lib-jvm-siop-openid4vp-kt to v0.4.2

## [0.9.0-SNAPSHOT]

_28 May 2024_

- Bump eu.europa.ec.eudi:eudi-lib-android-wallet-document-manager to 0.3.0-SNAPSHOT

## [0.8.0-SNAPSHOT]

_24 May 2024_

- DPoP support for OpenId4VCI

## [0.7.1-SNAPSHOT]

_24 May 2024_

- Bump eudi-lib-jvm-siop-openid4vp-kt to v0.4.0

## [0.7.0-SNAPSHOT]

_23 May 2024_

- Bump eudi-lib-jvm-siop-openid4vp-kt to v0.3.5
- Added support to provide the Legal Name of Preregistered Verifiers in Openid4vp Configuration
- Use the Legal Name provided by the the eudi-lib-jvm-siop-openid4vp-kt library in the ReaderAuth result

## [0.6.0-SNAPSHOT]

_22 May 2024_

- Support credential offer OpenId4VCI draft 13
- Update eudi-lib-jvm-openid4vci-kt library to 0.2.2
- Improvement on handling OpenId4VCI authorization code flow
- Allow to configure the usage of StrongBox for storing the document's keys
