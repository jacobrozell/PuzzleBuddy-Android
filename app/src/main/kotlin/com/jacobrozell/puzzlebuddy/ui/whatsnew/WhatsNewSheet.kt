package com.jacobrozell.puzzlebuddy.ui.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.domain.surface.WhatsNewCopy
import com.jacobrozell.puzzlebuddy.domain.surface.WhatsNewItem
import com.jacobrozell.puzzlebuddy.ui.designsystem.boundedSheetScroll
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val brand = BrandTokens.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .boundedSheetScroll()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Filled.Extension,
                    contentDescription = null,
                    tint = BrandTokens.accent,
                    modifier = Modifier
                        .size(88.dp)
                        .clearAndSetSemantics {},
                )
                Text(
                    WhatsNewCopy.TITLE,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = brand.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { heading() },
                )
                Text(
                    WhatsNewCopy.SUBTITLE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = brand.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                WhatsNewCopy.items.forEach { item ->
                    WhatsNewRow(item)
                }
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clearAndSetSemantics { contentDescription = "Got it. Closes the 1.1 update notes" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandTokens.accentStrong,
                    contentColor = Color.White,
                ),
            ) {
                Text(WhatsNewCopy.DISMISS_TITLE)
            }
        }
    }
}

@Composable
private fun WhatsNewRow(item: WhatsNewItem) {
    val brand = BrandTokens.colors
    Row(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "${item.title}. ${item.message}"
        },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            itemIcon(item.symbolName),
            contentDescription = null,
            tint = BrandTokens.accent,
            modifier = Modifier.size(28.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, color = brand.textPrimary)
            Text(item.message, style = MaterialTheme.typography.bodyMedium, color = brand.textSecondary)
        }
    }
}

private fun itemIcon(symbolName: String): ImageVector = when (symbolName) {
    "clock.arrow.circlepath" -> Icons.Filled.History
    "person.fill.checkmark" -> Icons.Filled.HowToReg
    "timer" -> Icons.Filled.Timer
    else -> Icons.AutoMirrored.Filled.Undo
}
