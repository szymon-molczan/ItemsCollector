package org.wut.items.collector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import org.wut.items.collector.AppContainer
import org.wut.items.collector.model.AttributeType
import org.wut.items.collector.model.AttributeValue
import org.wut.items.collector.model.CollectionDto
import org.wut.items.collector.model.ItemDto
import org.wut.items.collector.model.ItemImageDto
import org.wut.items.collector.media.MediaResult
import org.wut.items.collector.media.MediaOptimizationOptions
import org.wut.items.collector.pdf.PrimaryImageRef
import kotlinx.coroutines.flow.Flow

class CollectionDetailViewModel(
    private val app: AppContainer,
    private val collectionId: String
) : ViewModel() {

    enum class SortBy { UPDATED, NAME, ATTRIBUTE, FAVORITE }
    enum class ViewMode { LIST, GALLERY }

    private val _collection = MutableStateFlow<CollectionDto?>(null)
    val collection: StateFlow<CollectionDto?> = _collection.asStateFlow()

    private val _items = MutableStateFlow<List<ItemDto>>(emptyList())
    val items: StateFlow<List<ItemDto>> = _items.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.UPDATED)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _sortAttributeKey = MutableStateFlow<String?>(null)
    val sortAttributeKey: StateFlow<String?> = _sortAttributeKey.asStateFlow()

    




    private val _sortAsc = MutableStateFlow(false)
    val sortAsc: StateFlow<Boolean> = _sortAsc.asStateFlow()

    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    
    private val _searchAttributeKey = MutableStateFlow<String?>(null)
    val searchAttributeKey: StateFlow<String?> = _searchAttributeKey.asStateFlow()

    
    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites: StateFlow<Boolean> = _onlyFavorites.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    
    private val _gridColumns = MutableStateFlow(2)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    init {
        viewModelScope.launch {
            app.collectionRepo.observeById(collectionId).collect { _collection.value = it }
        }
        viewModelScope.launch {
            app.itemRepo.observeByCollection(collectionId).collect { _items.value = it }
        }
    }

    fun setSortBy(s: SortBy, attrKey: String? = null) {
        _sortBy.value = s
        _sortAttributeKey.value = if (s == SortBy.ATTRIBUTE) attrKey else null
        
        _sortAsc.value = when (s) {
            SortBy.UPDATED, SortBy.FAVORITE -> false
            SortBy.NAME, SortBy.ATTRIBUTE -> true
        }
    }

    fun toggleSortDirection() { _sortAsc.value = !_sortAsc.value }

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun setSearchAttribute(key: String?) { _searchAttributeKey.value = key }

    fun toggleOnlyFavorites() { _onlyFavorites.value = !_onlyFavorites.value }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.LIST) ViewMode.GALLERY else ViewMode.LIST
    }

    fun setGridColumns(columns: Int) {
        _gridColumns.value = columns.coerceIn(1, 5)
    }

    




    fun displayItems(): List<ItemDto> {
        val q = _searchQuery.value.trim()
        val attrKey = _searchAttributeKey.value
        val onlyFav = _onlyFavorites.value

        var filtered = if (q.isEmpty()) {
            _items.value
        } else {
            _items.value.filter { item ->
                if (attrKey == null) {
                    item.name.contains(q, ignoreCase = true)
                } else {
                    item.attributes.firstOrNull { it.key == attrKey }?.value?.contains(q, ignoreCase = true) == true
                }
            }
        }

        if (onlyFav) {
            filtered = filtered.filter { it.isFavorite }
        }

        val sorted: List<ItemDto> = when (_sortBy.value) {
            SortBy.FAVORITE -> filtered.sortedBy { it.isFavorite }
            SortBy.UPDATED -> filtered.sortedBy { it.updatedAt }
            SortBy.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortBy.ATTRIBUTE -> {
                val key = _sortAttributeKey.value ?: return filtered
                val type = _collection.value?.schema?.firstOrNull { it.key == key }?.type
                    ?: AttributeType.TEXT
                when (type) {
                    AttributeType.NUMBER -> filtered.sortedBy {
                        it.attributes.firstOrNull { a -> a.key == key }?.value?.toDoubleOrNull()
                            ?: Double.MAX_VALUE  
                    }
                    AttributeType.DATE -> filtered.sortedBy {
                        it.attributes.firstOrNull { a -> a.key == key }?.value?.toLongOrNull()
                            ?: Long.MAX_VALUE
                    }
                    AttributeType.BOOLEAN, AttributeType.TEXT, AttributeType.SELECT ->
                        filtered.sortedBy {
                            it.attributes.firstOrNull { a -> a.key == key }?.value?.lowercase().orEmpty()
                        }
                }
            }
        }
        return if (_sortAsc.value) sorted else sorted.asReversed()
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            app.itemRepo.delete(id)
            _uiMessages.emit("Przedmiot oznaczony do usunięcia. Użyj synchronizacji, aby wysłać zmiany.")
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            app.itemRepo.toggleFavorite(id)
            _uiMessages.emit("Zmiana zapisana lokalnie. Użyj synchronizacji, aby wysłać ją na serwer.")
        }
    }

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _bulkImportState = MutableStateFlow<BulkImportState>(BulkImportState.Idle)
    val bulkImportState: StateFlow<BulkImportState> = _bulkImportState.asStateFlow()
    private var pendingBulkImages: List<MediaResult> = emptyList()

    sealed class ExportState {
        object Idle : ExportState()
        object InProgress : ExportState()
        data class Done(val path: String) : ExportState()
        data class Error(val message: String) : ExportState()
    }

    sealed class BulkImportState {
        object Idle : BulkImportState()
        object Picking : BulkImportState()
        data class Confirming(val count: Int) : BulkImportState()
        data class Importing(val total: Int) : BulkImportState()
    }

    
    fun exportToPdf() {
        viewModelScope.launch {
            val coll = _collection.value ?: return@launch
            _exportState.value = ExportState.InProgress
            try {
                
                
                val primaryProvider: (String) -> PrimaryImageRef? = { itemId ->
                    app.itemImageRepo.getPrimaryByItem(itemId)?.let { dto ->
                        PrimaryImageRef(
                            pendingImagePath = dto.pendingImagePath,
                            imageUrl = dto.imageUrl
                        )
                    }
                }
                val path = app.pdfExporter.export(coll, _items.value, primaryProvider)
                _exportState.value = ExportState.Done(path)
                app.fileSharer.share(path, "application/pdf", "Udostępnij PDF kolekcji")
            } catch (t: Throwable) {
                _exportState.value = ExportState.Error(t.message ?: "Nieznany błąd eksportu")
            }
        }
    }

    fun clearExportState() { _exportState.value = ExportState.Idle }

    



    fun importImagesAsItems() {
        viewModelScope.launch {
            _bulkImportState.value = BulkImportState.Picking
            try {
                val selected = app.mediaPicker.pickMultipleFromGallery()
                if (selected.isEmpty()) {
                    _bulkImportState.value = BulkImportState.Idle
                    return@launch
                }

                pendingBulkImages = selected
                _bulkImportState.value = BulkImportState.Confirming(selected.size)
            } catch (t: Throwable) {
                _bulkImportState.value = BulkImportState.Idle
                pendingBulkImages = emptyList()
                _uiMessages.emit("Nie udało się wybrać zdjęć: ${t.message ?: "błąd"}")
            }
        }
    }

    fun confirmBulkImageImport(useFileNames: Boolean, optimizeImages: Boolean) {
        viewModelScope.launch {
            val selected = pendingBulkImages
            if (selected.isEmpty()) {
                _bulkImportState.value = BulkImportState.Idle
                return@launch
            }

            _bulkImportState.value = BulkImportState.Importing(selected.size)
            try {
                val imagesToImport = if (optimizeImages) {
                    selected.map { media ->
                        app.mediaPicker.optimizeForImport(
                            media,
                            MediaOptimizationOptions(maxDimension = 2000, jpegQuality = 85)
                        )
                    }
                } else {
                    selected
                }

                app.database.itemsQueries.transaction {
                    imagesToImport.forEachIndexed { index, media ->
                        val item = app.itemRepo.create(
                            collectionId = collectionId,
                            name = draftName(media, index, useFileNames),
                            description = "",
                            imageUrl = null,
                            pendingImagePath = null,
                            attributes = emptyList()
                        )
                        app.itemImageRepo.add(
                            itemId = item.id,
                            imageUrl = null,
                            pendingImagePath = media.localPath
                        )
                    }
                }
                pendingBulkImages = emptyList()
                _bulkImportState.value = BulkImportState.Idle
                _uiMessages.emit("Dodano ${selected.size} zdjęć jako nowe pozycje. Użyj synchronizacji, aby wysłać je na serwer.")
            } catch (t: Throwable) {
                _bulkImportState.value = BulkImportState.Idle
                pendingBulkImages = emptyList()
                _uiMessages.emit("Nie udało się zaimportować zdjęć: ${t.message ?: "błąd"}")
            }
        }
    }

    fun cancelBulkImageImport() {
        pendingBulkImages = emptyList()
        _bulkImportState.value = BulkImportState.Idle
    }

    private fun draftName(media: MediaResult, index: Int, useFileNames: Boolean): String {
        if (!useFileNames) return numberedDraftName(index)
        val displayName = media.displayName
        val fromFile = displayName
            ?.substringBeforeLast('.', missingDelimiterValue = displayName)
            ?.replace(Regex("[_\\-]+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        return fromFile ?: numberedDraftName(index)
    }

    private fun numberedDraftName(index: Int): String =
        "Zdjęcie ${(index + 1).toString().padStart(3, '0')}"

    




    private val _uiMessages = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 4)
    val uiMessages: SharedFlow<String> = _uiMessages.asSharedFlow()

    
    val serverBaseUrl: String get() = app.apiClient.baseUrl()

    












    suspend fun prepareImageForEdit(
        pendingImagePath: String?,
        imageUrl: String?
    ): String? {
        
        if (pendingImagePath != null) return pendingImagePath
        if (imageUrl == null) return null
        
        val absoluteUrl = when {
            imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> imageUrl
            imageUrl.startsWith("/") -> "${app.apiClient.baseUrl()}$imageUrl"
            else -> return null
        }
        return try {
            val bytes = app.apiClient.downloadBytes(absoluteUrl)
            val targetPath = app.importPaths.newImageFile()
            val path = targetPath.toPath()
            app.fileSystem.sink(path).buffer().use { it.write(bytes) }
            targetPath
        } catch (t: Throwable) {
            _uiMessages.emit("Nie udało się pobrać zdjęcia do edycji: ${t.message ?: "błąd"}")
            null
        }
    }

    

    
    fun observeImages(itemId: String): Flow<List<ItemImageDto>> =
        app.itemImageRepo.observeByItem(itemId)

    



    fun addImageToGallery(itemId: String, pendingImagePath: String) {
        viewModelScope.launch {
            app.itemImageRepo.add(itemId = itemId, imageUrl = null, pendingImagePath = pendingImagePath)
            _uiMessages.emit("Zdjęcie zapisane lokalnie. Użyj synchronizacji, aby wysłać je na serwer.")
        }
    }

    
    fun deleteImage(imageId: String) {
        viewModelScope.launch {
            app.itemImageRepo.delete(imageId)
            _uiMessages.emit("Zdjęcie oznaczone do usunięcia. Użyj synchronizacji, aby wysłać zmiany.")
        }
    }

    
    fun setPrimaryImage(imageId: String) {
        viewModelScope.launch {
            app.itemImageRepo.setPrimary(imageId)
            _uiMessages.emit("Zdjęcie główne zmienione lokalnie. Użyj synchronizacji, aby wysłać zmiany.")
        }
    }

    
    val primaryImagesByItem: Flow<Map<String, org.wut.items.collector.data.ItemImageRepository.PrimaryImage>> =
        app.itemImageRepo.observePrimaryUrlsForCollection(collectionId)

    fun saveItem(
        id: String,
        name: String,
        description: String,
        attributes: List<AttributeValue>,
        isNew: Boolean
    ) {
        viewModelScope.launch {
            if (isNew) {
                app.itemRepo.create(
                    collectionId = collectionId,
                    name = name,
                    description = description,
                    imageUrl = null,
                    pendingImagePath = null,
                    attributes = attributes,
                    forcedId = id
                )
            } else {
                app.itemRepo.update(
                    id = id,
                    name = name,
                    description = description,
                    imageUrl = null,
                    pendingImagePath = null,
                    attributes = attributes
                )
            }
            _uiMessages.emit("Przedmiot zapisany lokalnie. Użyj synchronizacji, aby wysłać zmiany.")
        }
    }
}
