package com.jacobrozell.puzzlebuddy.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.puzzlebuddy.domain.catalog.CollectionStats
import com.jacobrozell.puzzlebuddy.ui.designsystem.AdaptiveLayout
import com.jacobrozell.puzzlebuddy.ui.designsystem.ReadableContentWidth

private data class StatItem(val title: String, val value: String, val subtitle: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionStatsScreen(
    viewModel: CollectionStatsViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val statItems = remember(stats) {
        buildList {
            add(StatItem("Completed", CollectionStats.formatPieceCount(stats.completedCount), "puzzles finished"))
            add(StatItem("Pieces assembled", CollectionStats.formatPieceCount(stats.totalPiecesCompleted)))
            add(StatItem("On your shelf", CollectionStats.formatPieceCount(stats.backlogCount), "To-Do backlog"))
            add(StatItem("In progress", stats.inProgressCount.toString()))
            add(StatItem("Time at the table", stats.formattedTotalHours))
            stats.formattedAverageRating?.let {
                add(StatItem("Average rating", it, "completed puzzles with ratings"))
            }
            stats.favoritePieceCount?.let {
                add(StatItem("Go-to piece count", CollectionStats.formatPieceCount(it)))
            }
            add(StatItem("Finished this month", stats.completionsThisMonth.toString()))
            add(StatItem("Finished this year", stats.completionsThisYear.toString()))
            stats.biggestCompletedPieces?.let {
                add(StatItem("Biggest completed", CollectionStats.formatPieceCount(it)))
            }
            stats.smallestCompletedPieces?.let {
                add(StatItem("Smallest completed", CollectionStats.formatPieceCount(it)))
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Collection Stats") }) },
    ) { padding ->
        ReadableContentWidth(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val useGrid = maxWidth >= AdaptiveLayout.expandedWidthBreakpoint
                if (useGrid) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(statItems, key = { it.title }) { item ->
                            StatCard(item.title, item.value, item.subtitle)
                        }
                        if (stats.topTags.isNotEmpty()) {
                            item(key = "top-tags") {
                                TopTagsCard(stats.topTags.map { it.name to it.count })
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        statItems.forEach { item ->
                            StatCard(item.title, item.value, item.subtitle)
                        }
                        if (stats.topTags.isNotEmpty()) {
                            TopTagsCard(stats.topTags.map { it.name to it.count })
                        }
                        if (stats.totalCount == 0) {
                            Text(
                                "Add puzzles to see collection insights.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopTagsCard(tags: List<Pair<String, Int>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Top tags", style = MaterialTheme.typography.titleMedium)
            tags.forEach { (name, count) ->
                Text("$name ($count)")
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, subtitle: String? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = buildString {
                    append(title)
                    append(", ")
                    append(value)
                    subtitle?.let { append(", $it") }
                }
            },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
