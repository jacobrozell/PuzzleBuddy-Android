package com.jacobrozell.puzzlebuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.domain.catalog.StarFill
import com.jacobrozell.puzzlebuddy.domain.catalog.starFillForRating
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating

@Composable
fun StarRatingSummary(
    rating: PuzzleRating,
    modifier: Modifier = Modifier,
) {
    if (rating == PuzzleRating.NONE) return
    Row(
        modifier = modifier.semantics {
            contentDescription = "Rating ${rating.value} out of 5"
        },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (position in 1..5) {
            val fill = starFillForRating(rating.value, position)
            val icon = when (fill) {
                StarFill.Full -> Icons.Filled.Star
                StarFill.Half -> Icons.Filled.StarHalf
                StarFill.Empty -> Icons.Outlined.Star
            }
            val tint = when (fill) {
                StarFill.Empty -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.primary
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = String.format("%.1f", rating.value),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
