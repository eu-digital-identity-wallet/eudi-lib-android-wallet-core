//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../index.md)/[IssuerTrustConfigBuilder](index.md)/[policy](policy.md)

# policy

[androidJvm]\
fun [policy](policy.md)(block: [TrustPolicy.Builder](../-trust-policy/-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html))

Configures the trust policy using the [TrustPolicy.Builder](../-trust-policy/-builder/index.md) DSL.

If not called, the default policy is [TrustPolicy.Action.ENFORCE](../-trust-policy/-action/-e-n-f-o-r-c-e/index.md) for all credentials.

#### Parameters

androidJvm

| | |
|---|---|
| block | configuration block applied to the [TrustPolicy.Builder](../-trust-policy/-builder/index.md) |
