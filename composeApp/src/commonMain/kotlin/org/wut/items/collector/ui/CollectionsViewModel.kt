package org.wut.items.collector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wut.items.collector.AppContainer
import org.wut.items.collector.data.SyncState
import org.wut.items.collector.network.ConnectivityStatus
import org.wut.items.collector.model.AttributeDef
import org.wut.items.collector.model.CollectionDto
import org.wut.items.collector.model.CollectionPreset
import org.wut.items.collector.model.CollectionPresets

class CollectionsViewModel(private val app: AppContainer) : ViewModel() {

    enum class SortBy { UPDATED, NAME }

    val collections: StateFlow<List<CollectionDto>> = run {
        val flow = MutableStateFlow<List<CollectionDto>>(emptyList())
        viewModelScope.launch {
            app.collectionRepo.observeAll().collect { flow.value = it }
        }
        flow.asStateFlow()
    }

    




    val itemCounts: StateFlow<Map<String, Int>> = run {
        val flow = MutableStateFlow<Map<String, Int>>(emptyMap())
        viewModelScope.launch {
            app.itemRepo.observeCounts().collect { flow.value = it }
        }
        flow.asStateFlow()
    }

    private val _sortBy = MutableStateFlow(SortBy.UPDATED)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _sortAsc = MutableStateFlow(false)
    val sortAsc: StateFlow<Boolean> = _sortAsc.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    




    val syncState: StateFlow<SyncState> = app.syncEngine.state

    
    val connectivityStatus: StateFlow<ConnectivityStatus> = 
        app.syncEngine.connectivity ?: MutableStateFlow(ConnectivityStatus.Available)

    val availablePresets: StateFlow<List<CollectionPreset>> = run {
        val flow = MutableStateFlow<List<CollectionPreset>>(emptyList())
        viewModelScope.launch {
            app.presetRepo.observeAll().collect { flow.value = it }
        }
        flow.asStateFlow()
    }

    
    val isOfflineMode: Boolean get() = app.sessionStore.isOffline()

    val serverBaseUrl: String get() = app.sessionStore.serverUrl()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }

    fun setSortBy(s: SortBy) {
        if (_sortBy.value == s) {
            _sortAsc.value = !_sortAsc.value
        } else {
            _sortBy.value = s
            _sortAsc.value = if (s == SortBy.NAME) true else false
        }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun saveAsPreset(name: String, description: String, schema: List<AttributeDef>) {
        viewModelScope.launch {
            app.presetRepo.saveAsPreset(name, description, schema)
            _uiEvent.emit(UiEvent.ShowSnackbar("Utworzono szablon: $name"))
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch {
            app.presetRepo.deletePreset(id)
        }
    }

    fun createCollection(name: String, description: String, schema: List<AttributeDef>, bannerImageUrl: String? = null, bannerAlignment: Float = 0.5f, pendingBannerPath: String? = null) {
        viewModelScope.launch {
            app.collectionRepo.create(name, description, schema, bannerImageUrl, bannerAlignment, pendingBannerPath)
            _uiEvent.emit(UiEvent.ShowSnackbar("Kolekcja zapisana lokalnie. Użyj synchronizacji, aby wysłać zmiany."))
        }
    }

    fun updateCollection(id: String, name: String, description: String, schema: List<AttributeDef>, bannerImageUrl: String? = null, bannerAlignment: Float = 0.5f, pendingBannerPath: String? = null) {
        viewModelScope.launch {
            app.collectionRepo.update(id, name, description, schema, bannerImageUrl, bannerAlignment, pendingBannerPath)
            _uiEvent.emit(UiEvent.ShowSnackbar("Zmiany zapisane lokalnie. Użyj synchronizacji, aby wysłać je na serwer."))
        }
    }

    fun getCollection(id: String): CollectionDto? {
        return app.collectionRepo.getById(id)
    }

    fun deleteCollection(id: String) {
        viewModelScope.launch {
            app.collectionRepo.delete(id)
            _uiEvent.emit(UiEvent.ShowSnackbar("Kolekcja oznaczona do usunięcia. Użyj synchronizacji, aby wysłać zmiany."))
        }
    }

    fun sync() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        viewModelScope.launch {
            app.syncEngine.syncAll()
            _isSyncing.value = false
        }
    }
}
