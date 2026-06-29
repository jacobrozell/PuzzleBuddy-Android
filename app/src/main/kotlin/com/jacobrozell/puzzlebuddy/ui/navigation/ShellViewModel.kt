package com.jacobrozell.puzzlebuddy.ui.navigation

import androidx.lifecycle.ViewModel
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.info
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val logger: AppLogger,
) : ViewModel() {
    private var lastTab: String? = null

    fun onTabSelected(tab: String) {
        if (lastTab == tab) return
        lastTab = tab
        logger.info(
            LogCategory.UI,
            eventName = "tab_selected",
            message = "Tab selected.",
            metadata = mapOf("tab" to tab),
        )
    }
}
