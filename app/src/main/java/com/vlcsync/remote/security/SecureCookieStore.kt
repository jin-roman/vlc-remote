package com.vlcsync.remote.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureCookieStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_vlc_sessions",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCookies(deviceId: String, cookies: String) {
        prefs.edit().putString("cookies_$deviceId", cookies).apply()
    }

    fun getCookies(deviceId: String): String? {
        return prefs.getString("cookies_$deviceId", null)
    }

    fun clearSession(deviceId: String) {
        prefs.edit().remove("cookies_$deviceId").apply()
    }
}