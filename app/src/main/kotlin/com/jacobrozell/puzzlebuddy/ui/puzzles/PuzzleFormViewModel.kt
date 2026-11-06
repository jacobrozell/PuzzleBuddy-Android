package com.jacobrozell.puzzlebuddy.ui.puzzles

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.puzzlebuddy.data.barcode.BarcodeMetadataCache
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleStoreException
import com.jacobrozell.puzzlebuddy.domain.barcode.BarcodeProductMetadata
import com.jacobrozell.puzzlebuddy.domain.catalog.PurchasePriceFormatting
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleDuplicateChecker
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleSimilarMatchFinder
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagIndex
import com.jacobrozell.puzzlebuddy.domain.loan.PuzzleLoanSemantics
import com.jacobrozell.puzzlebuddy.domain.model.BarcodeNormalizer
import com.jacobrozell.puzzlebuddy.support.review.StoreReviewPrompt
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleCutType
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleMaterial
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleShape
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleTime
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import com.jacobrozell.puzzlebuddy.domain.photo.MAX_PHOTOS
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhotoSemantics
import com.jacobrozell.puzzlebuddy.support.logging.PuzzleAddSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

data class PuzzleFormUiState(
    val puzzle: Puzzle = Puzzle(),
    /** Photo bytes not yet written to disk, keyed by [PuzzlePhoto.id]. */
    val pendingPhotoBytes: Map<String, ByteArray> = emptyMap(),
    /** Price as typed, so "12." does not jump to "12.0" mid-edit. */
    val priceText: String = "",
    val isLoaded: Boolean = true,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val missingPuzzle: Boolean = false,
    val saveError: String? = null,
    val tagCatalog: List<String> = emptyList(),
    val allPuzzles: List<Puzzle> = emptyList(),
    /** Opened from a barcode scan (iOS "Add from scan"). */
    val isQuickAdd: Boolean = false,
    val scanMetadata: BarcodeProductMetadata? = null,
) {
    val canSave: Boolean get() = puzzle.name.isNotBlank() && !isSaving

    /** Puzzles that look like this one by name, brand, or pieces (iOS quick add "Looks similar"). */
    val similarMatches: List<Puzzle>
        get() = if (!isQuickAdd) {
            emptyList()
        } else {
            PuzzleSimilarMatchFinder.findSimilar(puzzle.name, puzzle.source, puzzle.pieces, puzzle.barcode, null, allPuzzles)
        }

    val scanSourcePuzzle: Puzzle?
        get() = scanMetadata?.sourcePuzzleId?.let { id -> allPuzzles.firstOrNull { it.id == id } }

    /** Another puzzle with the same barcode (iOS shows "Matches X in your collection."). */
    val barcodeDuplicate: Puzzle?
        get() = PuzzleDuplicateChecker.findDuplicate(puzzle.barcode, puzzle.id, allPuzzles)

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = puzzle.id.hashCode()
}

/**
 * Add and edit form. Edits work on a copy: nothing reaches the store until Save, and a failed
 * save keeps the form open with the user's values and the error.
 */
@HiltViewModel
class PuzzleFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val repository: PuzzleRepository,
    private val barcodeMetadataCache: BarcodeMetadataCache,
    private val clock: Clock,
    private val reviewPrompt: StoreReviewPrompt,
) : ViewModel() {
    private val puzzleId: String? = savedStateHandle.get<String>("id")?.takeIf { it.isNotBlank() && it != "new" }
    private val initialBarcode: String? = savedStateHandle.get<String>("barcode")?.takeIf { it.isNotBlank() }
    private val initialName: String? = savedStateHandle.get<String>("name")?.takeIf { it.isNotBlank() }
    private val initialPieces: Int? = savedStateHandle.get<String>("pieces")?.toIntOrNull()
    private val initialSource: String? = savedStateHandle.get<String>("source")?.takeIf { it.isNotBlank() }
    private var original: Puzzle? = null

    val isNew: Boolean get() = puzzleId == null

    private val _uiState = MutableStateFlow(PuzzleFormUiState(isLoaded = puzzleId == null))
    val uiState: StateFlow<PuzzleFormUiState> = _uiState.asStateFlow()

    private val tagCatalog: StateFlow<List<String>> = repository.observePuzzles()
        .map { puzzles -> PuzzleTagIndex.allTagNames(puzzles) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (puzzleId == null) {
            val draft = Puzzle(
                barcode = initialBarcode,
                name = initialName.orEmpty(),
                pieces = initialPieces,
                source = initialSource,
                completionDate = Instant.now(clock),
            )
            original = draft
            _uiState.value = PuzzleFormUiState(puzzle = draft, isQuickAdd = initialBarcode != null)
            if (initialBarcode != null) {
                viewModelScope.launch {
                    val metadata = barcodeMetadataCache.metadataFor(initialBarcode)
                    _uiState.update { it.copy(scanMetadata = metadata) }
                }
            }
        } else {
            viewModelScope.launch {
                val puzzle = repository.findById(puzzleId)
                if (puzzle == null) {
                    _uiState.update { it.copy(isLoaded = true, missingPuzzle = true) }
                } else {
                    original = puzzle
                    _uiState.update {
                        it.copy(puzzle = puzzle, priceText = priceText(puzzle.purchasePrice), isLoaded = true)
                    }
                }
            }
        }
        viewModelScope.launch {
            tagCatalog.collect { catalog -> _uiState.update { it.copy(tagCatalog = catalog) } }
        }
        viewModelScope.launch {
            repository.observePuzzles().collect { puzzles -> _uiState.update { it.copy(allPuzzles = puzzles) } }
        }
    }

    fun updateName(name: String) = update { it.copy(name = name) }
    fun updatePieces(pieces: Int?) = update { it.copy(pieces = pieces?.coerceIn(0, 99_999)) }
    fun updateRating(rating: PuzzleRating) = update { it.copy(rating = rating) }
    fun updateDifficulty(difficulty: PuzzleDifficulty) = update { it.copy(difficulty = difficulty) }
    fun updateStatus(status: PuzzleStatus) = update {
        it.withStatusChange(previous = it.status, new = status, now = Instant.now(clock))
    }
    fun updateProgress(percent: Int) = update {
        val clamped = PuzzleProgressSemantics.clamped(percent)
        val status = PuzzleProgressSemantics.statusFor(clamped)
        it.withStatusChange(previous = it.status, new = status, now = Instant.now(clock)).copy(progressPercent = clamped)
    }
    fun updateHours(hours: Int?) = update {
        it.copy(estimatedTimeSpent = (it.estimatedTimeSpent ?: PuzzleTime()).copy(hours = hours?.coerceAtLeast(0)))
    }
    fun updateMinutes(minutes: Int?) = update {
        val time = (it.estimatedTimeSpent ?: PuzzleTime()).copy(minutes = minutes?.coerceAtLeast(0))
        it.copy(estimatedTimeSpent = if ((time.minutes ?: 0) >= 60) time.normalized() else time)
    }
    fun useTimerTime(time: PuzzleTime) = update { it.copy(estimatedTimeSpent = time) }
    fun updateCompletionDate(instant: Instant) = update { it.copy(completionDate = instant) }
    fun updateStartDate(instant: Instant) = update { it.copy(startDate = instant) }
    fun updateNotes(notes: String) = update { it.copy(notes = notes.take(2_000)) }
    fun updateSource(source: String) = update { it.copy(source = source.take(200)) }
    fun updateBarcode(raw: String) = update {
        val digits = raw.filter(Char::isDigit).take(14)
        it.copy(barcode = digits.ifEmpty { null })
    }

    fun applyScannedBarcode(raw: String, activity: Activity?) {
        val normalized = BarcodeNormalizer.normalize(raw)
        if (normalized != null) {
            update { it.copy(barcode = normalized) }
            if (activity != null) reviewPrompt.requestIfEligible(activity, StoreReviewPrompt.Reason.BARCODE_SCAN)
        } else {
            updateBarcode(raw)
        }
    }
    fun updateHasMissingPieces(value: Boolean) = update { it.copy(hasMissingPieces = value) }
    fun updateTags(tags: List<String>) = update { it.copy(tags = tags) }
    fun updatePurchaseLocation(value: String) = update { it.copy(purchaseLocation = value.take(200)) }
    fun updateReleaseYear(year: Int?) = update { it.copy(releaseYear = year) }
    fun updatePrice(text: String) {
        _uiState.update { it.copy(priceText = text) }
        update { it.copy(purchasePrice = PurchasePriceFormatting.parse(text)) }
    }
    fun updateDimensions(value: String) = update { it.copy(dimensionsText = value.take(80)) }
    fun updateType(value: PuzzleType) = update { it.copy(puzzleType = value) }
    fun updateMaterial(value: PuzzleMaterial) = update { it.copy(material = value) }
    fun updateShape(value: PuzzleShape) = update { it.copy(puzzleShape = value) }
    fun updateCutType(value: PuzzleCutType) = update { it.copy(cutType = value) }
    fun updateDisposition(value: PuzzleDisposition) = update {
        val next = it.copy(disposition = value)
        if (PuzzleLoanSemantics.dispositionEndsOwnership(value)) PuzzleLoanSemantics.clearLoan(next) else next
    }
    fun updateOnLoan(on: Boolean) = update {
        if (!on) {
            PuzzleLoanSemantics.clearLoan(it)
        } else {
            val now = Instant.now(clock)
            it.copy(
                isOnLoan = true,
                loanedAt = it.loanedAt ?: now,
                dueBackDate = it.dueBackDate ?: now.atZone(ZoneId.systemDefault()).plusDays(14).toInstant(),
            )
        }
    }
    fun updateLoanedTo(name: String) = update { it.copy(loanedToDisplayName = name.take(80)) }
    fun updateDueBack(instant: Instant) = update { it.copy(dueBackDate = instant) }

    fun addPhotos(images: List<ByteArray>) {
        val state = _uiState.value
        val ordered = PuzzlePhotoSemantics.photosInOrder(state.puzzle.photos)
        val room = MAX_PHOTOS - ordered.size
        if (room <= 0 || images.isEmpty()) return
        val now = Instant.now(clock)
        val added = images.take(room).mapIndexed { index, bytes ->
            PuzzlePhoto(sortOrder = ordered.size + index, createdAt = now) to bytes
        }
        _uiState.update { it.copy(pendingPhotoBytes = it.pendingPhotoBytes + added.associate { (photo, bytes) -> photo.id to bytes }) }
        update { it.copy(photos = ordered + added.map { (photo, _) -> photo }) }
    }

    fun removePhoto(id: String) {
        _uiState.update { it.copy(pendingPhotoBytes = it.pendingPhotoBytes - id) }
        update { puzzle ->
            puzzle.copy(
                photos = PuzzlePhotoSemantics.normalizedSortOrders(
                    PuzzlePhotoSemantics.photosInOrder(puzzle.photos).filterNot { it.id == id },
                ),
            )
        }
    }

    fun movePhoto(id: String, direction: PuzzlePhotoSemantics.MoveDirection) = update {
        it.copy(photos = PuzzlePhotoSemantics.movingPhotoOneStep(id, direction, it.photos))
    }

    fun setCoverPhoto(id: String) = update { it.copy(photos = PuzzlePhotoSemantics.movingPhotoToCover(id, it.photos)) }

    suspend fun photoBytes(photo: PuzzlePhoto): ByteArray? =
        _uiState.value.pendingPhotoBytes[photo.id] ?: repository.photoData(photo)

    fun dismissSaveError() = _uiState.update { it.copy(saveError = null) }

    fun save(activity: Activity?, onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.canSave) return
        val draft = cleaned(state.puzzle)
        _uiState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            try {
                if (isNew) {
                    repository.add(
                        draft,
                        state.pendingPhotoBytes,
                        if (initialBarcode != null) PuzzleAddSource.BARCODE else PuzzleAddSource.MANUAL,
                    )
                } else {
                    repository.update(draft, state.pendingPhotoBytes)
                }
                _uiState.update { it.copy(isSaving = false, isDirty = false) }
                if (!isNew && !draft.isDemo && activity != null) {
                    reviewPrompt.requestIfEligible(activity, StoreReviewPrompt.Reason.PUZZLE_EDITED)
                }
                onSaved()
            } catch (error: PuzzleStoreException) {
                _uiState.update { it.copy(isSaving = false, saveError = error.message) }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, saveError = "Could not save changes to your collection. Your edits are still here.")
                }
            }
        }
    }

    private fun update(transform: (Puzzle) -> Puzzle) {
        _uiState.update { state ->
            val puzzle = transform(state.puzzle)
            state.copy(puzzle = puzzle, isDirty = puzzle != original || state.pendingPhotoBytes.isNotEmpty())
        }
    }

    private fun cleaned(puzzle: Puzzle): Puzzle = puzzle.copy(
        name = puzzle.name.trim(),
        source = puzzle.source?.trim()?.ifEmpty { null },
        purchaseLocation = puzzle.purchaseLocation?.trim()?.ifEmpty { null },
        notes = puzzle.notes?.takeIf { it.isNotBlank() },
        dimensionsText = puzzle.dimensionsText?.trim()?.ifEmpty { null },
        loanedToDisplayName = puzzle.loanedToDisplayName?.trim()?.ifEmpty { null },
        barcode = BarcodeNormalizer.normalize(puzzle.barcode),
    )

    private fun priceText(price: Double?): String = price?.let { String.format(Locale.US, "%.2f", it) }.orEmpty()
}
