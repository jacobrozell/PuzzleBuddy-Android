package com.jacobrozell.puzzlebuddy.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.puzzlebuddy.BuildConfig
import com.jacobrozell.puzzlebuddy.domain.catalog.DemoDataCatalog
import com.jacobrozell.puzzlebuddy.domain.surface.AppLinks
import com.jacobrozell.puzzlebuddy.domain.surface.LegalCopy
import com.jacobrozell.puzzlebuddy.domain.surface.ProductSurface
import com.jacobrozell.puzzlebuddy.ui.designsystem.ReadableContentWidth
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens

private enum class SettingsConfirm { LOAD_DEMO, REMOVE_DEMO, DELETE_ALL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onReplayOnboarding: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirm by remember { mutableStateOf<SettingsConfirm?>(null) }

    confirm?.let { pending ->
        val (title, message, action) = when (pending) {
            SettingsConfirm.LOAD_DEMO -> Triple(
                "Load demo puzzles?",
                "Adds ${DemoDataCatalog.PUZZLE_COUNT} sample puzzles so you can explore the app. Your existing puzzles stay in the collection.",
                "Load Demo Data",
            )
            SettingsConfirm.REMOVE_DEMO -> Triple(
                "Remove demo puzzles?",
                "Removes ${state.demoCount} sample puzzles. Your own puzzles are not affected.",
                "Remove Demo Data",
            )
            SettingsConfirm.DELETE_ALL -> Triple(
                "Delete all data?",
                "This permanently removes every puzzle, photo, completion, and borrower name on this device. This cannot be undone.",
                "Delete All Data",
            )
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    when (pending) {
                        SettingsConfirm.LOAD_DEMO -> viewModel.loadDemoData()
                        SettingsConfirm.REMOVE_DEMO -> viewModel.removeDemoData()
                        SettingsConfirm.DELETE_ALL -> viewModel.deleteAllData()
                    }
                }) {
                    Text(
                        action,
                        color = if (pending == SettingsConfirm.LOAD_DEMO) Color.Unspecified else MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(error.title) },
            text = { Text(error.message) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            ReadableContentWidth(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Header() }
                    item { DisplaySection(state.appearanceMode, viewModel::setAppearance) }
                    item {
                        CollectionSection(
                            state = state,
                            onLoadDemo = { confirm = SettingsConfirm.LOAD_DEMO },
                            onRemoveDemo = { confirm = SettingsConfirm.REMOVE_DEMO },
                            onDeleteAll = { confirm = SettingsConfirm.DELETE_ALL },
                        )
                    }
                    item { HelpSection(context, onWriteReview = viewModel::recordReviewLinkOpened) }
                    item {
                        AboutSection(state) {
                            viewModel.replayOnboarding()
                            onReplayOnboarding()
                        }
                    }
                }
            }
            if (state.isBusy) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Extension, contentDescription = null, tint = BrandTokens.accent, modifier = Modifier.size(56.dp))
        Text("Puzzle Buddy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("Track your puzzle collection", color = BrandTokens.colors.textSecondary)
    }
}

@Composable
private fun DisplaySection(selected: String, onSelect: (String) -> Unit) {
    SectionHeader("Display")
    listOf("system" to "System default", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
        ListItem(
            headlineContent = { Text(label) },
            leadingContent = { RadioButton(selected = selected == value, onClick = null) },
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = selected == value, role = Role.RadioButton) { onSelect(value) },
        )
    }
}

@Composable
private fun CollectionSection(
    state: SettingsUiState,
    onLoadDemo: () -> Unit,
    onRemoveDemo: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    val error = MaterialTheme.colorScheme.error
    SectionHeader("Collection")
    ActionRow("Load Demo Data", Icons.Filled.Inventory2, onClick = onLoadDemo)
    ActionRow(
        "Remove Demo Data",
        Icons.Filled.LayersClear,
        enabled = state.demoCount > 0,
        tint = error,
        supporting = if (state.demoCount == 0) "No demo puzzles to remove" else "${state.demoCount} demo puzzles",
        onClick = onRemoveDemo,
    )
    ActionRow(
        "Delete All Data",
        Icons.Filled.Delete,
        enabled = state.hasDataToDelete,
        tint = error,
        onClick = onDeleteAll,
    )
    SectionFooter("Your collection stays on this device. Demo data is optional sample puzzles for exploring the app.")
}

@Composable
private fun HelpSection(context: Context, onWriteReview: () -> Unit) {
    SectionHeader("Help & Legal")
    if (ProductSurface.isPlayListingLive) {
        LinkRow("Write a Review") {
            onWriteReview()
            openPlayListing(context)
        }
    }
    LinkRow("Privacy Policy") { openUrl(context, AppLinks.privacyPolicy) }
    LinkRow("Support") { openUrl(context, AppLinks.support) }
    LinkRow("Accessibility") { openUrl(context, AppLinks.accessibility) }
    SectionFooter(
        "Barcodes work offline. The barcode icon on your puzzle list checks for duplicates while shopping. " +
            "Scanning to add can suggest details from puzzles you already saved — always review before saving.",
    )
    SectionFooter(LegalCopy.BRAND_TRADEMARK_DISCLAIMER)
}

@Composable
private fun AboutSection(state: SettingsUiState, onReplayOnboarding: () -> Unit) {
    SectionHeader("About")
    ValueRow("Version", BuildConfig.VERSION_NAME)
    ValueRow("Puzzles", state.puzzleCount.toString())
    if (state.demoCount > 0) ValueRow("Demo puzzles", state.demoCount.toString())
    LinkRow("Replay onboarding", onReplayOnboarding)
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = BrandTokens.colors.textSecondary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SectionFooter(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = BrandTokens.colors.textSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun ActionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    supporting: String? = null,
    onClick: () -> Unit,
) {
    val color = if (enabled) tint else BrandTokens.colors.textSecondary.copy(alpha = 0.6f)
    ListItem(
        headlineContent = { Text(title, color = color) },
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = if (color == Color.Unspecified) BrandTokens.colors.accentText else color) },
        colors = ListItemDefaults.colors(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, color = BrandTokens.colors.accentText) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun ValueRow(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Text(value, color = BrandTokens.colors.textSecondary) },
        modifier = Modifier.semantics(mergeDescendants = true) {},
    )
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // No browser installed; nothing to open.
    }
}

private fun openPlayListing(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.playStoreMarket)))
    } catch (_: ActivityNotFoundException) {
        openUrl(context, AppLinks.playStoreWeb)
    }
}
