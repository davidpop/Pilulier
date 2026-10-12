package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pilulier_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SNOOZE_DURATION_MINUTES = intPreferencesKey("snooze_duration_minutes")
        val APP_THEME = stringPreferencesKey("app_theme")
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
    }

    val snoozeMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SNOOZE_DURATION_MINUTES] ?: 10
    }

    val appTheme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_THEME] ?: "SYSTEM"
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SNOOZE_DURATION_MINUTES] = minutes
        }
    }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME] = theme
        }
    }
}
