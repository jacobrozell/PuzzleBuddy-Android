package com.jacobrozell.puzzlebuddy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleRating

@Composable
fun HalfStarRatingRow(
    rating: PuzzleRating,
    onRatingChange: (PuzzleRating) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.semantics {
            contentDescription = "Rating ${rating.value} out of 5"
        },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (star in 1..5) {
            val fullValue = star.toDouble()
            val halfValue = star - 0.5
            Row(modifier = Modifier.width(40.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .size(36.dp)
                        .clickable { onRatingChange(PuzzleRating.fromValue(halfValue)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (rating.value >= halfValue) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Half star $halfValue",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .size(36.dp)
                        .clickable { onRatingChange(PuzzleRating.fromValue(fullValue)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (rating.value >= fullValue) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Full star $fullValue",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (rating != PuzzleRating.NONE) {
            Text(
                "Clear",
                modifier = Modifier
                    .clickable { onRatingChange(PuzzleRating.NONE) }
                    .padding(start = 8.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
