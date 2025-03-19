//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)/[withKtorHttpClientFactory](with-ktor-http-client-factory.md)

# withKtorHttpClientFactory

[androidJvm]\
fun [withKtorHttpClientFactory](with-ktor-http-client-factory.md)(ktorHttpClientFactory: () -&gt; HttpClient): &lt;Error class: unknown class&gt;

Configure with the given Ktor HTTP client factory to use for making HTTP requests. Ktor HTTP client is used by the [OpenId4VpManager](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-manager/index.md) and [OpenId4VciManager](../../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md) for making HTTP requests.

If not set, the default Ktor HTTP client factory will be used which is initialized with the default

#### Return

this [Builder](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| ktorHttpClientFactory | the Ktor HTTP client factory |
