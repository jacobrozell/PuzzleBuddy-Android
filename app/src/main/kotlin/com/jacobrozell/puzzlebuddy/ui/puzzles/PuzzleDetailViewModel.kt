package com.jacobrozell.puzzlebuddy.ui.puzzles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleDetailMetrics
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PuzzleDetailUiState(
    val puzzle: Puzzle? = null,
    val imageData: ByteArray? = null,
    val metrics: PuzzleDetailMetrics = PuzzleDetailMetrics(null, null),
    val isLoading: Boolean = true,
)

@HiltViewModel
class PuzzleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val repository: PuzzleRepository,
) : ViewModel() {
    private val puzzleId: String = savedStateHandle.get<String>("id").orEmpty()
    private val _uiState = MutableStateFlow(PuzzleDetailUiState())
    val uiState: StateFlow<PuzzleDetailUiState> = _uiState.asStateFlow()

    init {
        if (puzzleId.isNotEmpty()) {
            load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val puzzle = repository.findById(puzzleId)
            val image = repository.imageDataFor(puzzleId)
            _uiState.value = PuzzleDetailUiState(
                puzzle = puzzle,
                imageData = image,
                metrics = PuzzleDetailMetrics.compute(puzzle?.pieces, puzzle?.estimatedTimeSpent),
                isLoading = false,
            )
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(puzzleId)
            onDeleted()
        }
    }
}
