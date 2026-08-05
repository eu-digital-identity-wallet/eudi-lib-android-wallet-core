//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet.trust](../../index.md)/[TrustPolicy](../index.md)/[Companion](index.md)/[build](build.md)

# build

[androidJvm]\
fun [build](build.md)(block: [TrustPolicy.Builder](../-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)): [TrustPolicy](../index.md)

Creates a policy using the [Builder](../-builder/index.md) DSL.

Example:

```kotlin
val policy = TrustPolicy.build {
    default(Action.ENFORCE)
    forContext(VerificationContext.PID, Action.INFORM)
    forDocType("org.iso.18013.5.1.mDL", Action.INFORM)
}
```

#### Return

a [TrustPolicy](../index.md) configured according to the builder

#### Parameters

androidJvm

| | |
|---|---|
| block | configuration block applied to the [Builder](../-builder/index.md) |
