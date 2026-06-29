package com.jacobrozell.puzzlebuddy.ui.puzzles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.ui.components.PuzzlePhoto
import com.jacobrozell.puzzlebuddy.ui.components.StarRatingSummary
import com.jacobrozell.puzzlebuddy.ui.designsystem.AdaptiveTwoPane
import com.jacobrozell.puzzlebuddy.ui.designsystem.ReadableContentWidth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleDetailScreen(
    puzzleId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: PuzzleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val puzzle = state.puzzle

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(puzzle?.name ?: "Puzzle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit, enabled = puzzle != null) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit puzzle")
                    }
                    IconButton(
                        onClick = { viewModel.delete(onDeleted = onBack) },
                        enabled = puzzle != null,
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete puzzle")
                    }
                },
            )
        },
    ) { padding ->
        if (puzzle == null) {
            Text("Puzzle not found", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        val dateLabel = DateTimeFormatter.ofPattern("MMMM d, yyyy")
            .format(puzzle.completionDate.atZone(ZoneId.systemDefault()))

        ReadableContentWidth(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            AdaptiveTwoPane(
                modifier = Modifier.fillMaxWidth(),
                firstPane = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PuzzlePhoto(
                            puzzleId = puzzle.id,
                            imageData = state.imageData,
                            repository = viewModel.repository,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(puzzle.name, style = MaterialTheme.typography.headlineSmall)
                                puzzle.source?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                StarRatingSummary(rating = puzzle.rating)
                                if (puzzle.difficulty != PuzzleDifficulty.NONE) {
                                    Text("Difficulty ${puzzle.difficulty.raw} / 5")
                                }
                                if (puzzle.tags.isNotEmpty()) {
                                    Text(
                                        "Tags: ${puzzle.tags.joinToString()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                },
                secondPane = {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DetailRow("Status", puzzle.status.raw)
                            if (puzzle.status == PuzzleStatus.IN_PROGRESS) {
                                DetailRow("Progress", PuzzleProgressSemantics.displayLabel(puzzle.progressPercent))
                            }
                            DetailRow("Completion date", dateLabel)
                            puzzle.estimatedTimeSpent?.displayLabel()?.let { DetailRow("Time spent", it) }
                            puzzle.pieces?.let { DetailRow("Pieces", it.toString()) }
                            state.metrics.timeBucketLabel?.let { DetailRow("Puzzle pace", it) }
                            state.metrics.formattedHoursPer1000Pieces?.let { DetailRow("Pace", it) }
                            puzzle.notes?.let { DetailRow("Notes", it) }
                            if (puzzle.hasMissingPieces) DetailRow("Missing pieces", "Yes")
                            puzzle.barcode?.let { DetailRow("Barcode", it) }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
