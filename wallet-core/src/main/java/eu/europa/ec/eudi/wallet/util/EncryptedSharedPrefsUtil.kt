package eu.europa.ec.eudi.wallet.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.AEADBadTagException

@Throws(java.security.GeneralSecurityException::class, java.io.IOException::class)
fun getEncryptedSharedPreferences(context: Context, name: String): SharedPreferences {
    val masterKey: MasterKey = MasterKey
        .Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return try {
        createEncryptedSharedPreferences(context, masterKey, name)
    } catch (e: AEADBadTagException) {
        clearEncryptedSharedPreferences(context, name)
        createEncryptedSharedPreferences(context, masterKey, name)
    }
}

@Throws(java.security.GeneralSecurityException::class, java.io.IOException::class)
private fun createEncryptedSharedPreferences(
    context: Context,
    masterKey: MasterKey,
    name: String,
) = EncryptedSharedPreferences.create(
    context,
    name,
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

private fun clearEncryptedSharedPreferences(context: Context, name: String) {
    context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
}