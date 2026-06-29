package com.jacobrozell.puzzlebuddy.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository

@Composable
fun PuzzlePhoto(
    puzzleId: String?,
    imageData: ByteArray?,
    repository: PuzzleRepository,
    modifier: Modifier = Modifier,
    contentDescription: String = "Puzzle photo",
    placeholderSize: Dp = 32.dp,
) {
    var loaded by remember(puzzleId, imageData) { mutableStateOf(imageData) }
    var loading by remember(puzzleId) { mutableStateOf(false) }

    LaunchedEffect(puzzleId, imageData) {
        if (imageData != null) {
            loaded = imageData
            return@LaunchedEffect
        }
        if (puzzleId.isNullOrEmpty()) {
            loaded = null
            return@LaunchedEffect
        }
        loading = true
        loaded = repository.imageDataFor(puzzleId)
        loading = false
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            loaded != null -> {
                val bitmap = remember(loaded) {
                    BitmapFactory.decodeByteArray(loaded, 0, loaded!!.size)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    PlaceholderIcon(placeholderSize)
                }
            }
            else -> PlaceholderIcon(placeholderSize)
        }
    }
}

@Composable
private fun PlaceholderIcon(size: Dp) {
    Icon(
        imageVector = Icons.Default.Extension,
        contentDescription = null,
        modifier = Modifier.size(size),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
