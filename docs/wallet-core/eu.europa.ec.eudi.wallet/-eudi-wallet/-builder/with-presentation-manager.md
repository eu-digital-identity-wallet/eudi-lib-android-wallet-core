//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)/[withPresentationManager](with-presentation-manager.md)

# withPresentationManager

[androidJvm]\
fun [withPresentationManager](with-presentation-manager.md)(presentationManager: [PresentationManager](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md)): &lt;Error class: unknown class&gt;

Configure with the given [PresentationManager](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager/index.md) to use for both proximity and remote presentation. If not set, the default presentation manager will be used which is [PresentationManagerImpl](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager-impl/index.md) that uses the eu.europa.ec.eudi.iso18013.transfer.TransferManagerImpl for proximity presentation and [OpenId4VpManager](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-manager/index.md) for remote presentation.

#### Return

this [Builder](index.md) instance

#### Parameters

androidJvm

| | |
|---|---|
| presentationManager | the presentation manager |
