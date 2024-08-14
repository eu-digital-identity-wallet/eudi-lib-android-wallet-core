package eu.europa.ec.eudi.wallet.issue.openid4vci

import android.content.Context
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.util.getEncryptedSharedPreferences
import org.json.JSONException
import org.json.JSONObject
import java.util.Base64

object DocumentManagerSdJwt {
    private lateinit var dataStore: SdJwtDocumentDataStore

    fun init(context: Context, requiresUserAuth: Boolean) {
        dataStore = SdJwtDocumentDataStore(context, requiresUserAuth)
    }

    fun storeDocument(id: String, credentials: String) {
        dataStore.add(id, credentials)
    }

    fun getDocumentById(id: String) = dataStore.get(id)

    fun getAllDocuments() = dataStore.getAll()

    fun deleteDocument(documentId: DocumentId) {
        // quick but very dirty solution (we decided to only have one document at all times)
        deleteAllDocuments()
    }

    fun deleteAllDocuments() {
        dataStore.deleteAll()
    }

}

data class SdJwtDocument(
    val id: String,
    val vct: String,
    val docName: String,
    val requiresUserAuth: Boolean,
    val data: String,
)

private class SdJwtDocumentDataStore(
    context: Context,
    val requiresUserAuth: Boolean,
) {
    private var sharedPreferences = getEncryptedSharedPreferences(context, PREF_FILE_NAME)

    fun add(id: String, credentials: String) {
        sharedPreferences.edit().putString(id, credentials).apply()
    }

    fun get(id: String) = sharedPreferences.getString(id, null)?.toDocument(id, requiresUserAuth)

    fun getAll() = sharedPreferences.all.mapNotNull {
        (it.value as? String)?.toDocument(it.key, requiresUserAuth)
    }

    fun delete(id: String) {
        sharedPreferences.edit().remove(id).apply()
    }

    fun deleteAll() {
        sharedPreferences.edit().clear().apply()
    }

    private companion object {
        private const val PREF_FILE_NAME = "document_manager_sdjwt_prefs"
    }
}

private fun String.toDocument(
    id: String,
    requiresUserAuth: Boolean,
) = try {
    val payloadString = split(".")[1]
    val payloadJson = JSONObject(String(Base64.getUrlDecoder().decode(payloadString)))

    val vct = payloadJson.getString("vct")
    val docName = "Personalausweis"
    val data = payloadJson.toString()

    SdJwtDocument(
        id = id,
        vct = vct,
        docName = docName,
        requiresUserAuth = requiresUserAuth,
        data = this,
    )
} catch (_: JSONException) {
    null
}