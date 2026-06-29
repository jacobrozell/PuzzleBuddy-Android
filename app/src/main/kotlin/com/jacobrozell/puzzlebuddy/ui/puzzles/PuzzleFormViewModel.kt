package com.jacobrozell.puzzlebuddy.ui.puzzles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagIndex
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import com.jacobrozell.puzzlebuddy.support.logging.PuzzleAddSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class PuzzleFormUiState(
    val puzzle: Puzzle = Puzzle(),
    val imageData: ByteArray? = null,
    val isLoading: Boolean = false,
    val canSave: Boolean = false,
    val tagCatalog: List<String> = emptyList(),
    val lookupNotice: String? = null,
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = puzzle.id.hashCode()
}

@HiltViewModel
class PuzzleFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val repository: PuzzleRepository,
) : ViewModel() {
    private val puzzleId: String? = savedStateHandle.get<String>("id")?.takeIf { it != "new" }
    private val initialBarcode: String? = savedStateHandle.get<String>("barcode")
        ?.takeIf { it.isNotBlank() }
    private val initialName: String? = savedStateHandle.get<String>("name")?.takeIf { it.isNotBlank() }
    private val initialPieces: Int? = savedStateHandle.get<String>("pieces")?.toIntOrNull()
    private val initialSource: String? = savedStateHandle.get<String>("source")?.takeIf { it.isNotBlank() }
    private val initialLookupNotice: String? = savedStateHandle.get<String>("lookup_notice")?.takeIf { it.isNotBlank() }
    private val _uiState = MutableStateFlow(PuzzleFormUiState())
    val uiState: StateFlow<PuzzleFormUiState> = _uiState.asStateFlow()

    val tagCatalog: StateFlow<List<String>> = repository.observePuzzles()
        .map { puzzles -> PuzzleTagIndex.allTagNames(puzzles) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (puzzleId == null && initialBarcode != null) {
            _uiState.value = PuzzleFormUiState(
                puzzle = Puzzle(
                    barcode = initialBarcode,
                    name = initialName.orEmpty(),
                    pieces = initialPieces,
                    source = initialSource,
                ),
                canSave = !initialName.isNullOrBlank(),
                lookupNotice = initialLookupNotice,
            )
        }
        if (puzzleId != null) {
            viewModelScope.launch {
                val puzzle = repository.findById(puzzleId)
                val image = repository.imageDataFor(puzzleId)
                if (puzzle != null) {
                    _uiState.value = PuzzleFormUiState(
                        puzzle = puzzle,
                        imageData = image,
                        canSave = puzzle.name.trim().isNotEmpty(),
                    )
                }
            }
        }
        viewModelScope.launch {
            tagCatalog.collect { catalog ->
                _uiState.value = _uiState.value.copy(tagCatalog = catalog)
            }
        }
    }

    fun dismissLookupNotice() {
        _uiState.value = _uiState.value.copy(lookupNotice = null)
    }

    fun updateName(name: String) = update { it.copy(name = name) }
    fun updatePieces(pieces: Int?) = update { it.copy(pieces = pieces) }
    fun updateRating(rating: PuzzleRating) = update { it.copy(rating = rating) }
    fun updateDifficulty(difficulty: PuzzleDifficulty) = update { it.copy(difficulty = difficulty) }
    fun updateStatus(status: PuzzleStatus) = update {
        it.copy(
            status = status,
            progressPercent = PuzzleProgressSemantics.progressFor(status, it.progressPercent),
        )
    }
    fun updateProgress(percent: Int) = update {
        val clamped = PuzzleProgressSemantics.clamped(percent)
        it.copy(
            progressPercent = clamped,
            status = PuzzleProgressSemantics.statusFor(clamped),
        )
    }
    fun updateHours(hours: Int?) = update {
        it.copy(estimatedTimeSpent = (it.estimatedTimeSpent ?: PuzzleTime()).copy(hours = hours))
    }
    fun updateMinutes(minutes: Int?) = update {
        it.copy(estimatedTimeSpent = (it.estimatedTimeSpent ?: PuzzleTime()).copy(minutes = minutes))
    }
    fun updateCompletionDate(instant: Instant) = update { it.copy(completionDate = instant) }
    fun updateNotes(notes: String) = update { it.copy(notes = notes.trim().takeIf { text -> text.isNotEmpty() }) }
    fun updateSource(source: String) = update { it.copy(source = source.trim().takeIf { text -> text.isNotEmpty() }) }
    fun updateBarcode(barcode: String) = update {
        it.copy(barcode = barcode.trim().takeIf { text -> text.isNotEmpty() })
    }
    fun updateHasMissingPieces(value: Boolean) = update { it.copy(hasMissingPieces = value) }
    fun updateTags(tags: List<String>) = update { it.copy(tags = tags) }
    fun updateImage(imageData: ByteArray?) {
        _uiState.value = _uiState.value.copy(imageData = imageData)
    }

    private fun update(transform: (Puzzle) -> Puzzle) {
        val current = _uiState.value
        val puzzle = transform(current.puzzle)
        _uiState.value = current.copy(
            puzzle = puzzle,
            canSave = puzzle.name.trim().isNotEmpty(),
        )
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            repository.upsert(
                puzzle = state.puzzle.copy(hasImage = state.imageData != null),
                imageData = state.imageData,
                addSource = if (initialBarcode != null && puzzleId == null) {
                    PuzzleAddSource.BARCODE
                } else {
                    PuzzleAddSource.MANUAL
                },
            )
            onSaved()
        }
    }
}
