package com.jacobrozell.puzzlebuddy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jacobrozell.puzzlebuddy.data.prefs.AppPreferencesStore
import com.jacobrozell.puzzlebuddy.data.prefs.OnboardingStore
import com.jacobrozell.puzzlebuddy.data.repository.PuzzleRepository
import com.jacobrozell.puzzlebuddy.domain.importing.PuzzleImportSummary
import com.jacobrozell.puzzlebuddy.domain.export.PuzzleCollectionExportFormat
import com.jacobrozell.puzzlebuddy.domain.export.PuzzleCollectionExporter
import com.jacobrozell.puzzlebuddy.support.ExportShareHelper
import android.content.Context
import com.jacobrozell.puzzlebuddy.domain.model.Puzzle
import com.jacobrozell.puzzlebuddy.support.logging.AppLogger
import com.jacobrozell.puzzlebuddy.support.logging.LogCategory
import com.jacobrozell.puzzlebuddy.support.logging.error
import com.jacobrozell.puzzlebuddy.support.logging.info
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val puzzleCount: Int = 0,
    val demoCount: Int = 0,
    val appearanceMode: String = "system",
    val barcodeLookupEnabled: Boolean = false,
    val isBusy: Boolean = false,
    val importSummary: PuzzleImportSummary? = null,
    val errorMessage: String? = null,
    val exportErrorMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PuzzleRepository,
    private val appPreferences: AppPreferencesStore,
    private val onboardingStore: OnboardingStore,
    private val logger: AppLogger,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val importSummary = MutableStateFlow<PuzzleImportSummary?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val exportErrorMessage = MutableStateFlow<String?>(null)
    private val demoCount = MutableStateFlow(0)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.observePuzzles(),
        appPreferences.appearanceMode,
        appPreferences.isBarcodeLookupEnabled,
        busy,
        importSummary,
        errorMessage,
        exportErrorMessage,
        demoCount,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val puzzles = values[0] as List<Puzzle>
        SettingsUiState(
            puzzleCount = puzzles.size,
            demoCount = values[7] as Int,
            appearanceMode = values[1] as String,
            barcodeLookupEnabled = values[2] as Boolean,
            isBusy = values[3] as Boolean,
            importSummary = values[4] as PuzzleImportSummary?,
            errorMessage = values[5] as String?,
            exportErrorMessage = values[6] as String?,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        refreshDemoCount()
    }

    fun setAppearance(mode: String) {
        viewModelScope.launch { appPreferences.setAppearanceMode(mode) }
    }

    fun setBarcodeLookupEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setBarcodeLookupEnabled(enabled) }
    }

    fun exportCollection(format: PuzzleCollectionExportFormat) {
        viewModelScope.launch {
            exportErrorMessage.value = null
            try {
                val puzzles = repository.observePuzzles().first()
                val data = when (format) {
                    PuzzleCollectionExportFormat.JSON -> PuzzleCollectionExporter.jsonData(puzzles)
                    PuzzleCollectionExportFormat.CSV -> PuzzleCollectionExporter.csvData(puzzles)
                }
                ExportShareHelper.share(
                    context = context,
                    data = data,
                    format = format,
                    fileName = PuzzleCollectionExporter.fileName(format),
                )
                logger.info(
                    LogCategory.PUZZLES,
                    eventName = "settings_collection_exported",
                    message = "Collection exported.",
                    metadata = mapOf(
                        "format" to format.extension,
                        "puzzle_count" to puzzles.size.toString(),
                    ),
                )
            } catch (error: Exception) {
                exportErrorMessage.value = error.message ?: "Export failed"
                logger.error(
                    LogCategory.PUZZLES,
                    eventName = "collection_export_failed",
                    message = error.message ?: "Export failed",
                    metadata = mapOf("format" to format.extension),
                )
            }
        }
    }

    fun dismissExportError() {
        exportErrorMessage.value = null
    }

    fun replayOnboarding() {
        onboardingStore.requestReplay()
    }

    fun loadDemoData() {
        viewModelScope.launch {
            busy.value = true
            repository.loadDemoPuzzles()
            refreshDemoCount()
            busy.value = false
        }
    }

    fun removeDemoData() {
        viewModelScope.launch {
            busy.value = true
            repository.removeDemoPuzzles()
            refreshDemoCount()
            busy.value = false
        }
    }

    fun clearCollection() {
        viewModelScope.launch {
            busy.value = true
            repository.clearAll()
            refreshDemoCount()
            busy.value = false
        }
    }

    fun importCsv(data: ByteArray) {
        viewModelScope.launch {
            busy.value = true
            errorMessage.value = null
            try {
                val summary = repository.importIpdbCsv(data)
                importSummary.value = summary
                refreshDemoCount()
                if (summary.imported > 0) {
                    logger.info(
                        LogCategory.PUZZLES,
                        eventName = "puzzle_import_completed",
                        message = "Imported puzzles from file.",
                        metadata = mapOf(
                            "puzzle_count" to summary.imported.toString(),
                            "puzzle_status" to "imported",
                        ),
                    )
                }
            } catch (error: Exception) {
                errorMessage.value = error.message ?: "Import failed"
                logger.error(
                    LogCategory.PUZZLES,
                    eventName = "ipdb_import_failed",
                    message = error.message ?: "Import failed",
                    metadata = mapOf("operation" to "ipdb_csv"),
                )
            } finally {
                busy.value = false
            }
        }
    }

    fun dismissImportSummary() {
        importSummary.value = null
    }

    fun dismissError() {
        errorMessage.value = null
    }

    private fun refreshDemoCount() {
        viewModelScope.launch {
            demoCount.value = repository.demoCount()
        }
    }
}
