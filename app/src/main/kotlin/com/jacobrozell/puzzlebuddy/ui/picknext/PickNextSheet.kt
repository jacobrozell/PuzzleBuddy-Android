package com.jacobrozell.puzzlebuddy.ui.picknext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleListPieceCountFilter
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleRandomPicker
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleTagCount
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.ui.components.FormSection
import com.jacobrozell.puzzlebuddy.ui.components.PickerField
import com.jacobrozell.puzzlebuddy.ui.components.SwitchRow
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickNextSheet(
    puzzles: List<Puzzle>,
    tags: List<PuzzleTagCount>,
    onOpen: (String) -> Unit,
    onPicked: (PuzzleListPieceCountFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var includeInProgress by rememberSaveable { mutableStateOf(false) }
    var pieceCount by rememberSaveable { mutableStateOf(PuzzleListPieceCountFilter.ANY) }
    var tagFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedId by rememberSaveable { mutableStateOf<String?>(null) }
    val brand = BrandTokens.colors
    val eligible = PuzzleRandomPicker.eligible(puzzles, includeInProgress, pieceCount, tagFilter)
    val picked = puzzles.firstOrNull { it.id == pickedId }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Pick my next puzzle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                IconButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = "Done", tint = brand.accentText)
                }
            }
            FormSection(header = "Filters", footer = poolSummary(eligible.size)) {
                SwitchRow(
                    "Include In-Progress puzzles",
                    includeInProgress,
                    onCheckedChange = { includeInProgress = it },
                )
                PickerField(
                    label = "Pieces",
                    options = PuzzleListPieceCountFilter.entries,
                    selected = pieceCount,
                    labelFor = { it.title },
                    onSelected = { pieceCount = it },
                )
                PickerField(
                    label = "Tag",
                    options = listOf<String?>(null) + tags.map { it.name },
                    selected = tagFilter,
                    labelFor = { name ->
                        if (name == null) "Any tag" else {
                            val count = tags.firstOrNull { it.name.equals(name, ignoreCase = true) }?.count
                            if (count == null) name else "$name ($count)"
                        }
                    },
                    onSelected = { tagFilter = it },
                )
            }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = brand.cardElevated),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (picked != null) {
                        Text(
                            picked.name.ifBlank { "Untitled puzzle" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        picked.pieces?.let { Text("$it pieces", color = brand.textSecondary) }
                        if (picked.tags.isNotEmpty()) {
                            Text(
                                picked.tags.joinToString(" · "),
                                color = brand.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Button(
                            onClick = {
                                onDismiss()
                                onOpen(picked.id)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandTokens.accentStrong,
                                contentColor = Color.White,
                            ),
                        ) { Text("Open puzzle") }
                    } else {
                        Text(
                            if (eligible.isEmpty()) {
                                "No eligible puzzles. Adjust filters or add To-Do puzzles."
                            } else {
                                "Tap Spin to pick a puzzle from your backlog."
                            },
                            color = brand.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Button(
                onClick = {
                    val next = PuzzleRandomPicker.pick(eligible, excluding = pickedId)
                    pickedId = next?.id
                    if (next != null) onPicked(pieceCount)
                },
                enabled = eligible.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandTokens.accentStrong,
                    contentColor = Color.White,
                ),
            ) {
                Text(if (picked == null) "Spin" else "Spin again")
            }
        }
    }
}

private fun poolSummary(count: Int): String = when (count) {
    0 -> "No eligible puzzles match these filters."
    1 -> "1 puzzle in the pool."
    else -> "$count puzzles in the pool."
}
