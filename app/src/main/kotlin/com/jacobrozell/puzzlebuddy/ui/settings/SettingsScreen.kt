package com.jacobrozell.puzzlebuddy.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.puzzlebuddy.domain.export.PuzzleCollectionExportFormat
import com.jacobrozell.puzzlebuddy.domain.surface.ProductSurface
import com.jacobrozell.puzzlebuddy.ui.designsystem.ReadableContentWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onReplayOnboarding: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showClearAlert by remember { mutableStateOf(false) }
    var showDemoAlert by remember { mutableStateOf(false) }
    var showRemoveDemoAlert by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) viewModel.importCsv(bytes)
        }
    }

    if (showClearAlert) {
        AlertDialog(
            onDismissRequest = { showClearAlert = false },
            title = { Text("Delete all puzzles?") },
            text = { Text("This permanently removes every puzzle on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearAlert = false
                    viewModel.clearCollection()
                }) { Text("Delete All") }
            },
            dismissButton = { TextButton(onClick = { showClearAlert = false }) { Text("Cancel") } },
        )
    }
    if (showDemoAlert) {
        AlertDialog(
            onDismissRequest = { showDemoAlert = false },
            title = { Text("Load demo puzzles?") },
            text = { Text("Adds four sample puzzles. Your existing puzzles stay in the collection.") },
            confirmButton = {
                TextButton(onClick = {
                    showDemoAlert = false
                    viewModel.loadDemoData()
                }) { Text("Load Demo Data") }
            },
            dismissButton = { TextButton(onClick = { showDemoAlert = false }) { Text("Cancel") } },
        )
    }
    if (showRemoveDemoAlert) {
        AlertDialog(
            onDismissRequest = { showRemoveDemoAlert = false },
            title = { Text("Remove demo puzzles?") },
            text = { Text("Removes ${state.demoCount} sample puzzles. Your own puzzles are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDemoAlert = false
                    viewModel.removeDemoData()
                }) { Text("Remove Demo Data") }
            },
            dismissButton = { TextButton(onClick = { showRemoveDemoAlert = false }) { Text("Cancel") } },
        )
    }
    state.importSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImportSummary,
            title = { Text("Import complete") },
            text = { Text(summary.message) },
            confirmButton = { TextButton(onClick = viewModel::dismissImportSummary) { Text("OK") } },
        )
    }
    state.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Something went wrong") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } },
        )
    }

    state.exportErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissExportError,
            title = { Text("Export failed") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissExportError) { Text("OK") } },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        if (state.isBusy) {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        ReadableContentWidth(modifier = Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.Extension, contentDescription = "Puzzle Buddy", tint = MaterialTheme.colorScheme.primary)
                    Text("Puzzle Buddy", style = MaterialTheme.typography.titleLarge)
                    Text("Track your puzzle collection", style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                SectionHeader("Display")
                listOf("system" to "System default", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        trailingContent = {
                            if (state.appearanceMode == value) Text("✓", color = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier
                            .clickable { viewModel.setAppearance(value) }
                            .fillMaxWidth(),
                    )
                }
            }
            item {
                SectionHeader("Collection")
                if (ProductSurface.isIPDbImportEnabled) {
                    ListItem(
                        headlineContent = { Text("Import from IPDb CSV") },
                        supportingContent = { Text("Export from IPDb Listview → CSV, then choose the file") },
                        modifier = Modifier
                            .clickable {
                                importLauncher.launch(arrayOf("text/*", "text/csv", "application/csv"))
                            }
                            .fillMaxWidth(),
                    )
                }
                ListItem(
                    headlineContent = { Text("Export collection") },
                    supportingContent = { Text("Share JSON backup or IPDb-compatible CSV") },
                    modifier = Modifier
                        .clickable(enabled = state.puzzleCount > 0) { showExportMenu = true }
                        .fillMaxWidth(),
                )
                if (showExportMenu) {
                    AlertDialog(
                        onDismissRequest = { showExportMenu = false },
                        title = { Text("Export format") },
                        text = {
                            androidx.compose.foundation.layout.Column {
                                TextButton(onClick = {
                                    showExportMenu = false
                                    viewModel.exportCollection(PuzzleCollectionExportFormat.JSON)
                                }) { Text("JSON backup") }
                                TextButton(onClick = {
                                    showExportMenu = false
                                    viewModel.exportCollection(PuzzleCollectionExportFormat.CSV)
                                }) { Text("IPDb CSV") }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showExportMenu = false }) { Text("Cancel") }
                        },
                    )
                }
                ListItem(
                    headlineContent = { Text("Load Demo Data") },
                    modifier = Modifier.clickable { showDemoAlert = true }.fillMaxWidth(),
                )
                ListItem(
                    headlineContent = { Text("Remove Demo Data") },
                    supportingContent = { Text("${state.demoCount} demo puzzles") },
                    modifier = Modifier
                        .clickable(enabled = state.demoCount > 0) { showRemoveDemoAlert = true }
                        .fillMaxWidth(),
                )
                ListItem(
                    headlineContent = { Text("Delete All Puzzles") },
                    modifier = Modifier
                        .clickable(enabled = state.puzzleCount > 0) { showClearAlert = true }
                        .fillMaxWidth(),
                )
            }
            item {
                SectionHeader("Barcode")
                ListItem(
                    headlineContent = { Text("Look up product info") },
                    supportingContent = { Text("Uses UPCitemdb when online; learns from your saved puzzles offline") },
                    trailingContent = {
                        Switch(
                            checked = state.barcodeLookupEnabled,
                            onCheckedChange = viewModel::setBarcodeLookupEnabled,
                        )
                    },
                )
            }
            item {
                SectionHeader("Help & Legal")
                LegalLink("Privacy Policy", "https://jacobrozell.github.io/PuzzleBuddy/privacy.html")
                LegalLink("Support", "https://jacobrozell.github.io/PuzzleBuddy/support.html")
                LegalLink("Accessibility", "https://jacobrozell.github.io/PuzzleBuddy/accessibility.html")
            }
            item {
                SectionHeader("About")
                ListItem(headlineContent = { Text("Version") }, supportingContent = { Text(ProductSurface.LEAN_VERSION) })
                ListItem(headlineContent = { Text("Puzzles") }, supportingContent = { Text(state.puzzleCount.toString()) })
                ListItem(
                    headlineContent = { Text("Replay onboarding") },
                    modifier = Modifier
                        .clickable {
                            viewModel.replayOnboarding()
                            onReplayOnboarding()
                        }
                        .fillMaxWidth(),
                )
            }
        }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp, 8.dp))
}

@Composable
private fun LegalLink(label: String, url: String) {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(label) },
        modifier = Modifier
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .fillMaxWidth(),
    )
}
