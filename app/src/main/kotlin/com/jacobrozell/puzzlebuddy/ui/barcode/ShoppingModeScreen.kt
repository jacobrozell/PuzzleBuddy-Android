package com.jacobrozell.puzzlebuddy.ui.barcode

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.ui.theme.BrandTokens

sealed interface ShoppingScanResult {
    data class Match(val puzzle: Puzzle, val barcode: String) : ShoppingScanResult
    data class NoMatch(val barcode: String) : ShoppingScanResult
}

/** Duplicate check while shopping, from iOS `ShoppingModeView`. Never adds anything by itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingModeScreen(
    onDismiss: () -> Unit,
    onOpenPuzzle: (String) -> Unit,
    onAddPuzzle: (String) -> Unit,
    viewModel: ShoppingModeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var scannerKey by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check duplicate") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Cancel shopping mode") }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.result == null && !state.busy) {
                key(scannerKey) {
                    BarcodeScannerContent(
                        onBarcodeScanned = { raw -> viewModel.handleScan(raw, context as? Activity) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (state.busy && state.result == null) {
                state.pendingBarcode?.let { barcode ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Checking this barcode", style = MaterialTheme.typography.titleMedium)
                        ScannedBarcode(barcode)
                    }
                }
            }
            if (state.scansTotal > 0) {
                ShoppingSessionBanner(
                    scansTotal = state.scansTotal,
                    duplicatesFound = state.duplicatesFound,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                )
            }
            state.result?.let { result ->
                BarcodeScanResultCard(
                    result = result,
                    onOpenPuzzle = { puzzle ->
                        onDismiss()
                        onOpenPuzzle(puzzle.id)
                    },
                    onAddPuzzle = { barcode ->
                        onDismiss()
                        onAddPuzzle(barcode)
                    },
                    onScanAnother = {
                        viewModel.clearResult()
                        scannerKey++
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }
}

/** Result of one scan, from iOS `BarcodeScanResultCard`. [onScanAnother] null hides that button. */
@Composable
fun BarcodeScanResultCard(
    result: ShoppingScanResult,
    onOpenPuzzle: (Puzzle) -> Unit,
    onAddPuzzle: (String) -> Unit,
    onScanAnother: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val brand = BrandTokens.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = brand.card),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (result) {
                is ShoppingScanResult.Match -> {
                    ResultHeading("You already own this", Icons.Filled.CheckCircle, brand.accentText)
                    Text(result.puzzle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    result.puzzle.pieces?.let { Text("$it pieces", color = brand.textSecondary) }
                    ScannedBarcode(result.barcode)
                    Button(onClick = { onOpenPuzzle(result.puzzle) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Open puzzle")
                    }
                }
                is ShoppingScanResult.NoMatch -> {
                    ResultHeading("Not in your collection", Icons.Filled.AddShoppingCart, brand.accentWarmText)
                    Text("No puzzle in your collection uses this barcode.", color = brand.textSecondary)
                    ScannedBarcode(result.barcode)
                    AddScannedPuzzleButton(result.barcode, onAddPuzzle)
                }
            }
            onScanAnother?.let {
                OutlinedButton(
                    onClick = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "Scan another barcode" },
                ) { Text("Scan another") }
            }
        }
    }
}

@Composable
private fun ShoppingSessionBanner(scansTotal: Int, duplicatesFound: Int, modifier: Modifier = Modifier) {
    val newCount = scansTotal - duplicatesFound
    Text(
        "$scansTotal scanned · $duplicatesFound duplicate${if (duplicatesFound == 1) "" else "s"} · $newCount new",
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics {
                contentDescription =
                    "$scansTotal barcodes scanned, $duplicatesFound duplicates, $newCount not in collection"
            },
    )
}

@Composable
private fun ScannedBarcode(barcode: String) {
    Text(
        barcode,
        style = MaterialTheme.typography.titleMedium,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BrandTokens.colors.cardElevated)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = "Barcode $barcode" },
    )
}

@Composable
private fun AddScannedPuzzleButton(barcode: String, onAddPuzzle: (String) -> Unit) {
    Button(
        onClick = { onAddPuzzle(barcode) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { contentDescription = "Add puzzle with barcode $barcode" },
    ) {
        Text("Add this puzzle")
    }
}

@Composable
private fun ResultHeading(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) { heading() },
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(text, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerSheet(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Boolean,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan barcode") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
                },
            )
        },
    ) { padding ->
        BarcodeScannerContent(
            onBarcodeScanned = onBarcodeScanned,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
