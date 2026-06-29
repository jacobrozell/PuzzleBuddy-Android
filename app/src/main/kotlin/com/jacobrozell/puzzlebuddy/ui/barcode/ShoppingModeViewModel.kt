package com.jacobrozell.puzzlebuddy.ui.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.catalog.BarcodeValidator
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.info
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingModeViewModel @Inject constructor(
    private val repository: PuzzleRepository,
    private val logger: AppLogger,
) : ViewModel() {
    private val _scanResult = MutableStateFlow<ShoppingScanResult?>(null)
    val scanResult: StateFlow<ShoppingScanResult?> = _scanResult.asStateFlow()

    fun handleScan(raw: String) {
        if (_scanResult.value != null) return
        viewModelScope.launch {
            val normalized = BarcodeValidator.normalizeOrNull(raw) ?: return@launch
            val match = repository.findByBarcode(normalized)
            _scanResult.value = if (match != null) {
                logger.info(
                    LogCategory.PUZZLES,
                    eventName = "shopping_scan_match",
                    message = "Duplicate found while shopping.",
                    metadata = mapOf("source" to "shopping_mode"),
                )
                ShoppingScanResult.Match(match)
            } else {
                logger.info(
                    LogCategory.PUZZLES,
                    eventName = "shopping_scan_no_match",
                    message = "No duplicate while shopping.",
                    metadata = mapOf("source" to "shopping_mode"),
                )
                ShoppingScanResult.NoMatch(normalized)
            }
        }
    }

    fun clearResult() {
        _scanResult.value = null
    }
}
