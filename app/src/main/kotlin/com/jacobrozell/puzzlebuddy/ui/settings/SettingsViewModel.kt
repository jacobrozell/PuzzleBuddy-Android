package com.jacobrozell.puzzlebuddy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.puzzlebuddy.data.prefs.AppPreferencesStore
import com.jacobrozell.puzzlebuddy.data.prefs.OnboardingStore
import com.jacobrozell.puzzlebuddy.data.repository.FriendRepository
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsError(val title: String, val message: String)

data class SettingsUiState(
    val puzzleCount: Int = 0,
    val demoCount: Int = 0,
    val friendCount: Int = 0,
    val appearanceMode: String = "system",
    val isBusy: Boolean = false,
    val error: SettingsError? = null,
) {
    /** iOS disables Delete All Data only when there are no puzzles and no borrower names. */
    val hasDataToDelete: Boolean get() = puzzleCount > 0 || friendCount > 0
}

/** Settings from iOS 1.1.0 `SettingsView`. Import / export UI was removed there and here. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PuzzleRepository,
    friendRepository: FriendRepository,
    private val appPreferences: AppPreferencesStore,
    private val onboardingStore: OnboardingStore,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<SettingsError?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.observePuzzles(),
        friendRepository.observeFriends(),
        appPreferences.appearanceMode,
        busy,
        error,
    ) { puzzles, friends, appearance, isBusy, error ->
        SettingsUiState(
            puzzleCount = puzzles.size,
            demoCount = puzzles.count { it.isDemo },
            friendCount = friends.size,
            appearanceMode = appearance,
            isBusy = isBusy,
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setAppearance(mode: String) {
        viewModelScope.launch { appPreferences.setAppearanceMode(mode) }
    }

    fun replayOnboarding() {
        onboardingStore.requestReplay()
    }

    fun loadDemoData() = perform("Could not load demo data") { repository.loadDemoPuzzles() }

    fun removeDemoData() = perform("Could not remove demo data") { repository.removeDemoPuzzles() }

    fun deleteAllData() = perform("Could not delete data") { repository.clearAll() }

    fun dismissError() {
        error.value = null
    }

    private fun perform(title: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            try {
                block()
            } catch (failure: Exception) {
                error.value = SettingsError(title, failure.message ?: "Try again.")
            } finally {
                busy.value = false
            }
        }
    }
}
