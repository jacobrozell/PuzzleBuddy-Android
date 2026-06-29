package com.jacobrozell.puzzlebuddy.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPrefs: DataStore<Preferences> by preferencesDataStore(name = "puzzle_buddy_app")

@Singleton
class AppPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val appearanceKey = stringPreferencesKey("appearance_mode")
    private val barcodeLookupKey = booleanPreferencesKey("barcode_lookup_enabled")

    val appearanceMode: Flow<String> = context.appPrefs.data.map { prefs ->
        prefs[appearanceKey] ?: "system"
    }

    val isBarcodeLookupEnabled: Flow<Boolean> = context.appPrefs.data.map { prefs ->
        prefs[barcodeLookupKey] ?: false
    }

    suspend fun setAppearanceMode(mode: String) {
        context.appPrefs.edit { prefs ->
            prefs[appearanceKey] = mode
        }
    }

    suspend fun setBarcodeLookupEnabled(enabled: Boolean) {
        context.appPrefs.edit { prefs ->
            prefs[barcodeLookupKey] = enabled
        }
    }
}
