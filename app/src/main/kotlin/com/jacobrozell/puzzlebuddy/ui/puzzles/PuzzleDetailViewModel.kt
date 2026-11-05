package com.jacobrozell.puzzlebuddy.ui.puzzles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

sealed interface PuzzleDetailContent {
    data object Loading : PuzzleDetailContent
    data object Missing : PuzzleDetailContent
    data class Loaded(val puzzle: Puzzle) : PuzzleDetailContent
}

data class DetailError(val title: String, val message: String)

data class PuzzleDetailUiState(
    val content: PuzzleDetailContent = PuzzleDetailContent.Loading,
    val error: DetailError? = null,
)

/** Follows the stored puzzle, so edits saved elsewhere show up here without a reload. */
@HiltViewModel
class PuzzleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val repository: PuzzleRepository,
    private val clock: Clock,
) : ViewModel() {
    private val puzzleId: String = savedStateHandle.get<String>("id").orEmpty()
    private val error = MutableStateFlow<DetailError?>(null)

    val uiState: StateFlow<PuzzleDetailUiState> = combine(repository.observePuzzles(), error) { puzzles, error ->
        val puzzle = puzzles.firstOrNull { it.id == puzzleId }
        val content = if (puzzle != null) PuzzleDetailContent.Loaded(puzzle) else PuzzleDetailContent.Missing
        PuzzleDetailUiState(content = content, error = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PuzzleDetailUiState())

    suspend fun photoBytes(photo: PuzzlePhoto): ByteArray? = repository.photoData(photo)

    fun dismissError() {
        error.value = null
    }

    fun commitProgress(puzzle: Puzzle, percent: Int) = perform("Could not save progress") {
        val clamped = PuzzleProgressSemantics.clamped(percent)
        val status = PuzzleProgressSemantics.statusFor(clamped)
        repository.update(
            puzzle.withStatusChange(puzzle.status, status, Instant.now(clock)).copy(progressPercent = clamped),
        )
    }

    fun puzzleAgain() = perform("Could not start again") { repository.startRedo(puzzleId) }

    fun markReturned() = perform("Could not mark returned") { repository.markReturned(puzzleId) }

    fun delete(onDeleted: () -> Unit) = perform("Could not delete puzzle") {
        repository.delete(puzzleId)
        onDeleted()
    }

    private fun perform(title: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (failure: Exception) {
                error.value = DetailError(title, failure.message ?: "Try again.")
            }
        }
    }
}
