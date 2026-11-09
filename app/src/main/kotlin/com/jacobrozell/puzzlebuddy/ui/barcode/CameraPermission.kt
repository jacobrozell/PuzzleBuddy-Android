package com.jacobrozell.puzzlebuddy.ui.barcode

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Runs [action] after the camera permission is granted. A denial explains why the
 * scan needs the camera and offers the system Settings screen. The system dialog
 * does not appear again after the user has chosen "Don't ask again."
 */
@Composable
fun rememberCameraPermissionRequest(): (action: () -> Unit) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showDenied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val action = pending
        pending = null
        if (granted) action?.invoke() else showDenied = true
    }
    if (showDenied) {
        AlertDialog(
            onDismissRequest = { showDenied = false },
            title = { Text("Camera access is off") },
            text = {
                Text(
                    "Puzzle Buddy uses the camera only to read a barcode on the box. " +
                        "Nothing from the camera is saved or uploaded. Turn the camera on in Settings to scan.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDenied = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                ) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showDenied = false }) { Text("Not now") }
            },
        )
    }
    return { action ->
        pending = action
        launcher.launch(Manifest.permission.CAMERA)
    }
}
