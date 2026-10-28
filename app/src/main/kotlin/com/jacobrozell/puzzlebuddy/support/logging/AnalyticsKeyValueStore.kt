package com.jacobrozell.puzzlebuddy.support.logging

interface AnalyticsKeyValueStore {
    fun getLongOrNull(key: String): Long?
    fun putLong(key: String, value: Long)
    fun getStringSet(key: String): Set<String>
    fun putStringSet(key: String, values: Set<String>)
    fun getBoolean(key: String): Boolean
    fun putBoolean(key: String, value: Boolean)
}

class InMemoryAnalyticsKeyValueStore : AnalyticsKeyValueStore {
    private val longs = mutableMapOf<String, Long>()
    private val stringSets = mutableMapOf<String, Set<String>>()
    private val booleans = mutableMapOf<String, Boolean>()

    override fun getLongOrNull(key: String): Long? = longs[key]

    override fun putLong(key: String, value: Long) {
        longs[key] = value
    }

    override fun getStringSet(key: String): Set<String> = stringSets[key].orEmpty()

    override fun putStringSet(key: String, values: Set<String>) {
        stringSets[key] = values.toSet()
    }

    override fun getBoolean(key: String): Boolean = booleans[key] == true

    override fun putBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }
}

fun interface AnalyticsClock {
    fun nowMillis(): Long
}
