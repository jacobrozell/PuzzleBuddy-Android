package com.jacobrozell.puzzlebuddy.ui.barcode

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.catalog.BarcodeValidator
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.info
import com.jacobrozell.puzzlebuddy.support.review.StoreReviewPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class ShoppingModeViewModel @Inject constructor(
    private val repository: PuzzleRepository,
    private val logger: AppLogger,
    private val reviewPrompt: StoreReviewPrompt,
) : ViewModel() {
    private val _state = MutableStateFlow(ShoppingModeState())
    val state: StateFlow<ShoppingModeState> = _state.asStateFlow()
    private val gate = AtomicBoolean(false)
    private var session = 0

    /**
     * The dialog does not own this ViewModel, so a previous result would still be on screen
     * the next time Check duplicate opens. Call this before showing the sheet and when it closes.
     */
    fun beginSession() {
        session += 1
        gate.set(false)
        _state.value = ShoppingModeState()
    }

    /** False when the value is not a barcode yet, so the camera can keep looking. */
    fun handleScan(raw: String, activity: Activity?): Boolean {
        if (!gate.compareAndSet(false, true)) return false
        val normalized = BarcodeValidator.normalizeOrNull(raw) ?: run {
            gate.set(false)
            return false
        }
        val sessionAtScan = session
        _state.update { it.copy(busy = true, pendingBarcode = normalized) }
        viewModelScope.launch {
            val match = repository.findByBarcode(normalized)
            if (session != sessionAtScan) return@launch
            val next = ShoppingScanSession.record(_state.value, normalized, match)
            val outcome = if (match != null) "match" else "no_match"
            logger.info(
                LogCategory.PUZZLES,
                eventName = if (match != null) "shopping_scan_match" else "shopping_scan_no_match",
                message = if (match != null) "Duplicate found while shopping." else "No duplicate while shopping.",
            )
            logger.info(
                LogCategory.PUZZLES,
                eventName = "barcode_scan_completed",
                message = "Barcode scan completed.",
                metadata = mapOf(
                    "scan_context" to "shopping",
                    "scan_result" to outcome,
                ),
            )
            if (activity != null) reviewPrompt.requestIfEligible(activity, StoreReviewPrompt.Reason.BARCODE_SCAN)
            _state.value = next
        }
        return true
    }

    fun clearResult() {
        gate.set(false)
        _state.update { it.copy(result = null, busy = false, pendingBarcode = null) }
    }
}

internal object ShoppingScanSession {
    fun record(state: ShoppingModeState, normalized: String, match: Puzzle?): ShoppingModeState {
        val result = if (match != null) {
            ShoppingScanResult.Match(match, normalized)
        } else {
            ShoppingScanResult.NoMatch(normalized)
        }
        return state.copy(
            result = result,
            busy = false,
            pendingBarcode = null,
            scansTotal = state.scansTotal + 1,
            duplicatesFound = state.duplicatesFound + if (match != null) 1 else 0,
        )
    }
}

data class ShoppingModeState(
    val result: ShoppingScanResult? = null,
    val busy: Boolean = false,
    val pendingBarcode: String? = null,
    val scansTotal: Int = 0,
    val duplicatesFound: Int = 0,
)
