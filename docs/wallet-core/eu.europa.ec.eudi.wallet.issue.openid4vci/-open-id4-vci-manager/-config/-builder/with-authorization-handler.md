//[wallet-core](../../../../../index.md)/[eu.europa.ec.eudi.wallet.issue.openid4vci](../../../index.md)/[OpenId4VciManager](../../index.md)/[Config](../index.md)/[Builder](index.md)/[withAuthorizationHandler](with-authorization-handler.md)

# withAuthorizationHandler

[release]\
fun [withAuthorizationHandler](with-authorization-handler.md)(authorizationHandler: [AuthorizationHandler](../../../-authorization-handler/index.md)): [OpenId4VciManager.Config.Builder](index.md)

Set the authorization handler for handling authorization requests. If not set, [BrowserAuthorizationHandler](../../../-browser-authorization-handler/index.md) will be used by default.

#### Return

this builder

#### Parameters

release

| | |
|---|---|
| authorizationHandler | the authorization handler |