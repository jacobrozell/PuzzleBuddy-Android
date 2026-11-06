package com.jacobrozell.puzzlebuddy.data.local

import com.jacobrozell.puzzlebuddy.support.logging.InMemoryAnalyticsKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoreNoticesTest {
    private fun notices(
        ephemeral: Boolean = false,
        resetPending: Boolean = false,
        underTest: Boolean = false,
        store: InMemoryAnalyticsKeyValueStore = InMemoryAnalyticsKeyValueStore(),
    ): StoreNotices {
        if (resetPending) store.putBoolean(StoreRuntime.STORE_WAS_RESET_NOTICE, true)
        return StoreNotices(store, isEphemeral = { ephemeral }, isUnderTest = { underTest })
    }

    @Test
    fun nothingByDefault() {
        assertNull(notices().notice.value)
    }

    @Test
    fun resetNoticeShowsOnceUntilDismissed() {
        val store = InMemoryAnalyticsKeyValueStore()
        val subject = notices(resetPending = true, store = store)
        assertEquals(StoreNotice.RESET, subject.notice.value)

        subject.dismiss(StoreNotice.RESET)

        assertNull(subject.notice.value)
        assertNull(notices(store = store).notice.value)
    }

    @Test
    fun ephemeralWarningWinsAndStaysDismissed() {
        val store = InMemoryAnalyticsKeyValueStore()
        val subject = notices(ephemeral = true, resetPending = true, store = store)
        assertEquals(StoreNotice.EPHEMERAL, subject.notice.value)

        subject.dismiss(StoreNotice.EPHEMERAL)

        assertEquals(StoreNotice.RESET, subject.notice.value)
        assertEquals(StoreNotice.RESET, notices(ephemeral = true, store = store).notice.value)
    }

    @Test
    fun hiddenUnderInstrumentedTests() {
        assertNull(notices(ephemeral = true, resetPending = true, underTest = true).notice.value)
    }
}
