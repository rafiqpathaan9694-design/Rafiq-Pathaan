package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.dataStore by preferencesDataStore(name = "mobile_solution_prefs")

data class AppSettingsState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val pinEnabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = ""
)

class SettingsPreferences(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
    }

    val settingsFlow: Flow<AppSettingsState> = context.dataStore.data.map { prefs ->
        AppSettingsState(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.ENGLISH.code),
            pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            pinSalt = prefs[Keys.PIN_SALT] ?: ""
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun setPin(pin: String) {
        val saltBytes = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
        val hash = hashPin(pin, saltHex)
        context.dataStore.edit { prefs ->
            prefs[Keys.PIN_ENABLED] = true
            prefs[Keys.PIN_SALT] = saltHex
            prefs[Keys.PIN_HASH] = hash
        }
    }

    suspend fun disablePin() {
        context.dataStore.edit { prefs ->
            prefs[Keys.PIN_ENABLED] = false
            prefs[Keys.PIN_HASH] = ""
            prefs[Keys.PIN_SALT] = ""
        }
    }

    fun verifyPin(pin: String, state: AppSettingsState): Boolean {
        if (!state.pinEnabled || state.pinHash.isBlank()) return true
        return hashPin(pin, state.pinSalt) == state.pinHash
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
