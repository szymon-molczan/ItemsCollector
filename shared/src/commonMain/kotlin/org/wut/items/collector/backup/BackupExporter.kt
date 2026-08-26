package org.wut.items.collector.backup

import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import org.wut.items.collector.data.CollectionRepository
import org.wut.items.collector.data.ItemImageRepository
import org.wut.items.collector.data.ItemRepository
import org.wut.items.collector.data.SessionStore
import org.wut.items.collector.model.ItemImageDto
import org.wut.items.collector.network.ApiClient
import org.wut.items.collector.util.currentTimeMillis























class BackupExporter(
    private val collectionRepo: CollectionRepository,
    private val itemRepo: ItemRepository,
    private val itemImageRepo: ItemImageRepository,
    private val sessionStore: SessionStore,
    private val apiClient: ApiClient,
    private val fileSystem: FileSystem,
    private val zipBuilder: ZipBuilder,
    private val backupPaths: BackupPaths
) {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    




    data class Result(
        val path: String,
        val collectionsExported: Int,
        val itemsExported: Int,
        val imagesIncluded: Int,
        val imagesFailed: Int
    )

    



    suspend fun exportCollections(collectionIds: Set<String>): Result {
        require(collectionIds.isNotEmpty()) { "Lista kolekcji do eksportu jest pusta" }

        val allCollections = collectionRepo.observeAll().first()
        val selected = allCollections.filter { it.id in collectionIds }

        
        val allItems = mutableListOf<Pair<String, org.wut.items.collector.model.ItemDto>>()
        for (coll in selected) {
            val items = itemRepo.observeByCollection(coll.id).first()
            items.forEach { allItems += coll.id to it }
        }

        
        val outPath = backupPaths.newBackupFile()
        zipBuilder.open(outPath)

        var imagesIncluded = 0
        var imagesFailed = 0
        val backupItems = mutableListOf<BackupItem>()

        try {
            
            val backupCollections = mutableListOf<BackupCollection>()
            for (coll in selected) {
                var bannerFileName: String? = null
                val bannerBytes = fetchBannerBytes(coll)
                if (bannerBytes != null) {
                    bannerFileName = "banner-${coll.id}.jpg"
                    zipBuilder.addEntry("${BackupConstants.IMAGES_DIR}/$bannerFileName", bannerBytes)
                    imagesIncluded++
                } else if (coll.bannerImageUrl != null || coll.pendingBannerPath != null) {
                    imagesFailed++
                }

                backupCollections += BackupCollection(
                    id = coll.id,
                    name = coll.name,
                    description = coll.description,
                    schema = coll.schema,
                    bannerFileName = bannerFileName,
                    bannerAlignment = coll.bannerAlignment,
                    createdAt = coll.createdAt,
                    updatedAt = coll.updatedAt
                )
            }

            for ((_, item) in allItems) {
                
                
                val galleryImages = itemImageRepo.listByItem(item.id)
                val backupImages = mutableListOf<BackupImage>()
                var primaryFileName: String? = null

                if (galleryImages.isNotEmpty()) {
                    for (img in galleryImages) {
                        val bytes = fetchImageBytesForGallery(img)
                        if (bytes == null) {
                            imagesFailed++
                            continue
                        }
                        
                        
                        val fileName = if (img.isPrimary) "${item.id}.jpg" else "${item.id}-${img.id}.jpg"
                        zipBuilder.addEntry("${BackupConstants.IMAGES_DIR}/$fileName", bytes)
                        imagesIncluded++
                        if (img.isPrimary) primaryFileName = fileName
                        backupImages += BackupImage(
                            id = img.id,
                            fileName = fileName,
                            isPrimary = img.isPrimary,
                            sortOrder = img.sortOrder,
                            createdAt = img.createdAt,
                            updatedAt = img.updatedAt
                        )
                    }
                } else {
                    
                    val imageBytes = fetchImageBytes(item)
                    if (imageBytes != null) {
                        val name = "${item.id}.jpg"
                        zipBuilder.addEntry("${BackupConstants.IMAGES_DIR}/$name", imageBytes)
                        imagesIncluded++
                        primaryFileName = name
                    } else if (item.imageUrl != null || item.pendingImagePath != null) {
                        imagesFailed++
                    }
                }

                backupItems += BackupItem(
                    id = item.id,
                    collectionId = item.collectionId,
                    name = item.name,
                    description = item.description,
                    attributes = item.attributes,
                    isFavorite = item.isFavorite,
                    imageFileName = primaryFileName,
                    images = backupImages,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt
                )
            }

            val backup = BackupFile(
                version = BackupConstants.CURRENT_VERSION,
                exportedAt = currentTimeMillis(),
                exportedBy = sessionStore.state.value?.email.orEmpty(),
                collections = backupCollections,
                items = backupItems
            )
            val jsonBytes = json.encodeToString(backup).encodeToByteArray()
            zipBuilder.addEntry(BackupConstants.DATA_JSON, jsonBytes)
        } finally {
            zipBuilder.close()
        }

        return Result(
            path = outPath,
            collectionsExported = selected.size,
            itemsExported = backupItems.size,
            imagesIncluded = imagesIncluded,
            imagesFailed = imagesFailed
        )
    }

    






    private suspend fun fetchImageBytes(
        item: org.wut.items.collector.model.ItemDto
    ): ByteArray? {
        
        item.pendingImagePath?.let { path ->
            try {
                val okioPath = path.toPath()
                if (fileSystem.exists(okioPath)) {
                    return fileSystem.read(okioPath) { readByteArray() }
                }
            } catch (_: Throwable) {  }
        }
        
        val url = item.imageUrl ?: return null
        return downloadIfRemote(url)
    }

    private suspend fun fetchBannerBytes(
        coll: org.wut.items.collector.model.CollectionDto
    ): ByteArray? {
        
        coll.pendingBannerPath?.let { path ->
            try {
                val okioPath = path.toPath()
                if (fileSystem.exists(okioPath)) {
                    return fileSystem.read(okioPath) { readByteArray() }
                }
            } catch (_: Throwable) {  }
        }
        
        val url = coll.bannerImageUrl ?: return null
        return downloadIfRemote(url)
    }

    
    private suspend fun fetchImageBytesForGallery(img: ItemImageDto): ByteArray? {
        img.pendingImagePath?.let { path ->
            try {
                val okioPath = path.toPath()
                if (fileSystem.exists(okioPath)) {
                    return fileSystem.read(okioPath) { readByteArray() }
                }
            } catch (_: Throwable) {  }
        }
        val url = img.imageUrl ?: return null
        return downloadIfRemote(url)
    }

    private suspend fun downloadIfRemote(url: String): ByteArray? {
        val absoluteUrl = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("/") -> "${apiClient.baseUrl()}$url"
            else -> return null
        }
        return try {
            apiClient.downloadBytes(absoluteUrl)
        } catch (_: Throwable) {
            null
        }
    }
}
