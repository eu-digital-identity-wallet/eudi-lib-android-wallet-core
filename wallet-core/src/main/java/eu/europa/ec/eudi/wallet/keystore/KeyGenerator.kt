package eu.europa.ec.eudi.wallet.keystore

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import eu.europa.ec.eudi.wallet.keystore.KeyGenerator.SigningKeyConfig
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PrivateKey
import java.security.Signature
import java.security.SignatureException
import java.security.UnrecoverableEntryException
import java.security.cert.CertificateException

private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val DEV_KEY_ALIAS = "eudi_wallet_dev_key"
private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"

interface KeyGenerator {
    @RequiresApi(Build.VERSION_CODES.R)
    @Throws(KeyStoreException::class)
    fun getSigningKey(config: SigningKeyConfig): KeyStore.PrivateKeyEntry

    @Throws(SignatureException::class)
    fun sign(key: PrivateKey, data: ByteArray): String

    data class SigningKeyConfig(
        val keyType: Int,
        val timeoutSeconds: Int,
    )
}

internal object KeyGeneratorImpl : KeyGenerator {
    @RequiresApi(Build.VERSION_CODES.R)
    @Throws(KeyStoreException::class)
    override fun getSigningKey(config: SigningKeyConfig): KeyStore.PrivateKeyEntry {
        val entry = getKeyStoreEntry(config)
        if (entry !is KeyStore.PrivateKeyEntry) throw KeyStoreException("Entry not an instance of a PrivateKeyEntry.")
        return entry
    }

    @Throws(SignatureException::class)
   override fun sign(
        key: PrivateKey,
        data: ByteArray,
    ) = try {
        Signature
            .getInstance(SIGNATURE_ALGORITHM)
            .run {
                initSign(key)
                update(data)
                sign()
            }.toBase64String()
    } catch (exception: NoSuchAlgorithmException) {
        throw SignatureException(exception)
    } catch (exception: InvalidKeyException) {
        throw SignatureException(exception)
    }

    private fun ByteArray.toBase64String() = String(Base64.encode(this, Base64.DEFAULT))

    @RequiresApi(Build.VERSION_CODES.R)
    @Throws(KeyStoreException::class)
    private fun getKeyStoreEntry(config: SigningKeyConfig) = try {
        val keyStore = getKeyStore()
        keyStore.getEntry(DEV_KEY_ALIAS, null).let {
            if (it == null) {
                generateKey(config)
                keyStore.getEntry(DEV_KEY_ALIAS, null)!!
            } else {
                it
            }
        }
    } catch (exception: KeyStoreException) {
        throw KeyStoreException("Get KeyStore entry failed.", exception)
    } catch (exception: NoSuchAlgorithmException) {
        throw KeyStoreException("Get KeyStore entry failed.", exception)
    } catch (exception: UnrecoverableEntryException) {
        throw KeyStoreException("Get KeyStore entry failed.", exception)
    } catch (exception: NoSuchProviderException) {
        throw KeyStoreException("Get KeyStore entry failed.", exception)
    } catch (exception: InvalidAlgorithmParameterException) {
        throw KeyStoreException("Get KeyStore entry failed.", exception)
    }

    @Throws(KeyStoreException::class)
    fun getKeyStore(): KeyStore = try {
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    } catch (exception: KeyStoreException) {
        throw KeyStoreException("Get KeyStore instance failed.", exception)
    } catch (exception: CertificateException) {
        throw KeyStoreException("Get KeyStore instance failed.", exception)
    } catch (exception: IOException) {
        throw KeyStoreException("Get KeyStore instance failed.", exception)
    } catch (exception: NoSuchAlgorithmException) {
        throw KeyStoreException("Get KeyStore instance failed.", exception)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Throws(KeyStoreException::class)
    private fun generateKey(config: SigningKeyConfig) {
        val keyPairGenerator: KeyPairGenerator =
            try {
                KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC,
                    ANDROID_KEY_STORE
                )
            } catch (exception: NoSuchAlgorithmException) {
                throw KeyStoreException("Generate key failed.", exception)
            } catch (exception: NoSuchProviderException) {
                throw KeyStoreException("Generate key failed.", exception)
            }
        val parameterSpec: KeyGenParameterSpec =
            KeyGenParameterSpec
                .Builder(
                    DEV_KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN,
                ).run {
                    setUserAuthenticationParameters(config.timeoutSeconds, config.keyType)
                    setUserAuthenticationRequired(true)
                    setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    build()
                }

        try {
            keyPairGenerator.initialize(parameterSpec)
        } catch (exception: InvalidAlgorithmParameterException) {
            throw KeyStoreException("Generate key failed.", exception)
        }
        keyPairGenerator.generateKeyPair()
    }
}