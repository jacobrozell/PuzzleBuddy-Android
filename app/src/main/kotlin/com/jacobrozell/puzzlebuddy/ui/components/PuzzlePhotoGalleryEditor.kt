package com.jacobrozell.puzzlebuddy.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jacobrozell.puzzlebuddy.domain.photo.MAX_PHOTOS
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhoto
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhotoPick
import com.jacobrozell.puzzlebuddy.domain.photo.PuzzlePhotoSemantics
import com.jacobrozell.puzzlebuddy.support.ImageCompression
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens
import java.io.File

/**
 * Up to [MAX_PHOTOS] photos with arrow reordering, from iOS `PuzzlePhotoGalleryEditor`. The first
 * photo is the cover. Long-press a photo for Set as cover / Move / Remove; TalkBack gets the same
 * as custom actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PuzzlePhotoGalleryEditor(
    photos: List<PuzzlePhoto>,
    loadBytes: suspend (PuzzlePhoto) -> ByteArray?,
    onAddPhotos: (List<ByteArray>) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (String, PuzzlePhotoSemantics.MoveDirection) -> Unit,
    onSetCover: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val brand = BrandTokens.colors
    val sorted = PuzzlePhotoSemantics.photosInOrder(photos)
    val remaining = (MAX_PHOTOS - sorted.size).coerceAtLeast(0)
    var pendingRemoval by remember { mutableStateOf<PuzzlePhoto?>(null) }
    var photoNotice by remember { mutableStateOf<String?>(null) }

    fun read(uri: Uri): ByteArray? =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.let(ImageCompression::compressJpeg)

    fun deliver(uris: List<Uri>) {
        val slots = remaining
        val kept = uris.take(slots)
        val added = kept.mapNotNull(::read)
        if (added.isNotEmpty()) onAddPhotos(added)
        photoNotice = PuzzlePhotoPick.overflowMessage(
            slots = slots,
            selected = uris.size,
            unreadable = kept.size - added.size,
        )
    }

    val pickOne = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) deliver(listOf(uri))
    }
    // Recreate the picker when the open slots change. Otherwise it keeps offering five photos
    // after the puzzle already has some, and the extras used to disappear with no message.
    val pickMany = key(remaining.coerceAtLeast(2)) {
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(maxItems = remaining.coerceAtLeast(2)),
        ) { uris -> deliver(uris) }
    }
    val cameraUri = remember {
        val file = File(context.cacheDir, "images/capture-${System.currentTimeMillis()}.jpg").apply { parentFile?.mkdirs() }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) read(cameraUri)?.let { onAddPhotos(listOf(it)) }
    }

    photoNotice?.let { message ->
        AlertDialog(
            onDismissRequest = { photoNotice = null },
            title = { Text("Some photos were not added") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { photoNotice = null }) { Text("OK") }
            },
        )
    }

    pendingRemoval?.let { photo ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove photo?") },
            text = { Text("This photo will be removed from the puzzle.") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(photo.id)
                    pendingRemoval = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") } },
        )
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${sorted.size} of $MAX_PHOTOS photos",
                style = MaterialTheme.typography.labelLarge,
                color = brand.textSecondary,
                modifier = Modifier.weight(1f),
            )
            if (sorted.size > 1) {
                Text("Use arrows to reorder", style = MaterialTheme.typography.labelMedium, color = brand.textSecondary)
            }
        }

        if (sorted.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brand.cardElevated)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "No photos yet. Add box art, progress, or finished shots."
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = brand.textSecondary)
                    Text("Add box art, progress, or finished shots", color = brand.textSecondary)
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(sorted, key = { _, photo -> photo.id }) { index, photo ->
                    PhotoTile(
                        photo = photo,
                        index = index,
                        count = sorted.size,
                        loadBytes = loadBytes,
                        onRequestRemove = { pendingRemoval = photo },
                        onMove = { direction -> onMove(photo.id, direction) },
                        onSetCover = { onSetCover(photo.id) },
                    )
                }
            }
        }

        if (remaining > 0) {
            OutlinedButton(
                onClick = {
                    val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    if (remaining == 1) pickOne.launch(request) else pickMany.launch(request)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Text(if (remaining == 1) "  Add photo" else "  Add photos")
            }
            Button(
                onClick = { camera.launch(cameraUri) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Text("  Camera")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoTile(
    photo: PuzzlePhoto,
    index: Int,
    count: Int,
    loadBytes: suspend (PuzzlePhoto) -> ByteArray?,
    onRequestRemove: () -> Unit,
    onMove: (PuzzlePhotoSemantics.MoveDirection) -> Unit,
    onSetCover: () -> Unit,
) {
    val brand = BrandTokens.colors
    var bytes by remember(photo.id) { mutableStateOf<ByteArray?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(photo.id, photo.fileName) { bytes = loadBytes(photo) }
    val isCover = index == 0
    val label = "Photo ${index + 1} of $count${if (isCover) ", cover" else ""}"
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brand.cardElevated)
                .combinedClickable(onClick = {}, onLongClick = { menuOpen = true })
                .semantics {
                    contentDescription = label
                    customActions = buildList {
                        if (!isCover) add(CustomAccessibilityAction("Set as cover") { onSetCover(); true })
                        if (index > 0) {
                            add(CustomAccessibilityAction("Move earlier") { onMove(PuzzlePhotoSemantics.MoveDirection.EARLIER); true })
                        }
                        if (index < count - 1) {
                            add(CustomAccessibilityAction("Move later") { onMove(PuzzlePhotoSemantics.MoveDirection.LATER); true })
                        }
                        add(CustomAccessibilityAction("Remove") { onRequestRemove(); true })
                    }
                },
        ) {
            val bitmap = remember(bytes) { bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (isCover) {
                Text(
                    "Cover",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(BrandTokens.accentStrong)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            IconButton(
                onClick = onRequestRemove,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove photo ${index + 1}",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                        .padding(4.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (!isCover) DropdownMenuItem(text = { Text("Set as cover") }, onClick = { menuOpen = false; onSetCover() })
                if (index > 0) {
                    DropdownMenuItem(
                        text = { Text("Move earlier") },
                        onClick = { menuOpen = false; onMove(PuzzlePhotoSemantics.MoveDirection.EARLIER) },
                    )
                }
                if (index < count - 1) {
                    DropdownMenuItem(
                        text = { Text("Move later") },
                        onClick = { menuOpen = false; onMove(PuzzlePhotoSemantics.MoveDirection.LATER) },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuOpen = false; onRequestRemove() },
                )
            }
        }
        if (count > 1) {
            Row {
                IconButton(
                    onClick = { onMove(PuzzlePhotoSemantics.MoveDirection.EARLIER) },
                    enabled = index > 0,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move photo ${index + 1} earlier")
                }
                IconButton(
                    onClick = { onMove(PuzzlePhotoSemantics.MoveDirection.LATER) },
                    enabled = index < count - 1,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move photo ${index + 1} later")
                }
            }
        }
    }
}
