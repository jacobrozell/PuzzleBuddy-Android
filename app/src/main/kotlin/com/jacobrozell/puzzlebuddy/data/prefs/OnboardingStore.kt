package com.jacobrozell.puzzlebuddy.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "puzzle_buddy_prefs")

@Singleton
class OnboardingStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val completeKey = booleanPreferencesKey("PuzzleBuddy.OnboardingComplete")
    private val legacyKey = booleanPreferencesKey("PuzzlePal_Onboarding_Complete")
    private val replayListeners = CopyOnWriteArrayList<() -> Unit>()

    val isCompleteFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[completeKey] == true || prefs[legacyKey] == true
    }

    val shouldPresentOnLaunch: Boolean
        get() = runBlocking { !isCompleteFlow.first() }

    suspend fun markComplete() {
        context.dataStore.edit { prefs ->
            prefs[completeKey] = true
        }
    }

    suspend fun resetForReplay() {
        context.dataStore.edit { prefs ->
            prefs.remove(completeKey)
            prefs.remove(legacyKey)
        }
    }

    fun requestReplay() {
        replayListeners.forEach { it.invoke() }
    }

    fun addReplayListener(listener: () -> Unit) {
        replayListeners += listener
    }

    fun removeReplayListener(listener: () -> Unit) {
        replayListeners -= listener
    }
}
