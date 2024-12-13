//[wallet-core](../../../../index.md)/[eu.europa.ec.eudi.wallet](../../index.md)/[EudiWallet](../index.md)/[Builder](index.md)/[build](build.md)

# build

[androidJvm]\
fun [build](build.md)(): [EudiWallet](../index.md)

Build the [EudiWallet](../index.md) instance

The [EudiWallet](../index.md) instance will be created based on the configuration provided in the [Builder](index.md) class.

The [EudiWallet](../index.md) instance will be created with the following default implementations if not set:

- 
   AndroidStorageEngine for storing/retrieving documents
- 
   AndroidKeystoreSecureArea for managing documents' keys
- 
   DocumentManagerImpl for managing documents
- 
   [PresentationManagerImpl](../../../eu.europa.ec.eudi.wallet.presentation/-presentation-manager-impl/index.md) for both proximity and remote presentation
- 
   [OpenId4VpManager](../../../eu.europa.ec.eudi.wallet.transfer.openId4vp/-open-id4-vp-manager/index.md) for remote presentation
- 
   TransferManagerImpl for proximity presentation

**Note**: The [EudiWalletConfig.documentsStorageDir](../../-eudi-wallet-config/documents-storage-dir.md) is not set, the default storage directory will be used which is the application's no backup files directory.

**Note**: The [EudiWalletConfig.userAuthenticationRequired](../../-eudi-wallet-config/user-authentication-required.md) is set to true and the device is not secured with a PIN, pattern, or password, the configuration will be updated to set the user authentication required to false.

**Note**: The [EudiWalletConfig.useStrongBoxForKeys](../../-eudi-wallet-config/use-strong-box-for-keys.md) is set to true and the device does not support StrongBox, the configuration will be updated to set the use StrongBox for keys to false.

#### Return

[EudiWallet](../index.md)
