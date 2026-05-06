//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.presentation](../index.md)/[PresentationManager](index.md)/[startRemotePresentation](start-remote-presentation.md)

# startRemotePresentation

[release]\
abstract fun [startRemotePresentation](start-remote-presentation.md)(uri: [Uri](https://developer.android.com/reference/kotlin/android/net/Uri.html), refererUrl: [String](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-string/index.html)? = null)

Start a remote presentation with the given URI The URI could be either

- 
   a REST API request ISO-18013-7
- 
   a OpenId4Vp request

#### Parameters

release

| | |
|---|---|
| uri | the URI |

[release]\
abstract fun [~~startRemotePresentation~~](start-remote-presentation.md)(intent: [Intent](https://developer.android.com/reference/kotlin/android/content/Intent.html))

---

### Deprecated

Use startRemotePresentation(uri: Uri)

---

Start a remote presentation with the given intent The intent.data could either contain the URI of

- 
   a REST API request ISO-18013-7
- 
   a OpenId4Vp request

#### Parameters

release

| | |
|---|---|
| intent | the intent |