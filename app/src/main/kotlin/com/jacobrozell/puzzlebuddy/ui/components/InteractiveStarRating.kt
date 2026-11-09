package com.jacobrozell.puzzlebuddy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.domain.catalog.StarFill
import com.jacobrozell.puzzlebuddy.domain.catalog.decrementRating
import com.jacobrozell.puzzlebuddy.domain.catalog.incrementRating
import com.jacobrozell.puzzlebuddy.domain.catalog.ratingForStarTap
import com.jacobrozell.puzzlebuddy.domain.catalog.ratingSelectionLabel
import com.jacobrozell.puzzlebuddy.domain.catalog.starFillForRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens

/**
 * Half-star picker matching iOS `RatingsView`: one glyph per star, left/right tap targets,
 * and a label so the selected value is obvious.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HalfStarRatingRow(
    rating: PuzzleRating,
    onRatingChange: (PuzzleRating) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = BrandTokens.colors
    Column(modifier = modifier) {
        FlowRow(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "Puzzle rating"
                stateDescription = rating.accessibilityDescription
                customActions = listOf(
                    CustomAccessibilityAction("Increase rating") {
                        val next = incrementRating(rating)
                        if (next != rating) onRatingChange(next)
                        true
                    },
                    CustomAccessibilityAction("Decrease rating") {
                        val next = decrementRating(rating)
                        if (next != rating) onRatingChange(next)
                        true
                    },
                    CustomAccessibilityAction("Clear rating") {
                        onRatingChange(PuzzleRating.NONE)
                        true
                    },
                )
            },
            verticalArrangement = Arrangement.Center,
        ) {
            for (star in 1..5) {
                StarTapTarget(
                    fill = starFillForRating(rating.value, star),
                    onLeft = { onRatingChange(ratingForStarTap(star, leftSide = true)) },
                    onRight = { onRatingChange(ratingForStarTap(star, leftSide = false)) },
                )
            }
            Text(
                text = ratingSelectionLabel(rating),
                style = MaterialTheme.typography.labelLarge,
                color = if (rating == PuzzleRating.NONE) brand.textSecondary else brand.accentWarmText,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (rating != PuzzleRating.NONE) {
            TextButton(
                onClick = { onRatingChange(PuzzleRating.NONE) },
                modifier = Modifier.semantics { contentDescription = "Clear rating" },
            ) {
                Text("Clear")
            }
        }
    }
}

@Composable
private fun StarTapTarget(
    fill: StarFill,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        StarGlyph(fill = fill, modifier = Modifier.size(28.dp))
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onLeft),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onRight),
            )
        }
    }
}

@Composable
private fun StarGlyph(fill: StarFill, modifier: Modifier = Modifier) {
    val tint = BrandTokens.accentWarm
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (fill != StarFill.Full) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = tint.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (fill != StarFill.Empty) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (fill == StarFill.Half) Modifier.clip(LeftHalf) else Modifier),
            )
        }
    }
}

/** Left half of the star, so a 4.5 rating reads as four full stars and one half. */
private object LeftHalf : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rectangle(Rect(0f, 0f, size.width / 2f, size.height))
}
