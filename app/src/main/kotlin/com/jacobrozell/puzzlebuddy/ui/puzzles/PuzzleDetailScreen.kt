package com.jacobrozell.puzzlebuddy.ui.puzzles

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.puzzlebuddy.domain.catalog.MediumDateLabel
import com.jacobrozell.puzzlebuddy.domain.completion.CompletionUndoSemantics
import com.jacobrozell.puzzlebuddy.domain.catalog.PurchasePriceFormatting
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleDateSemantics
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleDetailMetrics
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.loan.PuzzleLoanSemantics
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleCutType
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDifficulty
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleDisposition
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleMaterial
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleShape
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleType
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhotoSemantics
import com.jacobrozell.puzzlebuddy.ui.components.StarRatingSummary
import com.jacobrozell.puzzlebuddy.ui.designsystem.AdaptiveTwoPane
import com.jacobrozell.puzzlebuddy.ui.designsystem.ReadableContentWidth
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

private enum class PendingConfirm { PUZZLE_AGAIN, MARK_RETURNED, DELETE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleDetailScreen(
    puzzleId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: PuzzleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val puzzle = (state.content as? PuzzleDetailContent.Loaded)?.puzzle
    var confirm by remember { mutableStateOf<PendingConfirm?>(null) }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(error.title) },
            text = { Text(error.message) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } },
        )
    }

    confirm?.let { pending ->
        val (title, message, action) = when (pending) {
            PendingConfirm.PUZZLE_AGAIN -> Triple(
                "Puzzle again?",
                "Start this puzzle again? Your previous completions stay in your history.",
                "Puzzle again",
            )
            PendingConfirm.MARK_RETURNED -> Triple("Mark returned?", "Clear the on-loan status for this puzzle?", "Mark returned")
            PendingConfirm.DELETE -> Triple(
                "Delete ${puzzle?.name ?: "puzzle"}?",
                "This puzzle will be removed from your collection. This cannot be undone.",
                "Delete",
            )
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    when (pending) {
                        PendingConfirm.PUZZLE_AGAIN -> viewModel.puzzleAgain()
                        PendingConfirm.MARK_RETURNED -> viewModel.markReturned()
                        PendingConfirm.DELETE -> viewModel.delete(onDeleted = onBack)
                    }
                }) {
                    Text(action, color = if (pending == PendingConfirm.DELETE) MaterialTheme.colorScheme.error else Color.Unspecified)
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(puzzle?.name ?: "Puzzle", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (puzzle != null) {
                        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit puzzle") }
                        IconButton(onClick = { confirm = PendingConfirm.DELETE }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete puzzle")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val content = state.content) {
            PuzzleDetailContent.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            PuzzleDetailContent.Missing -> MissingPuzzle(Modifier.padding(padding), onBack)
            is PuzzleDetailContent.Loaded -> ReadableContentWidth(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    state.undo?.let { snapshot ->
                        LaunchedEffect(snapshot.recordedAt) {
                            val remaining = Duration.between(
                                Instant.now(),
                                snapshot.recordedAt.plus(CompletionUndoSemantics.WINDOW),
                            )
                            if (!remaining.isNegative && !remaining.isZero) delay(remaining.toMillis())
                            viewModel.expireUndo()
                        }
                        UndoCompletionBanner(
                            onUndo = viewModel::undoCompletion,
                            onDismiss = viewModel::dismissUndo,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        AdaptiveTwoPane(
                            modifier = Modifier.fillMaxWidth(),
                            firstPane = {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    SummaryPanel(content.puzzle, viewModel::photoBytes)
                                    ProgressPanel(
                                        puzzle = content.puzzle,
                                        onCommit = { percent -> viewModel.commitProgress(content.puzzle, percent) },
                                        onPuzzleAgain = { confirm = PendingConfirm.PUZZLE_AGAIN },
                                        onMarkReturned = { confirm = PendingConfirm.MARK_RETURNED },
                                    )
                                    CompletionHistorySection(
                                        puzzle = content.puzzle,
                                        onSave = viewModel::saveCompletion,
                                        onDelete = viewModel::removeCompletion,
                                    )
                                    PuzzleTimerCard(
                                        puzzle = content.puzzle,
                                        onStart = viewModel::startTimer,
                                        onPause = viewModel::pauseTimer,
                                        onReset = viewModel::resetTimer,
                                    )
                                }
                            },
                            secondPane = { DetailsPanel(content.puzzle) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UndoCompletionBanner(onUndo: () -> Unit, onDismiss: () -> Unit) {
    val brand = BrandTokens.colors
    val largeFont = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val message = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = brand.accentText)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    "Marked complete",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = brand.textPrimary,
                )
                Text(
                    "Undo if that was an accident. Your previous status comes back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = brand.textSecondary,
                )
            }
        }
    }
    val actions = @Composable {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onUndo, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Undo", color = brand.accentText, maxLines = 1, softWrap = false)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss undo completion", tint = brand.textSecondary)
            }
        }
    }
    val banner = Modifier
        .fillMaxWidth()
        .background(BrandTokens.accent.copy(alpha = 0.12f))
        .semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "Marked complete. Undo if that was an accident."
        }
        .padding(12.dp)
    if (largeFont) {
        Column(modifier = banner, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            message()
            actions()
        }
    } else {
        Row(modifier = banner, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) { message() }
            actions()
        }
    }
}

@Composable
private fun MissingPuzzle(modifier: Modifier, onBack: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(
            "That puzzle is no longer in your collection.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onBack) { Text("Back to puzzles") }
    }
}

@Composable
private fun PanelCard(description: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrandTokens.colors.card),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

@Composable
private fun SummaryPanel(puzzle: Puzzle, loadBytes: suspend (PuzzlePhoto) -> ByteArray?) {
    PanelCard("Puzzle summary") {
        PhotoPager(puzzle, loadBytes)
        Text(
            puzzle.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        if (puzzle.rating != PuzzleRating.NONE) StarRatingSummary(rating = puzzle.rating)
        if (puzzle.difficulty != PuzzleDifficulty.NONE) {
            Text(
                "Difficulty: ${puzzle.difficulty.raw}",
                modifier = Modifier.semantics { contentDescription = puzzle.difficulty.accessibilityDescription },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoPager(puzzle: Puzzle, loadBytes: suspend (PuzzlePhoto) -> ByteArray?) {
    val brand = BrandTokens.colors
    val photos = PuzzlePhotoSemantics.photosInOrder(puzzle.photos)
    if (photos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brand.cardElevated)
                .semantics { contentDescription = "No puzzle photo" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Extension, contentDescription = null, tint = BrandTokens.accent, modifier = Modifier.size(56.dp))
        }
        return
    }
    val pager = rememberPagerState { photos.size }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) { page ->
            val photo = photos[page]
            var bytes by remember(photo.id) { mutableStateOf<ByteArray?>(null) }
            LaunchedEffect(photo.id) { bytes = loadBytes(photo) }
            val bitmap = remember(bytes) { bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brand.cardElevated)
                    .semantics {
                        contentDescription = "Puzzle photo for ${puzzle.name}, ${page + 1} of ${photos.size}" +
                            if (page == 0) ", cover" else ""
                    },
            ) {
                bitmap?.let {
                    Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                if (page == 0 && photos.size > 1) {
                    Text(
                        "Cover",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(BrandTokens.accentStrong)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
        if (photos.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(photos.size) { index ->
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (index == pager.currentPage) BrandTokens.accent else brand.textSecondary.copy(alpha = 0.3f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressPanel(
    puzzle: Puzzle,
    onCommit: (Int) -> Unit,
    onPuzzleAgain: () -> Unit,
    onMarkReturned: () -> Unit,
) {
    val brand = BrandTokens.colors
    var draft by remember(puzzle.id, puzzle.progressPercent) { mutableIntStateOf(PuzzleProgressSemantics.clamped(puzzle.progressPercent)) }
    PanelCard("Puzzle progress") {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$draft%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                PuzzleDateSemantics.statusPillLabel(PuzzleProgressSemantics.statusFor(draft).takeIf { draft != puzzle.progressPercent } ?: puzzle.status),
                color = brand.textSecondary,
            )
        }
        Slider(
            value = draft.toFloat(),
            onValueChange = { draft = (it / 5).toInt() * 5 },
            onValueChangeFinished = { if (draft != puzzle.progressPercent) onCommit(draft) },
            valueRange = 0f..100f,
            steps = 19,
            modifier = Modifier.semantics {
                contentDescription = "Progress"
                stateDescription = PuzzleProgressSemantics.displayLabel(draft)
            },
        )
        Text(
            PuzzleProgressSemantics.displayLabel(draft),
            color = brand.textSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        if (puzzle.status == PuzzleStatus.COMPLETED) {
            OutlinedButton(onClick = onPuzzleAgain, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Puzzle again", color = brand.accentText)
            }
        }
        if (puzzle.isOnLoan) {
            OutlinedButton(onClick = onMarkReturned, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Mark returned", color = brand.accentText)
            }
        }
    }
}

@Composable
private fun DetailsPanel(puzzle: Puzzle) {
    val now = Instant.now()
    val metrics = PuzzleDetailMetrics.compute(puzzle.pieces, puzzle.estimatedTimeSpent)
    val brand = BrandTokens.colors
    PanelCard("Puzzle details") {
        DetailRow("Status", puzzle.status.accessibilityDescription)
        puzzle.source?.trim()?.takeIf { it.isNotEmpty() }?.let { DetailRow("Brand", it) }
        puzzle.purchaseLocation?.trim()?.takeIf { it.isNotEmpty() }?.let { DetailRow("Bought at", it) }
        puzzle.releaseYear?.let { DetailRow("Year", it.toString()) }
        if (puzzle.puzzleType != PuzzleType.NONE) DetailRow("Type", puzzle.puzzleType.displayLabel)
        if (puzzle.material != PuzzleMaterial.NONE) DetailRow("Material", puzzle.material.displayLabel)
        if (puzzle.puzzleShape != PuzzleShape.NONE) DetailRow("Shape", puzzle.puzzleShape.displayLabel)
        if (puzzle.cutType != PuzzleCutType.NONE) DetailRow("Cut type", puzzle.cutType.displayLabel)
        puzzle.dimensionsText?.trim()?.takeIf { it.isNotEmpty() }?.let { DetailRow("Finished size", it) }
        puzzle.purchasePrice?.let { DetailRow("Paid", PurchasePriceFormatting.displayLabel(it, puzzle.purchaseCurrencyCode)) }
        if (puzzle.status == PuzzleStatus.COMPLETED && puzzle.disposition != PuzzleDisposition.NONE) {
            DetailRow("After finishing", puzzle.disposition.displayLabel)
        }
        if (puzzle.tags.isNotEmpty()) DetailRow("Tags", puzzle.tags.joinToString(", "))
        puzzle.barcode?.takeIf { it.isNotEmpty() }?.let { DetailRow("Barcode", it, copyAnnouncement = "Barcode copied") }
        DetailRow(PuzzleDateSemantics.detailDateLabel(puzzle.status), MediumDateLabel.format(puzzle.completionDate))
        PuzzleDateSemantics.progressDaysLabel(puzzle, now = now)?.let { DetailRow("Days puzzling", it) }
        puzzle.startDate?.takeIf { PuzzleDateSemantics.showsStartDatePicker(puzzle.status) }?.let {
            DetailRow("Started on", MediumDateLabel.format(it))
        }
        DetailRow("Missing pieces", if (puzzle.hasMissingPieces) "Yes" else "No")
        DetailRow("On loan", if (puzzle.isOnLoan) "Yes" else "No")
        if (puzzle.isOnLoan) {
            DetailRow("Loaned to", puzzle.loanedToDisplayName?.trim()?.takeIf { it.isNotEmpty() } ?: "Someone")
            puzzle.loanedAt?.let { DetailRow("Loaned on", MediumDateLabel.format(it)) }
            PuzzleLoanSemantics.dueBackDisplayValue(puzzle, now)?.let {
                DetailRow(
                    "Due back",
                    it,
                    valueColor = if (PuzzleLoanSemantics.isOverdue(puzzle, now)) brand.accentWarmText else brand.textPrimary,
                )
            }
        }
        puzzle.notes?.trim()?.takeIf { it.isNotEmpty() }?.let { DetailRow("Notes", it, copyAnnouncement = "Notes copied") }
        puzzle.estimatedTimeSpent?.displayLabel()?.let { DetailRow("Time spent", it) }
        metrics.timeBucketLabel?.let { DetailRow("Finish style", it) }
        puzzle.pieces?.let { DetailRow("Pieces", it.toString()) }
        metrics.formattedHoursPer1000Pieces?.let { DetailRow("Speed", it) }
    }
}

/**
 * Label / value row. Stacks at large font scale. With [copyAnnouncement], long-press copies the
 * value, like iOS `CopyableDetailRow`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = BrandTokens.colors.textPrimary,
    copyAnnouncement: String? = null,
) {
    val brand = BrandTokens.colors
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val stacked = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val copy: (() -> Unit)? = copyAnnouncement?.let { announcement ->
        {
            clipboard.setText(AnnotatedString(value))
            Toast.makeText(context, announcement, Toast.LENGTH_SHORT).show()
        }
    }
    val modifier = Modifier
        .fillMaxWidth()
        .then(
            if (copy != null) {
                Modifier
                    .combinedClickable(onClick = {}, onLongClick = copy)
                    .semantics { onLongClick(label = "Copy $label") { copy(); true } }
            } else {
                Modifier
            },
        )
        .semantics(mergeDescendants = true) {}
    if (stacked) {
        Column(modifier = modifier) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = brand.textSecondary)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
        }
    } else {
        Row(modifier = modifier, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = brand.textSecondary)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                textAlign = TextAlign.End,
                maxLines = 4,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
