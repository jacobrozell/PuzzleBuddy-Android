package com.jacobrozell.puzzlebuddy.ui.puzzles

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.domain.catalog.MediumDateLabel
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletion
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletionHistoryCopy
import com.jacobrozell.puzzlebuddy.domain.completion.PuzzleCompletionSemantics
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.ui.components.DateField
import com.jacobrozell.puzzlebuddy.ui.components.FormSection
import com.jacobrozell.puzzlebuddy.ui.components.SwitchRow
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens
import java.time.Instant

private sealed interface HistoryConfirm {
    val completion: PuzzleCompletion

    data class Remove(override val completion: PuzzleCompletion) : HistoryConfirm
    data class ChooseStatus(override val completion: PuzzleCompletion) : HistoryConfirm
}

/**
 * Finish logs on the detail screen. Visible when the puzzle is completed or already has logs.
 * Rows are a plain column because this sits inside the detail screen's scroll.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CompletionHistorySection(
    puzzle: Puzzle,
    onSave: (PuzzleCompletion, onSaved: () -> Unit, onFailed: (String) -> Unit) -> Unit,
    onDelete: (completionId: String, statusIfRemovingLast: PuzzleStatus?) -> Unit,
) {
    if (puzzle.completions.isEmpty() && puzzle.status != PuzzleStatus.COMPLETED) return

    val brand = BrandTokens.colors
    val sorted = PuzzleCompletionSemantics.sortedNewestFirst(puzzle.completions)
    var editing by remember { mutableStateOf<PuzzleCompletion?>(null) }
    var confirm by remember { mutableStateOf<HistoryConfirm?>(null) }
    var menuCompletionId by remember { mutableStateOf<String?>(null) }

    fun requestDelete(completion: PuzzleCompletion) {
        editing = null
        confirm = if (sorted.size == 1 && puzzle.status == PuzzleStatus.COMPLETED) {
            HistoryConfirm.ChooseStatus(completion)
        } else {
            HistoryConfirm.Remove(completion)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Completion history",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = brand.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .semantics { heading() },
            )
            if (puzzle.timesCompleted > 0) {
                Text(
                    puzzle.timesCompleted.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = brand.textPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(brand.cardElevated)
                        .clearAndSetSemantics {
                            contentDescription = "${puzzle.timesCompleted} completions"
                        }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = brand.card),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (sorted.isEmpty()) {
                Text(
                    "No completion logs yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = brand.textSecondary,
                    modifier = Modifier
                        .padding(16.dp)
                        .clearAndSetSemantics { contentDescription = "No completion logs yet" },
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    sorted.forEachIndexed { index, completion ->
                        CompletionRow(
                            completion = completion,
                            menuOpen = menuCompletionId == completion.id,
                            onOpen = { editing = completion },
                            onMenu = { menuCompletionId = completion.id },
                            onDismissMenu = { menuCompletionId = null },
                            onEdit = {
                                menuCompletionId = null
                                editing = completion
                            },
                            onRemove = {
                                menuCompletionId = null
                                requestDelete(completion)
                            },
                        )
                        if (index < sorted.lastIndex) {
                            HorizontalDivider(color = brand.textSecondary.copy(alpha = 0.18f))
                        }
                    }
                }
            }
        }
    }

    editing?.let { completion ->
        CompletionEditSheet(
            completion = completion,
            onDismiss = { editing = null },
            onSave = { updated, onFailed ->
                onSave(updated, { editing = null }, onFailed)
            },
            onRemove = { requestDelete(completion) },
        )
    }

    when (val pending = confirm) {
        is HistoryConfirm.Remove -> AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(PuzzleCompletionHistoryCopy.DELETE_DIALOG_TITLE) },
            text = {
                Text(
                    PuzzleCompletionHistoryCopy.deleteDialogMessage(
                        completionNumber = pending.completion.completionNumber,
                        completedAt = pending.completion.completedAt,
                        remainingCount = sorted.size,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    onDelete(pending.completion.id, null)
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
        is HistoryConfirm.ChooseStatus -> AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Set puzzle status") },
            text = { Text(PuzzleCompletionHistoryCopy.LAST_COMPLETION_STATUS_PROMPT) },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        confirm = null
                        onDelete(pending.completion.id, PuzzleStatus.IN_PROGRESS)
                    }) { Text("In progress") }
                    TextButton(onClick = {
                        confirm = null
                        onDelete(pending.completion.id, PuzzleStatus.TODO)
                    }) { Text("To-Do") }
                    TextButton(onClick = { confirm = null }) { Text("Cancel") }
                }
            },
        )
        null -> Unit
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompletionRow(
    completion: PuzzleCompletion,
    menuOpen: Boolean,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val brand = BrandTokens.colors
    val summary = completionSummary(completion)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .combinedClickable(
                        onClick = onOpen,
                        onLongClick = onMenu,
                        onClickLabel = "Opens editor for this completion log",
                        onLongClickLabel = "Edit or remove this finish log",
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription = completionAccessibilityLabel(completion)
                    }
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Completion ${completion.completionNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = brand.textPrimary,
                    )
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = brand.textSecondary)
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = brand.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = { Text(PuzzleCompletionHistoryCopy.SWIPE_EDIT_TITLE) },
                    onClick = onEdit,
                )
                DropdownMenuItem(
                    text = { Text(PuzzleCompletionHistoryCopy.SWIPE_REMOVE_TITLE, color = MaterialTheme.colorScheme.error) },
                    onClick = onRemove,
                )
            }
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Remove completion ${completion.completionNumber}",
                tint = brand.accentWarmText,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompletionEditSheet(
    completion: PuzzleCompletion,
    onDismiss: () -> Unit,
    onSave: (PuzzleCompletion, onFailed: (String) -> Unit) -> Unit,
    onRemove: () -> Unit,
) {
    var completedAt by remember(completion.id) { mutableStateOf(completion.completedAt) }
    var hours by remember(completion.id) { mutableIntStateOf(completion.timeSpentHours ?: 0) }
    var minutes by remember(completion.id) { mutableIntStateOf(completion.timeSpentMinutes ?: 0) }
    var tracksTime by remember(completion.id) {
        mutableStateOf((completion.timeSpentHours ?: 0) > 0 || (completion.timeSpentMinutes ?: 0) > 0)
    }
    var saveError by remember(completion.id) { mutableStateOf<String?>(null) }
    var saving by remember(completion.id) { mutableStateOf(false) }
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
                IconButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel")
                }
                Text(
                    "Edit completion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                IconButton(
                    onClick = {
                        if (saving) return@IconButton
                        saving = true
                        saveError = null
                        onSave(editedCompletion(completion, completedAt, tracksTime, hours, minutes)) { message ->
                            saving = false
                            saveError = message
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Save", tint = BrandTokens.colors.accentText)
                }
            }
            saveError?.let { message ->
                Column {
                    Text("Could not save completion", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
            FormSection(header = null) {
                DateField("Finished on", completedAt, onValueChange = { completedAt = it })
            }
            FormSection(header = null, footer = "Time spent is stored on this completion log only.") {
                SwitchRow("Log time spent", tracksTime, onCheckedChange = { tracksTime = it })
                if (tracksTime) {
                    StepperRow(
                        valueLabel = "$hours hr",
                        value = hours,
                        range = 0..999,
                        onValueChange = { hours = it },
                        decreaseDescription = "Decrease hours",
                        increaseDescription = "Increase hours",
                    )
                    StepperRow(
                        valueLabel = "$minutes min",
                        value = minutes,
                        range = 0..59,
                        onValueChange = { minutes = it },
                        decreaseDescription = "Decrease minutes",
                        increaseDescription = "Increase minutes",
                    )
                }
            }
            TextButton(
                onClick = onRemove,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StepperRow(
    valueLabel: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(valueLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        IconButton(
            onClick = { onValueChange(value - 1) },
            enabled = value > range.first,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icons.Filled.Remove, contentDescription = decreaseDescription)
        }
        IconButton(
            onClick = { onValueChange(value + 1) },
            enabled = value < range.last,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = increaseDescription)
        }
    }
}

private fun editedCompletion(
    completion: PuzzleCompletion,
    completedAt: Instant,
    tracksTime: Boolean,
    hours: Int,
    minutes: Int,
): PuzzleCompletion {
    val (storedHours, storedMinutes) = if (!tracksTime || (hours == 0 && minutes == 0)) {
        null to null
    } else {
        hours.takeIf { it > 0 } to minutes.takeIf { it > 0 }
    }
    return completion.copy(
        completedAt = completedAt,
        timeSpentHours = storedHours,
        timeSpentMinutes = storedMinutes,
    )
}

private fun completionSummary(completion: PuzzleCompletion): String = listOfNotNull(
    MediumDateLabel.format(completion.completedAt),
    completion.timeSpentLabel,
    completion.ratingLabel,
).joinToString(" · ")

private fun completionAccessibilityLabel(completion: PuzzleCompletion): String = buildString {
    append("Completion ${completion.completionNumber}, ")
    append(MediumDateLabel.format(completion.completedAt))
    completion.timeSpentLabel?.let { append(", $it") }
    completion.ratingLabel?.let { append(", $it") }
}
