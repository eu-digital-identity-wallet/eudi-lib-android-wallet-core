//[document-manager](../../../index.md)/[eu.europa.ec.eudi.wallet.document](../index.md)/[Outcome](index.md)/[getOrThrow](get-or-throw.md)

# getOrThrow

[release]\
fun [getOrThrow](get-or-throw.md)(): [T](index.md)

Returns the encapsulated value if this instance represents a successful outcome or throws the encapsulated exception if it is failure.

#### Return

the encapsulated value if this instance represents a successful outcome

#### Throws

| | |
|---|---|
| [Throwable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-throwable/index.html) | the encapsulated exception if this instance represents a failure outcome |