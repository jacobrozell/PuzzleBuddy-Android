package com.jacobrozell.puzzlebuddy.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.support.ImageCompression
import java.io.File

@Composable
fun PuzzlePhotoPicker(
    puzzleId: String?,
    imageData: ByteArray?,
    repository: PuzzleRepository,
    onImageSelected: (ByteArray?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            onImageSelected(bytes?.let(ImageCompression::compressJpeg))
        }
    }
    val cameraUri = remember {
        val file = File(context.cacheDir, "images/capture-${System.currentTimeMillis()}.jpg").apply {
            parentFile?.mkdirs()
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            val bytes = context.contentResolver.openInputStream(cameraUri)?.use { it.readBytes() }
            onImageSelected(bytes?.let(ImageCompression::compressJpeg))
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PuzzlePhoto(
            puzzleId = puzzleId,
            imageData = imageData,
            repository = repository,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp)),
            placeholderSize = 48.dp,
        )
        OutlinedButton(
            onClick = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp),
        ) {
            Text("Choose photo")
        }
        Button(
            onClick = { cameraLauncher.launch(cameraUri) },
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp),
        ) {
            Text("Take photo")
        }
        if (imageData != null) {
            OutlinedButton(
                onClick = { onImageSelected(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Remove photo", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
