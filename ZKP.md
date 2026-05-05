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
**Longfellow ZK** system, preloaded with the required circuits shipped in the library assets.

For details about the Longfellow ZK reference implementation, see: https://github.com/google/longfellow-zk

> **Circuit version note:** The default circuits bundled with this library
> are Longfellow **v7**, which support `IssuerSignedItem` CBOR maps in any
> key ordering. Older v6 circuits assume canonical key ordering
> and will fail proof generation for credentials whose issuer emits keys in
> a different order.

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
> Longfellow ZK implementation [v0.9](https://github.com/google/longfellow-zk/releases/tag/v0.9) 
> bundled with EUDI Wallet Core. 
> Incompatible circuits may fail to load or generate a proof.

### Known limitations

#### Longfellow circuit constraints
The current version of the Longfellow ZK implementation [v0.9](https://github.com/google/longfellow-zk/releases/tag/v0.9) has the following known limitations:

- **IssuerSignedItem size limits**:
  - Total encoded `IssuerSignedItem`: **max 119 bytes**
  - `elementIdentifier`: **max 32 bytes**
  - `elementValue` CBOR: **max 64 bytes**
  
  Attributes that exceed any of these limits will cause proof generation to fail.

- **MSO size limit (2533 bytes)**

  Larger MSOs will cause proof generation to fail.

#### Transport limitations

- When using proximity transfer over BLE in Central/Client mode, the transfer
  may remain incomplete.

### Fallback behavior

If ZKP proof generation fails for any reason, the library automatically falls
back to a regular `DeviceResponse` containing a `Document` structure (without
ZKP proof).