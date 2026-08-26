package org.wut.items.collector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wut.items.collector.AppContainer
import org.wut.items.collector.backup.BackupExporter
import org.wut.items.collector.model.CollectionDto
















class BackupExportViewModel(private val app: AppContainer) : ViewModel() {

    sealed class ExportState {
        object Idle : ExportState()
        object InProgress : ExportState()
        data class Done(val result: BackupExporter.Result) : ExportState()
        data class Error(val message: String) : ExportState()
    }

    val collections: StateFlow<List<CollectionDto>> = run {
        val flow = MutableStateFlow<List<CollectionDto>>(emptyList())
        viewModelScope.launch {
            app.collectionRepo.observeAll().collect { flow.value = it }
        }
        flow.asStateFlow()
    }

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun toggle(id: String) {
        _selectedIds.value = _selectedIds.value.toMutableSet().also {
            if (id in it) it.remove(id) else it.add(id)
        }
    }

    fun selectAll() {
        _selectedIds.value = collections.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    




    fun runExport() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) {
            _exportState.value = ExportState.Error("Zaznacz przynajmniej jedną kolekcję")
            return
        }
        _exportState.value = ExportState.InProgress
        viewModelScope.launch {
            val result = try {
                app.backupExporter.exportCollections(ids)
            } catch (t: Throwable) {
                _exportState.value = ExportState.Error(
                    "Eksport nie powiódł się: ${t.message ?: t::class.simpleName}"
                )
                return@launch
            }
            _exportState.value = ExportState.Done(result)
        }
    }

    
    fun share(path: String) {
        app.fileSharer.share(path, "application/zip", "Udostępnij kopię zapasową")
    }

    
    fun resetState() {
        _exportState.value = ExportState.Idle
    }
}
