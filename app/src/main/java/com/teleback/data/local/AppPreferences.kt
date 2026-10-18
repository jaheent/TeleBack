package com.teleback.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.teleback.core.crypto.CryptoEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "teleback_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_BOT_TOKEN = stringPreferencesKey("telegram_bot_token")
        val KEY_CHAT_ID = longPreferencesKey("telegram_chat_id")
        val KEY_CHAT_TITLE = stringPreferencesKey("telegram_chat_title")
        val KEY_FOLDER_URI = stringPreferencesKey("target_folder_uri")
        val KEY_FOLDER_NAME = stringPreferencesKey("target_folder_name")
        val KEY_MASTER_KEY = stringPreferencesKey("master_encryption_key")
        val KEY_ENCRYPTION_ENABLED = booleanPreferencesKey("encryption_enabled")
        val KEY_AUTO_SYNC = booleanPreferencesKey("auto_sync_enabled")
        val KEY_SYNC_INTERVAL = intPreferencesKey("sync_interval_hours")
        val KEY_HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        val KEY_THEME_ID = stringPreferencesKey("app_theme_id")
        val KEY_ACTIVE_ICON_ID = stringPreferencesKey("app_active_icon_id")
    }

    val hasSeenWelcome: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAS_SEEN_WELCOME] ?: false }
    val selectedThemeId: Flow<String> = context.dataStore.data.map { it[KEY_THEME_ID] ?: "matrix_mint" }
    val selectedIconId: Flow<String> = context.dataStore.data.map { it[KEY_ACTIVE_ICON_ID] ?: "icon_a" }
    val botToken: Flow<String?> = context.dataStore.data.map { it[KEY_BOT_TOKEN] }
    val chatId: Flow<Long?> = context.dataStore.data.map { it[KEY_CHAT_ID] }
    val chatTitle: Flow<String?> = context.dataStore.data.map { it[KEY_CHAT_TITLE] }
    val folderUri: Flow<String?> = context.dataStore.data.map { it[KEY_FOLDER_URI] }
    val folderName: Flow<String?> = context.dataStore.data.map { it[KEY_FOLDER_NAME] }
    val isEncryptionEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_ENCRYPTION_ENABLED] ?: true }
    val masterKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MASTER_KEY] ?: run {
            // Auto-generate key if not present
            val generated = CryptoEngine.generateMasterKeyBase64()
            saveMasterKey(generated)
            generated
        }
    }

    suspend fun saveBotToken(token: String) {
        context.dataStore.edit { it[KEY_BOT_TOKEN] = token.trim() }
    }

    suspend fun saveChat(id: Long, title: String?) {
        context.dataStore.edit {
            it[KEY_CHAT_ID] = id
            if (title != null) it[KEY_CHAT_TITLE] = title
        }
    }

    suspend fun saveFolder(uriString: String, name: String) {
        context.dataStore.edit {
            it[KEY_FOLDER_URI] = uriString
            it[KEY_FOLDER_NAME] = name
        }
    }

    suspend fun saveMasterKey(keyBase64: String) {
        context.dataStore.edit { it[KEY_MASTER_KEY] = keyBase64.trim() }
    }

    suspend fun setEncryptionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENCRYPTION_ENABLED] = enabled }
    }

    suspend fun setHasSeenWelcome(seen: Boolean = true) {
        context.dataStore.edit { it[KEY_HAS_SEEN_WELCOME] = seen }
    }

    suspend fun saveThemeId(themeId: String) {
        context.dataStore.edit { it[KEY_THEME_ID] = themeId }
    }

    suspend fun saveIconId(iconId: String) {
        context.dataStore.edit { it[KEY_ACTIVE_ICON_ID] = iconId }
    }

    suspend fun getOrGenerateMasterKey(): String {
        val existing = context.dataStore.data.first()[KEY_MASTER_KEY]
        if (!existing.isNullOrBlank()) return existing
        val newKey = CryptoEngine.generateMasterKeyBase64()
        saveMasterKey(newKey)
        return newKey
    }
}
