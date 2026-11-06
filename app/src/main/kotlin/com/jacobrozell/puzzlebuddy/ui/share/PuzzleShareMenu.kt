package com.jacobrozell.puzzlebuddy.ui.share

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle

@Composable
fun PuzzleShareMenu(
    entireCollection: List<Puzzle>,
    visibleList: List<Puzzle>,
    imageVector: ImageVector,
    onShare: (List<Puzzle>) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "Share collection. Creates a collage image of your puzzles to share"
            },
        ) {
            Icon(imageVector, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Share Entire Collection (${entireCollection.size})") },
                onClick = {
                    expanded = false
                    onShare(entireCollection)
                },
                enabled = entireCollection.isNotEmpty(),
            )
            DropdownMenuItem(
                text = { Text("Share Visible List (${visibleList.size})") },
                onClick = {
                    expanded = false
                    onShare(visibleList)
                },
                enabled = visibleList.isNotEmpty(),
            )
        }
    }
}
