# ZERO KNOWLEDGE PROOF (ZKP)

The **EUDI Wallet Core** library includes support for ZKP (Zero Knowledge Proof), as an optional feature
that can be enabled in your application.

## Enabling the ZKP

To enable ZKP, configure the `EudiWallet` with a `ZkSystemRepository`, which is responsible for providing the ZK systems 
and circuits needed for proof generation.

### Provide your own `ZkSystemRepository`

If you already have your own repository implementation, you can provide it directly:

```kotlin
val zkpRepository: ZkSystemRepository = MyZkSystemRepository()

val config = EudiWalletConfig()
    .configureZkp(
        zkSystemRepository = zkpRepository
    )
    // ... Rest of your configurations

val eudiWallet = EudiWallet(context, config)
```

### Use the default Longfellow ZKP repository

The library provides a default `ZkSystemRepository` implementation based on the 
**Longfellow ΖΚ** system, preloaded with the required circuits shipped in the library assets.

For details about the Longfellow ZK reference implementation, see: https://github.com/google/longfellow-zk

```kotlin

val longfellowZKSystemRepository: ZkSystemRepository =
    LongfellowZkSystemRepository(
        circuits = LongfellowCircuits.get(context)
    ).build()

val config = EudiWalletConfig()
    .configureZkp(
        zkSystemRepository = longfellowZKSystemRepository
    )
    // ... Rest of your configurations

val eudiWallet = EudiWallet(context, config)
```

### Use Longfellow with custom circuits

If you want to provide your own circuits, you can build the Longfellow repository from externally as follows:

```kotlin
val customCircuits: List<Circuit> = listOf(
    Circuit(filename = "<my_circuit_1>", bytes = ByteString(myCircuitBytes1)),
    Circuit(filename = "<my_circuit_2>", bytes = ByteString(myCircuitBytes2))
    // ...
)

val zkpRepository: ZkSystemRepository =
    LongfellowZkSystemRepository(customCircuits).build()

val config = EudiWalletConfig()
    .configureZkp(
        zkSystemRepository = zkpRepository
    )
    // ... Rest of your configurations

val eudiWallet = EudiWallet(context, config)
```

> **Important:** If you provide your own circuits, make sure they are **compatible** with the current 
> Longfellow ZK implementation bundled with EUDI Wallet Core. 
> Incompatible circuits may fail to load or generate a proof.

### Known limitations

1. The current version of the Longfellow ZK implementation has the following known limitations:

 -  **IssuerSignedItem field order must be preserved** 

The `IssuerSignedItem` structures provided by the issuer must be encoded with the following field order:

```cddl
IssuerSignedItem = {
   "digestID" : uint,
   "random" : bstr,
   "elementIdentifier" : DataElementIdentifier,
   "elementValue" : DataElementValue
}
```

If the issuer encodes these fields in a different order, **proof generation will fail** with the current implementation.

 - **IssuerSignedItem size limit (96 bytes)**

The current implementation expects each encoded `IssuerSignedItem` to be **at most 96 bytes**.
If an `IssuerSignedItem` exceeds this limit, **proof generation will fail**.

 - **Mobile security object (MSO) size should not exceed 2213 bytes**
If an `MSO` exceeds this limit, **proof generation will fail**.

2. When using proximity transfer over BLE in Central/Client mode, the transfer may remains incomplete.

> **Fallback behavior:** If ZKP proof generation fails for any reason, the library automatically falls 
> back to a `DeviceResponse` that contains a `Document` structure instead of a `ZkDocument` (i.e., without ZKP proof).