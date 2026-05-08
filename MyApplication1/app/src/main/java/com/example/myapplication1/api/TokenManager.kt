package com.example.myapplication1

import android.content.Context
import android.content.SharedPreferences

object TokenManager {

    private const val PREF_NAME     = "auth_prefs"
    private const val KEY_ACCESS    = "access_token"
    private const val KEY_REFRESH   = "refresh_token"
    private const val KEY_USER_ID   = "user_id"
    private const val KEY_EMAIL     = "email"
    private const val KEY_NOM       = "nom"
    private const val KEY_PRENOM    = "prenom"
    private const val KEY_TELEPHONE = "telephone"
    private const val KEY_VILLE     = "ville"
    private const val KEY_ADRESSE   = "adresse"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ✅ Save from LoginResponse
    fun saveAuth(context: Context, response: LoginResponse) {
        prefs(context).edit().apply {
            putString(KEY_ACCESS,    response.access)
            putString(KEY_REFRESH,   response.refresh)
            putInt   (KEY_USER_ID,   response.client.id)
            putString(KEY_EMAIL,     response.client.email)
            putString(KEY_NOM,       response.client.nom)
            putString(KEY_PRENOM,    response.client.prenom)
            putString(KEY_TELEPHONE, response.client.telephone)
            putString(KEY_VILLE,     response.client.ville)
            putString(KEY_ADRESSE,   response.client.adresse)
            apply()
        }
    }

    // ✅ Save from RegisterResponse
    fun saveAuth(context: Context, response: RegisterResponse) {
        prefs(context).edit().apply {
            putString(KEY_ACCESS,  response.token)
            putInt   (KEY_USER_ID, response.userId ?: 0)
            apply()
        }
    }

    fun getAccessToken(context: Context): String? =
        prefs(context).getString(KEY_ACCESS, null)

    fun getRefreshToken(context: Context): String? =
        prefs(context).getString(KEY_REFRESH, null)

    fun getUserId(context: Context): Int =
        prefs(context).getInt(KEY_USER_ID, 0)

    fun getEmail(context: Context): String? =
        prefs(context).getString(KEY_EMAIL, null)

    fun getNom(context: Context): String? =
        prefs(context).getString(KEY_NOM, null)

    fun getPrenom(context: Context): String? =
        prefs(context).getString(KEY_PRENOM, null)

    fun getTelephone(context: Context): String? =
        prefs(context).getString(KEY_TELEPHONE, null)

    fun getVille(context: Context): String? =
        prefs(context).getString(KEY_VILLE, null)

    fun getAdresse(context: Context): String? =
        prefs(context).getString(KEY_ADRESSE, null)

    // ✅ Keep old names as aliases so nothing breaks
    fun getFirstName(context: Context): String? = getPrenom(context)
    fun getLastName(context: Context): String?  = getNom(context)

    fun getBearerToken(context: Context): String =
        "Bearer ${getAccessToken(context).orEmpty()}"

    fun isLoggedIn(context: Context): Boolean =
        getAccessToken(context) != null

    fun logout(context: Context) {
        prefs(context).edit().clear().apply()
    }
    private const val KEY_TROTTINETTE_ID = "trottinette_id"

    fun saveTrottinetteId(context: Context, id: Int) {
        prefs(context).edit().putInt(KEY_TROTTINETTE_ID, id).apply()
    }

    fun getTrottinetteId(context: Context): Int =
        prefs(context).getInt(KEY_TROTTINETTE_ID, 0)
}