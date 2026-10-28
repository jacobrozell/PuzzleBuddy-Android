package com.jacobrozell.puzzlebuddy.support.logging

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesAnalyticsStore(
    private val prefs: SharedPreferences,
) : AnalyticsKeyValueStore {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    override fun getLongOrNull(key: String): Long? =
        if (prefs.contains(key)) prefs.getLong(key, 0L) else null

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getStringSet(key: String): Set<String> =
        prefs.getStringSet(key, emptySet())?.toSet().orEmpty()

    override fun putStringSet(key: String, values: Set<String>) {
        prefs.edit().putStringSet(key, values).apply()
    }

    override fun getBoolean(key: String): Boolean = prefs.getBoolean(key, false)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    companion object {
        const val PREFS_NAME = "puzzle_buddy_analytics"
    }
}
