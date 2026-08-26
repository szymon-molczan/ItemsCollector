package org.wut.items.collector.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wut.items.collector.db.AppDatabase
import org.wut.items.collector.db.Item_images
import org.wut.items.collector.model.ItemImageDto
import org.wut.items.collector.util.currentTimeMillis
import org.wut.items.collector.util.newUuid














class ItemImageRepository(private val db: AppDatabase) {

    

    
    fun observeByItem(itemId: String): Flow<List<ItemImageDto>> =
        db.itemImagesQueries.selectByItem(itemId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDto() } }

    



    fun observePrimaryUrlsForCollection(collectionId: String): Flow<Map<String, PrimaryImage>> =
        db.itemImagesQueries.selectPrimaryUrlsForCollection(collectionId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.associate { row ->
                    row.itemId to PrimaryImage(row.imageUrl, row.pendingImagePath)
                }
            }

    
    data class PrimaryImage(val imageUrl: String?, val pendingImagePath: String?)

    

    fun getById(id: String): ItemImageDto? =
        db.itemImagesQueries.selectById(id).executeAsOneOrNull()?.toDto()

    fun getPrimaryByItem(itemId: String): ItemImageDto? =
        db.itemImagesQueries.selectPrimaryByItem(itemId).executeAsOneOrNull()?.toDto()

    fun listByItem(itemId: String): List<ItemImageDto> =
        db.itemImagesQueries.selectByItem(itemId).executeAsList().map { it.toDto() }

    fun allDirty(): List<Item_images> = db.itemImagesQueries.selectAllDirty().executeAsList()

    fun allPendingImagePaths(): List<String> =
        db.itemImagesQueries.selectAllPendingImagePaths()
            .executeAsList()
            .mapNotNull { it }

    

    




    fun add(
        itemId: String,
        imageUrl: String?,
        pendingImagePath: String?
    ): String {
        val now = currentTimeMillis()
        val id = newUuid()
        val existing = db.itemImagesQueries.selectByItem(itemId).executeAsList()
        val isPrimary = if (existing.isEmpty()) 1L else 0L
        val sortOrder = existing.size.toLong()

        db.itemImagesQueries.upsert(
            id = id,
            itemId = itemId,
            imageUrl = imageUrl,
            pendingImagePath = pendingImagePath,
            isPrimary = isPrimary,
            sortOrder = sortOrder,
            createdAt = now,
            updatedAt = now,
            isDirty = 1L,
            isDeleted = 0L
        )
        return id
    }

    




    fun delete(imageId: String) {
        val image = db.itemImagesQueries.selectById(imageId).executeAsOneOrNull() ?: return
        val now = currentTimeMillis()
        db.itemImagesQueries.transaction {
            db.itemImagesQueries.markDeleted(now, imageId)
            
            if (image.isPrimary == 1L) {
                val remaining = db.itemImagesQueries.selectByItem(image.itemId).executeAsList()
                    .firstOrNull { it.id != imageId }
                if (remaining != null) {
                    db.itemImagesQueries.setPrimary(now, remaining.id)
                }
            }
        }
    }

    



    fun setPrimary(imageId: String) {
        val image = db.itemImagesQueries.selectById(imageId).executeAsOneOrNull() ?: return
        val now = currentTimeMillis()
        db.itemImagesQueries.transaction {
            db.itemImagesQueries.clearPrimaryForItem(now, image.itemId)
            db.itemImagesQueries.setPrimary(now, imageId)
        }
    }

    
    fun setUploadedImageUrl(imageId: String, url: String) {
        db.itemImagesQueries.setUploadedImageUrl(url, imageId)
    }

    fun markClean(imageId: String) = db.itemImagesQueries.markClean(imageId)

    fun deleteHard(imageId: String) = db.itemImagesQueries.deleteHard(imageId)

    fun deleteByItem(itemId: String) = db.itemImagesQueries.deleteByItem(itemId)

    



    fun replaceFromServer(dto: ItemImageDto) {
        
        
        val existingPending = db.itemImagesQueries.selectById(dto.id)
            .executeAsOneOrNull()?.pendingImagePath
        db.itemImagesQueries.upsert(
            id = dto.id,
            itemId = dto.itemId,
            imageUrl = dto.imageUrl,
            pendingImagePath = existingPending,
            isPrimary = if (dto.isPrimary) 1L else 0L,
            sortOrder = dto.sortOrder.toLong(),
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            isDirty = 0L,
            isDeleted = 0L
        )
    }

    
    fun deleteCleanNotIn(itemId: String, serverIds: Set<String>) {
        if (serverIds.isEmpty()) {
            db.itemImagesQueries.transaction {
                db.itemImagesQueries.selectByItem(itemId)
                    .executeAsList()
                    .filter { it.isDirty == 0L && it.isDeleted == 0L }
                    .forEach { db.itemImagesQueries.deleteHard(it.id) }
            }
        } else {
            db.itemImagesQueries.deleteCleanNotIn(itemId, serverIds)
        }
    }

    












    fun runOneShotMigration(): Int {
        var migrated = 0
        val collectionIds = db.itemsQueries.countByCollectionGrouped().executeAsList()
            .map { it.collectionId }
        for (cid in collectionIds) {
            val items = db.itemsQueries.selectByCollection(cid).executeAsList()
            for (item in items) {
                val hasAny = db.itemImagesQueries.existsAnyForItem(item.id).executeAsOne()
                if (hasAny) continue
                val hasPhoto = !item.imageUrl.isNullOrBlank() || !item.pendingImagePath.isNullOrBlank()
                if (!hasPhoto) continue
                val now = currentTimeMillis()
                db.itemImagesQueries.upsert(
                    id = newUuid(),
                    itemId = item.id,
                    imageUrl = item.imageUrl,
                    pendingImagePath = item.pendingImagePath,
                    isPrimary = 1L,
                    sortOrder = 0L,
                    createdAt = item.createdAt,
                    updatedAt = now,
                    
                    
                    
                    
                    isDirty = if (item.imageUrl.isNullOrBlank()) 1L else 0L,
                    isDeleted = 0L
                )
                migrated++
            }
        }
        return migrated
    }

    private fun Item_images.toDto(): ItemImageDto = ItemImageDto(
        id = id,
        itemId = itemId,
        imageUrl = imageUrl,
        pendingImagePath = pendingImagePath,
        isPrimary = isPrimary == 1L,
        sortOrder = sortOrder.toInt(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
