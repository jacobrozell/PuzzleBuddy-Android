package com.jacobrozell.puzzlebuddy.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CollectionStatsViewModel @Inject constructor(
    repository: PuzzleRepository,
) : ViewModel() {
    val stats: StateFlow<CollectionStats> = repository.observePuzzles()
        .map { puzzles -> CollectionStats.compute(puzzles) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CollectionStats.compute(emptyList()),
        )
}
