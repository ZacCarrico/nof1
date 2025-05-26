package com.nof1.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    companion object {
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val DEFAULT_BASE_URL = "https://api.openai.com/"
    }
    
    fun saveOpenAIApiKey(apiKey: String) {
        encryptedPrefs.edit()
            .putString(KEY_OPENAI_API_KEY, apiKey)
            .apply()
    }
    
    fun getOpenAIApiKey(): String? {
        return encryptedPrefs.getString(KEY_OPENAI_API_KEY, null)
    }
    
    fun removeOpenAIApiKey() {
        encryptedPrefs.edit()
            .remove(KEY_OPENAI_API_KEY)
            .apply()
    }
    
    fun hasOpenAIApiKey(): Boolean {
        return !getOpenAIApiKey().isNullOrBlank()
    }
    
    fun saveApiBaseUrl(baseUrl: String) {
        encryptedPrefs.edit()
            .putString(KEY_API_BASE_URL, baseUrl)
            .apply()
    }
    
    fun getApiBaseUrl(): String {
        return encryptedPrefs.getString(KEY_API_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }
}