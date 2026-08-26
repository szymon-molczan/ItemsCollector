package org.wut.items.collector.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import org.wut.items.collector.model.AttributeDef
import org.wut.items.collector.model.AttributeValue
import org.wut.items.collector.model.CreateCollectionRequest
import org.wut.items.collector.model.CreateItemImageRequest
import org.wut.items.collector.model.CreateItemRequest
import org.wut.items.collector.network.ApiClient
import org.wut.items.collector.network.ConnectivityObserver





sealed class SyncState {
    
    object Idle : SyncState()
    
    object Syncing : SyncState()
    





    data class Ok(val imageUploadFailures: Int = 0) : SyncState()
    
    data class Error(val message: String) : SyncState()
}















class SyncEngine(
    private val api: ApiClient,
    private val collections: CollectionRepository,
    private val items: ItemRepository,
    private val itemImages: ItemImageRepository,
    private val fileSystem: FileSystem,
    private val connectivityObserver: ConnectivityObserver? = null,
    private val sessionStore: SessionStore? = null
) {
    private val attrDefSer = ListSerializer(AttributeDef.serializer())
    private val attrValSer = ListSerializer(AttributeValue.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    private val mutex = Mutex()

    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    
    val state: StateFlow<SyncState> = _state.asStateFlow()

    
    val connectivity = connectivityObserver?.status

    







    suspend fun syncAll(): Boolean = mutex.withLock {
        if (sessionStore?.isOffline() == true) {
            println("Sync skipped: offline mode")
            _state.value = SyncState.Ok()
            return true
        }
        _state.value = SyncState.Syncing
        try {
            val imageFailures = pushDirty()
            pullAll()
            _state.value = SyncState.Ok(imageUploadFailures = imageFailures)
            true
        } catch (t: Throwable) {
            println("Sync failed: ${t.message}")
            _state.value = SyncState.Error(t.message ?: "Nieznany błąd sieci")
            false
        }
    }

    
    private suspend fun pushDirty(): Int {
        var imageUploadFailures = 0
        
        val dirtyCollections = collections.allIncludingDirty().filter { it.isDirty == 1L }
        for (c in dirtyCollections) {
            try {
                if (c.isDeleted == 1L) {
                    runCatching { api.deleteCollection(c.id) }
                    collections.deleteHard(c.id)
                    items.deleteByCollection(c.id)
                } else {
                    val schema = if (c.schemaJson.isBlank()) emptyList()
                    else json.decodeFromString(attrDefSer, c.schemaJson)

                    
                    var uploadSuccess = true
                    var uploadError: String? = null
                    val finalBannerUrl = if (c.pendingBannerPath != null) {
                        val uploadResult = runCatching {
                            val path = c.pendingBannerPath!!.toPath()
                            val bytes = fileSystem.read(path) { readByteArray() }
                            api.uploadImage(bytes, path.name).url
                        }
                        uploadResult.exceptionOrNull()?.let { e ->
                            imageUploadFailures++
                            uploadSuccess = false
                            uploadError = e.message
                            println("upload banner for collection ${c.id} failed: ${e.message}")
                        }
                        uploadResult.getOrNull() ?: c.bannerImageUrl
                    } else {
                        c.bannerImageUrl
                    }

                    
                    
                    if (c.pendingBannerPath != null && !uploadSuccess) {
                        println("Skipping collection sync for ${c.id} due to upload error: $uploadError")
                        continue
                    }

                    
                    val serverDto = api.createCollection(
                        CreateCollectionRequest(
                            id = c.id,
                            name = c.name,
                            description = c.description,
                            schema = schema,
                            bannerImageUrl = finalBannerUrl,
                            bannerAlignment = c.bannerAlignment.toFloat()
                        )
                    )
                    
                    collections.replaceFromServer(serverDto)
                }
            } catch (t: Throwable) {
                println("push collection ${c.id} failed: ${t.message}")
            }
        }

        
        val dirtyItems = items.allDirty()
        for (it in dirtyItems) {
            try {
                if (it.isDeleted == 1L) {
                    runCatching { api.deleteItem(it.collectionId, it.id) }
                    items.deleteHard(it.id)
                    
                    itemImages.deleteByItem(it.id)
                } else {
                    
                    
                    
                    
                    val finalImageUrl = if (it.pendingImagePath != null) {
                        val uploadResult = runCatching {
                            val path = it.pendingImagePath!!.toPath()
                            val bytes = fileSystem.read(path) { readByteArray() }
                            api.uploadImage(bytes, path.name).url
                        }
                        uploadResult.exceptionOrNull()?.let { e ->
                            imageUploadFailures++
                            println("upload image for item ${it.id} failed: ${e.message}")
                        }
                        uploadResult.getOrNull() ?: it.imageUrl
                    } else {
                        it.imageUrl
                    }

                    val attrs = if (it.attributesJson.isBlank()) emptyList()
                    else json.decodeFromString(attrValSer, it.attributesJson)

                    val dto = api.createItem(
                        it.collectionId,
                        CreateItemRequest(
                            id = it.id,
                            name = it.name,
                            description = it.description,
                            imageUrl = finalImageUrl,
                            attributes = attrs,
                            isFavorite = it.isFavorite == 1L
                        )
                    )
                    items.replaceFromServer(dto)
                }
            } catch (t: Throwable) {
                println("push item ${it.id} failed: ${t.message}")
            }
        }

        
        
        
        
        
        
        val dirtyImages = itemImages.allDirty()
        for (img in dirtyImages) {
            try {
                
                
                val parent = items.getById(img.itemId)
                if (parent == null) {
                    itemImages.deleteHard(img.id)
                    continue
                }

                if (img.isDeleted == 1L) {
                    runCatching { api.deleteItemImage(parent.collectionId, img.itemId, img.id) }
                    itemImages.deleteHard(img.id)
                    continue
                }

                
                val effectiveUrl: String? = if (img.pendingImagePath != null && img.imageUrl == null) {
                    val uploadResult = runCatching {
                        val path = img.pendingImagePath!!.toPath()
                        val bytes = fileSystem.read(path) { readByteArray() }
                        api.uploadImage(bytes, path.name).url
                    }
                    uploadResult.exceptionOrNull()?.let { e ->
                        imageUploadFailures++
                        println("upload gallery image ${img.id} failed: ${e.message}")
                    }
                    uploadResult.getOrNull()
                } else {
                    img.imageUrl
                }

                
                if (effectiveUrl == null) {
                    
                    continue
                }

                
                val serverDto = api.createItemImage(
                    parent.collectionId,
                    img.itemId,
                    CreateItemImageRequest(
                        id = img.id,
                        imageUrl = effectiveUrl,
                        isPrimary = img.isPrimary == 1L,
                        sortOrder = img.sortOrder.toInt()
                    )
                )
                itemImages.replaceFromServer(serverDto)
            } catch (t: Throwable) {
                println("push item_image ${img.id} failed: ${t.message}")
            }
        }

        return imageUploadFailures
    }

    private suspend fun pullAll() {
        val serverCollections = api.listCollections()

        
        
        
        val serverCollectionIds = serverCollections.map { it.id }.toSet()

        for (c in serverCollections) {
            collections.replaceFromServer(c)
            try {
                val serverItems = api.listItems(c.id)
                val serverItemIds = serverItems.map { it.id }.toSet()
                for (i in serverItems) items.replaceFromServer(i)
                
                
                items.deleteCleanNotIn(c.id, serverItemIds)

                
                
                
                for (i in serverItems) {
                    runCatching {
                        val serverImages = api.listItemImages(c.id, i.id)
                        val serverImageIds = serverImages.map { it.id }.toSet()
                        for (img in serverImages) itemImages.replaceFromServer(img)
                        itemImages.deleteCleanNotIn(i.id, serverImageIds)
                    }.onFailure { t ->
                        println("pull images for item ${i.id} failed: ${t.message}")
                    }
                }
            } catch (t: Throwable) {
                println("pull items for ${c.id} failed: ${t.message}")
            }
        }

        
        collections.deleteCleanNotIn(serverCollectionIds)
    }
}
