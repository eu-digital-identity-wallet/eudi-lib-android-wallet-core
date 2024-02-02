//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet](../index.md)/[EudiWallet](index.md)/[createIssuanceRequest](create-issuance-request.md)

# createIssuanceRequest

[androidJvm]\
fun [createIssuanceRequest](create-issuance-request.md)(docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), hardwareBacked: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), attestationChallenge: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)? = null): CreateIssuanceRequestResult

Create an issuance request for the given [docType](create-issuance-request.md)

#### Return

CreateIssuanceRequestResult

#### Parameters

androidJvm

| | |
|---|---|
| docType | the docType of the document |
| hardwareBacked | flag that indicates if the document's keys should be stored in hardware or not |
| attestationChallenge | the attestation challenge to be used for the document's keys attestation (optional). If not provided, the sdk will generate a random challenge |

#### See also

| |
|---|
| DocumentManager.createIssuanceRequest |

#### Throws

| | |
|---|---|
| [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) | if [EudiWallet](index.md) is not firstly initialized via the [init](init.md) method |
