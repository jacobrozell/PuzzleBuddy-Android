package com.jacobrozell.puzzlebuddy.ui.barcode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle

sealed interface ShoppingScanResult {
    data class Match(val puzzle: Puzzle) : ShoppingScanResult
    data class NoMatch(val barcode: String) : ShoppingScanResult
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingModeScreen(
    onDismiss: () -> Unit,
    onOpenPuzzle: (String) -> Unit,
    onAddPuzzle: (String) -> Unit,
    viewModel: ShoppingModeViewModel = hiltViewModel(),
) {
    val scanResult by viewModel.scanResult.collectAsStateWithLifecycle()
    var scannerKey by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check duplicate") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (scanResult == null) {
                key(scannerKey) {
                    BarcodeScannerContent(
                        onBarcodeScanned = viewModel::handleScan,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            scanResult?.let { result ->
                ShoppingResultCard(
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
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(androidx.compose.ui.Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun ShoppingResultCard(
    result: ShoppingScanResult,
    onOpenPuzzle: (Puzzle) -> Unit,
    onAddPuzzle: (String) -> Unit,
    onScanAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (result) {
                is ShoppingScanResult.Match -> {
                    Text("Already in your collection", style = MaterialTheme.typography.titleMedium)
                    Text(result.puzzle.name, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { onOpenPuzzle(result.puzzle) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open puzzle")
                    }
                }
                is ShoppingScanResult.NoMatch -> {
                    Text("Not in your collection", style = MaterialTheme.typography.titleMedium)
                    Text("Barcode ${result.barcode}", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { onAddPuzzle(result.barcode) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Add puzzle")
                    }
                }
            }
            OutlinedButton(onClick = onScanAnother, modifier = Modifier.fillMaxWidth()) {
                Text("Scan another")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerSheet(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan barcode") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
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
