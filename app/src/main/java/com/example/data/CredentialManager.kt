package com.example.data

import android.content.Context
import android.content.SharedPreferences

data class UserCredentials(
    val phone: String = "8487086679",
    val pass: String = "Kp87086679",
    val isAutoLoginEnabled: Boolean = true,
    val loginUrl: String = "https://dmfirst1.com/#/login",
    val homeUrl: String = "https://dmfirst1.com/#/"
)

class CredentialManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dm_portal_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PHONE = "key_phone"
        private const val KEY_PASSWORD = "key_password"
        private const val KEY_AUTO_LOGIN = "key_auto_login"
        private const val KEY_LOGIN_URL = "key_login_url"
        private const val KEY_HOME_URL = "key_home_url"

        const val DEFAULT_PHONE = "8487086679"
        const val DEFAULT_PASS = "Kp87086679"
        const val DEFAULT_LOGIN_URL = "https://dmfirst1.com/#/login"
        const val DEFAULT_HOME_URL = "https://dmfirst1.com/#/"
    }

    fun getCredentials(): UserCredentials {
        return UserCredentials(
            phone = prefs.getString(KEY_PHONE, DEFAULT_PHONE) ?: DEFAULT_PHONE,
            pass = prefs.getString(KEY_PASSWORD, DEFAULT_PASS) ?: DEFAULT_PASS,
            isAutoLoginEnabled = prefs.getBoolean(KEY_AUTO_LOGIN, true),
            loginUrl = prefs.getString(KEY_LOGIN_URL, DEFAULT_LOGIN_URL) ?: DEFAULT_LOGIN_URL,
            homeUrl = prefs.getString(KEY_HOME_URL, DEFAULT_HOME_URL) ?: DEFAULT_HOME_URL
        )
    }

    fun saveCredentials(credentials: UserCredentials) {
        prefs.edit()
            .putString(KEY_PHONE, credentials.phone)
            .putString(KEY_PASSWORD, credentials.pass)
            .putBoolean(KEY_AUTO_LOGIN, credentials.isAutoLoginEnabled)
            .putString(KEY_LOGIN_URL, credentials.loginUrl)
            .putString(KEY_HOME_URL, credentials.homeUrl)
            .apply()
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
