//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWalletConfig](../index.md)/[Builder](index.md)/[ktorHttpClientFactory](ktor-http-client-factory.md)

# ktorHttpClientFactory

[androidJvm]\
fun [ktorHttpClientFactory](ktor-http-client-factory.md)(factory: () -&gt;
HttpClient): [EudiWalletConfig.Builder](index.md)

Set the Ktor HttpClient factory. This factory will be used to create the Ktor HttpClient. This HttpClient will be used
by
the [eu.europa.ec.eudi.wallet.transfer.openid4vp.OpenId4vpManager](../../../eu.europa.ec.eudi.wallet.transfer.openid4vp/-open-id4vp-manager/index.md)
and [eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager](../../../eu.europa.ec.eudi.wallet.issue.openid4vci/-open-id4-vci-manager/index.md)

#### Return

[EudiWalletConfig.Builder](index.md)

#### Parameters

androidJvm

|         |
|---------|
| factory |

[androidJvm]\
var [ktorHttpClientFactory](ktor-http-client-factory.md): () -&gt; HttpClient?
