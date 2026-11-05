package com.jacobrozell.puzzlebuddy.ui.puzzles

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.catalog.MediumDateLabel
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleDateSemantics
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleRowAccessibility
import com.jacobrozell.puzzlebuddy.domain.loan.PuzzleLoanSemantics
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.timer.PuzzleTimerSemantics
import com.jacobrozell.puzzlebuddy.ui.components.PuzzleListProgressBar
import com.jacobrozell.puzzlebuddy.ui.components.PuzzlePhoto
import com.jacobrozell.puzzlebuddy.ui.components.PuzzleStatusPill
import com.jacobrozell.puzzlebuddy.ui.components.RowBadge
import com.jacobrozell.puzzlebuddy.ui.components.StarRatingSummary
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens
import java.time.Instant

/** Font scale at which rows stack badges vertically, like iOS accessibility Dynamic Type sizes. */
internal const val LARGE_FONT_SCALE = 1.3f

/**
 * List row ported from iOS `PuzzleCell`: cover, name, brand, two tags, stars, wrapping badges,
 * progress bar (In-Progress), pieces and finish date. TalkBack reads one merged label with a
 * "Delete puzzle" action; long-press also asks to delete.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PuzzleRow(
    puzzle: Puzzle,
    repository: PuzzleRepository,
    onOpen: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
    now: Instant = Instant.now(),
) {
    val brand = BrandTokens.colors
    val largeFont = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val label = PuzzleRowAccessibility.label(puzzle, now)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onRequestDelete)
            .clearAndSetSemantics {
                contentDescription = label
                onClick(label = "Open puzzle details") { onOpen(); true }
                customActions = listOf(CustomAccessibilityAction("Delete puzzle") { onRequestDelete(); true })
            },
        colors = CardDefaults.cardColors(containerColor = brand.card),
    ) {
        val content: @Composable () -> Unit = { RowText(puzzle, now, largeFont) }
        if (largeFont) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Thumbnail(puzzle, repository)
                content()
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Thumbnail(puzzle, repository)
                Box(modifier = Modifier.weight(1f)) { content() }
            }
        }
    }
}

@Composable
private fun Thumbnail(puzzle: Puzzle, repository: PuzzleRepository) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(BrandTokens.colors.cardElevated),
        contentAlignment = Alignment.Center,
    ) {
        PuzzlePhoto(
            puzzleId = if (puzzle.hasImage) puzzle.id else null,
            imageData = null,
            repository = repository,
            modifier = Modifier.fillMaxSize(),
            placeholderSize = 32.dp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowText(puzzle: Puzzle, now: Instant, largeFont: Boolean) {
    val brand = BrandTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            puzzle.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = brand.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        puzzle.source?.takeIf { it.isNotEmpty() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = brand.textSecondary, maxLines = 1)
        }
        if (puzzle.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                puzzle.tags.take(2).forEach { tag ->
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(brand.cardElevated)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = brand.textSecondary, modifier = Modifier.size(12.dp))
                        Text(tag, style = MaterialTheme.typography.labelSmall, color = brand.textSecondary, maxLines = 1)
                    }
                }
                if (puzzle.tags.size > 2) {
                    Text("+${puzzle.tags.size - 2}", style = MaterialTheme.typography.labelSmall, color = brand.textSecondary)
                }
            }
        }
        StarRatingSummary(rating = puzzle.rating)
        BadgeRow(puzzle, now, largeFont)
        if (puzzle.status == PuzzleStatus.IN_PROGRESS) {
            PuzzleListProgressBar(progress = puzzle.progressPercent)
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            puzzle.pieces?.let {
                Text("$it pieces", style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
            }
            Spacer(modifier = Modifier.weight(1f))
            PuzzleDateSemantics.listTrailingDate(puzzle)?.let {
                Text(MediumDateLabel.format(it), style = MaterialTheme.typography.bodySmall, color = brand.textSecondary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BadgeRow(puzzle: Puzzle, now: Instant, largeFont: Boolean) {
    val brand = BrandTokens.colors
    val badges: @Composable () -> Unit = {
        PuzzleStatusPill(puzzle.status)
        if (puzzle.timesCompleted > 1) {
            Text(
                "×${puzzle.timesCompleted}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = brand.textSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(brand.cardElevated)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        if (puzzle.hasMissingPieces) {
            RowBadge("Missing pieces", Icons.Filled.Warning, brand.accentWarmText)
        }
        if (puzzle.isOnLoan) {
            val overdue = PuzzleLoanSemantics.isOverdue(puzzle, now)
            RowBadge(
                PuzzleLoanSemantics.listBadgeTitle(puzzle, now),
                if (overdue) Icons.Filled.Schedule else Icons.Filled.PersonPin,
                brand.accentWarmText,
                maxLines = 2,
            )
        }
        if (puzzle.status == PuzzleStatus.IN_PROGRESS && PuzzleTimerSemantics.isRunning(puzzle.timerRunningSince, now)) {
            RowBadge("Timer running", Icons.Filled.Timer, brand.accentText)
        }
    }
    if (largeFont) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { badges() }
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { badges() }
    }
}
