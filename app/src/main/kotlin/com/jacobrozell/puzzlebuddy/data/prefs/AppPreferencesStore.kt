package com.jacobrozell.puzzlebuddy.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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

    val appearanceMode: Flow<String> = context.appPrefs.data.map { prefs ->
        prefs[appearanceKey] ?: "system"
    }

    suspend fun setAppearanceMode(mode: String) {
        context.appPrefs.edit { prefs ->
            prefs[appearanceKey] = mode
        }
    }
}
