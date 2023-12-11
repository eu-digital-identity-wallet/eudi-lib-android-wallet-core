//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.document.issue.openid4vci](../index.md)/[OpenId4VciManager](index.md)/[issueDocument](issue-document.md)

# issueDocument

[androidJvm]\

@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)

fun [issueDocument](issue-document.md)(
docType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html),
executor: [Executor](https://developer.android.com/reference/kotlin/java/util/concurrent/Executor.html)? =
null, callback: [OpenId4VciManager.OnIssueCallback](-on-issue-callback/index.md))

Issues a document of the given type.

#### Parameters

androidJvm

|          |                                                                               |
|----------|-------------------------------------------------------------------------------|
| docType  | the type of the document to issue (e.g. &quot;eu.europa.ec.eudiw.pid.1&quot;) |
| executor | the executor in which the callback will be invoked.                           |
| callback | the callback to be invoked when the result is available                       |
