package com.agenticbank.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("agentic_bank_session", Context.MODE_PRIVATE)

    companion object {
        const val KEY_TOKEN = "jwt_token"
        const val KEY_USERNAME = "username"
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_USER_ID = "user_id"
        const val KEY_ROLE = "role"
    }

    fun saveSession(token: String, username: String, accountId: Long?, userId: Long?, role: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USERNAME, username)
            putLong(KEY_ACCOUNT_ID, accountId ?: -1L)
            putLong(KEY_USER_ID, userId ?: -1L)
            putString(KEY_ROLE, role)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getAccountId(): Long? {
        val id = prefs.getLong(KEY_ACCOUNT_ID, -1L)
        return if (id == -1L) null else id
    }

    fun getUserId(): Long? {
        val id = prefs.getLong(KEY_USER_ID, -1L)
        return if (id == -1L) null else id
    }

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
