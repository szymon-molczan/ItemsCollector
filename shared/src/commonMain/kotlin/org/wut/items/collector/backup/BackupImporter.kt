package org.wut.items.collector.backup

import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import org.wut.items.collector.data.CollectionRepository
import org.wut.items.collector.data.ItemImageRepository
import org.wut.items.collector.data.ItemRepository





















class BackupImporter(
    private val collectionRepo: CollectionRepository,
    private val itemRepo: ItemRepository,
    private val itemImageRepo: ItemImageRepository,
    private val fileSystem: FileSystem,
    private val zipReader: ZipReader,
    private val importPaths: ImportPaths
) {
    private val json = Json { ignoreUnknownKeys = true }

    


    sealed class Result {
        data class Success(
            val collectionsImported: Int,
            val itemsImported: Int,
            val imagesRestored: Int,
            val imagesMissing: Int  
        ) : Result()
        data class Failure(val message: String) : Result()
    }

    





    suspend fun import(zipPath: String): Result {
        try {
            zipReader.open(zipPath)
        } catch (t: Throwable) {
            return Result.Failure("Nie można otworzyć pliku: ${t.message ?: t::class.simpleName}")
        }

        try {
            
            val jsonBytes = zipReader.readEntry(BackupConstants.DATA_JSON)
                ?: return Result.Failure("Plik nie jest prawidłową kopią zapasową (brak data.json)")

            val backup: BackupFile = try {
                json.decodeFromString(BackupFile.serializer(), jsonBytes.decodeToString())
            } catch (t: Throwable) {
                return Result.Failure("Nieprawidłowy format data.json: ${t.message}")
            }

            
            
            if (backup.version < BackupConstants.MIN_SUPPORTED_VERSION ||
                backup.version > BackupConstants.CURRENT_VERSION) {
                return Result.Failure(
                    "Nieobsługiwana wersja kopii zapasowej (${backup.version}). Obsługiwane wersje: " +
                            "${BackupConstants.MIN_SUPPORTED_VERSION}-${BackupConstants.CURRENT_VERSION}."
                )
            }

            
            var imagesRestored = 0
            var imagesMissing = 0
            val idMap = mutableMapOf<String, String>()
            for (coll in backup.collections) {
                val pendingBanner = extractToCache(coll.bannerFileName)
                if (coll.bannerFileName != null) {
                    if (pendingBanner != null) imagesRestored++ else imagesMissing++
                }

                val created = collectionRepo.create(
                    name = coll.name,
                    description = coll.description,
                    schema = coll.schema,
                    bannerAlignment = coll.bannerAlignment,
                    pendingBannerPath = pendingBanner
                )
                idMap[coll.id] = created.id
            }

            
            for (item in backup.items) {
                val newCollectionId = idMap[item.collectionId]
                    ?: continue  

                
                
                val useGallery = item.images.isNotEmpty()

                
                
                val itemPendingPath: String? = if (!useGallery) {
                    val (path, ok) = extractToCacheCounted(item.imageFileName)
                    if (item.imageFileName != null) {
                        if (ok) imagesRestored++ else imagesMissing++
                    }
                    path
                } else null

                val createdItem = itemRepo.create(
                    collectionId = newCollectionId,
                    name = item.name,
                    description = item.description,
                    imageUrl = null,
                    pendingImagePath = itemPendingPath,
                    attributes = item.attributes,
                    isFavorite = item.isFavorite
                )

                
                
                
                
                if (useGallery) {
                    var primaryToSet: String? = null
                    for (img in item.images) {
                        val (pending, ok) = extractToCacheCounted(img.fileName)
                        if (!ok || pending == null) {
                            imagesMissing++
                            continue
                        }
                        imagesRestored++
                        val newId = itemImageRepo.add(
                            itemId = createdItem.id,
                            imageUrl = null,
                            pendingImagePath = pending
                        )
                        if (img.isPrimary) primaryToSet = newId
                    }
                    primaryToSet?.let { itemImageRepo.setPrimary(it) }
                }
            }

            return Result.Success(
                collectionsImported = backup.collections.size,
                itemsImported = backup.items.count { idMap.containsKey(it.collectionId) },
                imagesRestored = imagesRestored,
                imagesMissing = imagesMissing
            )
        } finally {
            zipReader.close()
        }
    }

    





    private fun extractToCacheCounted(fileName: String?): Pair<String?, Boolean> {
        if (fileName == null) return null to false
        val entryPath = "${BackupConstants.IMAGES_DIR}/$fileName"
        val bytes = zipReader.readEntry(entryPath) ?: return null to false
        val target = importPaths.newImageFile()
        return try {
            fileSystem.write(target.toPath()) { write(bytes) }
            target to true
        } catch (_: Throwable) {
            null to false
        }
    }

    
    private fun extractToCache(fileName: String?): String? = extractToCacheCounted(fileName).first
}
