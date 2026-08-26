package org.wut.items.collector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wut.items.collector.AppContainer
import org.wut.items.collector.backup.BackupImporter













class BackupImportViewModel(private val app: AppContainer) : ViewModel() {

    sealed class State {
        object Idle : State()
        object Picking : State()       
        object Importing : State()      
        data class Done(val result: BackupImporter.Result.Success) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    



    fun pickAndImport() {
        if (_state.value is State.Importing || _state.value is State.Picking) return
        _state.value = State.Picking
        viewModelScope.launch {
            val zipPath = app.filePicker.pickZip()
            if (zipPath == null) {
                _state.value = State.Idle
                return@launch
            }
            _state.value = State.Importing
            val result = app.backupImporter.import(zipPath)
            _state.value = when (result) {
                is BackupImporter.Result.Success -> State.Done(result)
                is BackupImporter.Result.Failure -> State.Error(result.message)
            }
        }
    }

    fun reset() {
        _state.value = State.Idle
    }
}
