package com.jacobrozell.puzzlebuddy.ui.puzzles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.domain.timer.PuzzleTimerSemantics
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens
import kotlinx.coroutines.delay
import java.time.Instant

/** In-progress timer. The clock is computed from stored fields; ticks are not written to the database. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PuzzleTimerCard(
    puzzle: Puzzle,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
) {
    if (puzzle.status != PuzzleStatus.IN_PROGRESS) return

    val brand = BrandTokens.colors
    val ticking = PuzzleTimerSemantics.isRunning(puzzle.timerRunningSince, Instant.now())
    val now by produceState(Instant.now(), ticking) {
        value = Instant.now()
        while (ticking) {
            delay(1_000)
            value = Instant.now()
        }
    }
    val elapsed = PuzzleTimerSemantics.elapsedSeconds(puzzle, now)
    val running = PuzzleTimerSemantics.isRunning(puzzle.timerRunningSince, now)
    val spoken = PuzzleTimerSemantics.spokenLabel(elapsed - elapsed % 60)
    var confirmReset by remember { mutableStateOf(false) }
    val largeFont = LocalDensity.current.fontScale >= LARGE_FONT_SCALE

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Timer",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = brand.textPrimary,
            maxLines = 2,
            modifier = Modifier.semantics { heading() },
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = brand.card),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    PuzzleTimerSemantics.clockLabel(elapsed),
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = brand.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = spoken
                        liveRegion = LiveRegionMode.Polite
                    },
                )
                val actions = @Composable {
                    Button(
                        onClick = { if (running) onPause() else onStart() },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .semantics { stateDescription = if (running) "running" else "paused" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandTokens.accentStrong,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(if (running) "Pause timer" else "Start timer", maxLines = 1)
                    }
                    if (elapsed > 0) {
                        TextButton(
                            onClick = { confirmReset = true },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(
                                "Reset timer",
                                color = brand.accentText,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
                if (largeFont) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) { actions() }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset timer?") },
            text = { Text("The timer goes back to 0:00:00. Time you already logged stays.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onReset()
                }) {
                    Text("Reset timer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}
