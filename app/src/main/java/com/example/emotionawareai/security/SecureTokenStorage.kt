package com.example.emotionawareai.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure key-value store for sensitive credentials (e.g. HuggingFace access tokens).
 *
 * All values are encrypted at rest using [EncryptedSharedPreferences]:
 *  - **Keys**   — AES-256-SIV (deterministic, so key lookups still work)
 *  - **Values** — AES-256-GCM (authenticated encryption)
 *
 * The master key is stored in the **Android Keystore** system and never leaves
 * secure hardware on supported devices. The encrypted file lives in
 * `<app-private data>/shared_prefs/secure_tokens.xml` and is only accessible
 * to this app's process.
 *
 * Uses the `security-crypto:1.0.0` stable API: [MasterKeys.getOrCreate] returns
 * a String key alias backed by the Keystore; this alias is passed to
 * [EncryptedSharedPreferences.create] as the second argument.
 *
 * If [EncryptedSharedPreferences] fails to initialise (e.g. a corrupt keystore
 * on a factory-reset device), the class falls back to a plain in-memory map so
 * the app keeps working — tokens entered in that session will not persist until
 * the issue is resolved by the OS.
 */
@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Lazily-initialised encrypted prefs.
     * Failures are caught so a bad Keystore state does not crash the whole app.
     */
    private val prefs: SharedPreferences? by lazy {
        try {
            // MasterKeys.getOrCreate returns a String alias backed by Android Keystore.
            // The underlying key is AES-256-GCM and is created once, then reused.
            val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            // security-crypto 1.0.0 parameter order:
            //   create(fileName, masterKeyAlias, context, keyScheme, valueScheme)
            EncryptedSharedPreferences.create(
                PREFS_FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedSharedPreferences init failed — tokens will not persist this session", e)
            null
        }
    }

    /** In-memory fallback when encrypted prefs are unavailable. */
    private val memoryFallback = mutableMapOf<String, String>()

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns the saved HuggingFace access token, or an empty string if none is set. */
    fun getHuggingFaceToken(): String =
        (prefs?.getString(KEY_HF_TOKEN, "") ?: memoryFallback[KEY_HF_TOKEN]) ?: ""

    /**
     * Persists the HuggingFace access token securely.
     * Pass an empty string (or call [clearHuggingFaceToken]) to remove it.
     */
    fun setHuggingFaceToken(token: String) {
        val trimmed = token.trim()
        Log.i(TAG, "HuggingFace token ${if (trimmed.isBlank()) "cleared" else "saved (${trimmed.length} chars)"}")
        if (prefs != null) {
            prefs!!.edit().putString(KEY_HF_TOKEN, trimmed).apply()
        } else {
            if (trimmed.isBlank()) memoryFallback.remove(KEY_HF_TOKEN)
            else memoryFallback[KEY_HF_TOKEN] = trimmed
        }
    }

    /** Removes the stored HuggingFace token. */
    fun clearHuggingFaceToken() = setHuggingFaceToken("")

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "SecureTokenStorage"
        /**
         * Name of the encrypted SharedPreferences file.
         * Changing this value would orphan any previously saved tokens.
         */
        private const val PREFS_FILE_NAME = "secure_tokens"
        private const val KEY_HF_TOKEN = "hf_token"
    }
}
