package com.example.data.auth

import android.content.Context

class AuthSessionManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveSession(userId: String): Long {
        val now = System.currentTimeMillis()
        val expiresAt = now + SESSION_DURATION_MILLIS
        prefs.edit()
            .putString(KEY_ACTIVE_USER_ID, userId)
            .putLong(KEY_LOGGED_IN_AT, now)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
        return expiresAt
    }

    fun getValidSession(): SessionInfo? {
        val userId = prefs.getString(KEY_ACTIVE_USER_ID, null)?.trim()
        if (userId.isNullOrEmpty()) return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val loggedInAt = prefs.getLong(KEY_LOGGED_IN_AT, 0L)
        val now = System.currentTimeMillis()
        if (expiresAt <= now) {
            clearSession()
            return null
        }
        return SessionInfo(
            userId = userId,
            loggedInAtMillis = loggedInAt,
            expiresAtMillis = expiresAt
        )
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACTIVE_USER_ID)
            .remove(KEY_LOGGED_IN_AT)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    data class SessionInfo(
        val userId: String,
        val loggedInAtMillis: Long,
        val expiresAtMillis: Long
    )

    companion object {
        private const val PREFS_NAME = "medtime_auth_session"
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_LOGGED_IN_AT = "logged_in_at_millis"
        private const val KEY_EXPIRES_AT = "session_expires_at_millis"
        const val SESSION_DURATION_DAYS = 90L
        const val SESSION_DURATION_MILLIS = SESSION_DURATION_DAYS * 24L * 60L * 60L * 1000L
    }
}
