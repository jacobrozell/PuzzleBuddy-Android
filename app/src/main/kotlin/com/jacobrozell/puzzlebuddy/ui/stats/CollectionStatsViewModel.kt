package com.jacobrozell.puzzlebuddy.ui.stats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionMilestone
import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionMilestones
import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionStats
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListPieceCountFilter
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.support.logging.AnalyticsKeyValueStore
import com.jacobrozell.puzzlebuddy.support.logging.AnalyticsMilestones
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.PuzzleAnalyticsMetadata
import com.jacobrozell.puzzlebuddy.support.logging.info
import com.jacobrozell.puzzlebuddy.support.share.PuzzleCollectionSharer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionStatsUiState(
    val puzzles: List<Puzzle> = emptyList(),
    val stats: CollectionStats = CollectionStats.compute(emptyList()),
    val milestone: CollectionMilestone? = null,
)

@HiltViewModel
class CollectionStatsViewModel @Inject constructor(
    private val repository: PuzzleRepository,
    private val store: AnalyticsKeyValueStore,
    private val analyticsMilestones: AnalyticsMilestones,
    private val logger: AppLogger,
) : ViewModel() {
    private val acknowledged = MutableStateFlow(store.getStringSet(CollectionMilestones.STORAGE_KEY))

    val uiState: StateFlow<CollectionStatsUiState> = combine(
        repository.observePuzzles(),
        acknowledged,
    ) { puzzles, acked ->
        val stats = CollectionStats.compute(puzzles)
        CollectionStatsUiState(
            puzzles = puzzles,
            stats = stats,
            milestone = CollectionMilestones.newlyEarned(stats, acked).firstOrNull(),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CollectionStatsUiState(),
    )

    fun acknowledge(milestoneId: String) {
        val updated = CollectionMilestones.acknowledge(
            store.getStringSet(CollectionMilestones.STORAGE_KEY),
            milestoneId,
        )
        store.putStringSet(CollectionMilestones.STORAGE_KEY, updated)
        acknowledged.value = updated
    }

    fun onMilestoneShown(milestoneId: String) {
        analyticsMilestones.logIfNeeded(milestoneId)
    }

    fun share(context: Context, puzzles: List<Puzzle>) {
        viewModelScope.launch {
            PuzzleCollectionSharer.share(context, repository, puzzles)
        }
    }

    fun logPickNextSelected(pieceCount: PuzzleListPieceCountFilter) {
        logger.info(
            LogCategory.UI,
            eventName = "pick_next_puzzle_selected",
            message = "Pick next puzzle selected.",
            metadata = mapOf(
                "entry_point" to "stats",
                "piece_count_bucket" to PuzzleAnalyticsMetadata.pieceCountBucket(pieceCount),
            ),
        )
    }
}
