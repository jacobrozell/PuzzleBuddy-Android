package com.jacobrozell.puzzlebuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleDateSemantics
import com.jacobrozell.puzzlebuddy.domain.catalog.PuzzleProgressSemantics
import com.jacobrozell.puzzlebuddy.domain.model.PuzzleStatus
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens

enum class ChipTone { ACCENT, WARM }

/**
 * Capsule filter chip, like iOS `listFilterChipLabel`: card fill when off, a tinted wash with
 * contrast-safe text when on. At least 48 dp tall.
 */
@Composable
fun BrandFilterChip(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ChipTone = ChipTone.ACCENT,
    enabled: Boolean = true,
    contentDescription: String = title,
    stateDescription: String? = null,
) {
    val brand = BrandTokens.colors
    val activeText = if (tone == ChipTone.WARM) brand.accentWarmText else brand.accentText
    val activeFill = if (tone == ChipTone.WARM) BrandTokens.accentWarm else BrandTokens.accent
    val textColor = when {
        !enabled -> brand.textSecondary.copy(alpha = 0.6f)
        isActive -> activeText
        else -> brand.textPrimary
    }
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(if (isActive) activeFill.copy(alpha = 0.14f) else brand.card)
            .border(
                width = 1.dp,
                color = if (isActive) activeFill.copy(alpha = 0.45f) else brand.textSecondary.copy(alpha = 0.22f),
                shape = CircleShape,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                stateDescription?.let { this.stateDescription = it }
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
        Text(
            title,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Status capsule from iOS `PuzzleStatusPill`. Hidden from TalkBack; the row label says the status. */
@Composable
fun PuzzleStatusPill(status: PuzzleStatus, modifier: Modifier = Modifier) {
    val brand = BrandTokens.colors
    val (text, fill) = when (status) {
        PuzzleStatus.WISHLIST -> brand.accentWarmText to BrandTokens.accentWarm.copy(alpha = 0.15f)
        PuzzleStatus.TODO -> brand.textSecondary to brand.cardElevated
        PuzzleStatus.IN_PROGRESS -> Color.White to BrandTokens.accentStrong
        PuzzleStatus.COMPLETED -> brand.accentText to BrandTokens.accent.copy(alpha = 0.15f)
        PuzzleStatus.ABANDONED -> brand.textSecondary to brand.cardElevated.copy(alpha = 0.7f)
    }
    Text(
        text = PuzzleDateSemantics.statusPillLabel(status),
        color = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .background(fill)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clearAndSetSemantics {},
    )
}

/** Thin bar plus percent for In-Progress rows, from iOS `PuzzleListProgressBar`. */
@Composable
fun PuzzleListProgressBar(progress: Int, modifier: Modifier = Modifier) {
    val clamped = PuzzleProgressSemantics.clamped(progress)
    val brand = BrandTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(brand.cardElevated),
        ) {
            if (clamped > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(clamped / 100f)
                        .widthIn(min = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(BrandTokens.accent),
                )
            }
        }
        Text(
            "$clamped%",
            color = brand.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 34.dp),
        )
    }
}

/** Icon + text badge for list rows (missing pieces, on loan, timer). */
@Composable
fun RowBadge(text: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, maxLines: Int = 1) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}
