package org.wut.items.collector.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.wut.items.collector.db.AppDatabase
import org.wut.items.collector.model.AttributeValue
import org.wut.items.collector.model.ItemDto
import org.wut.items.collector.util.currentTimeMillis
import org.wut.items.collector.util.newUuid

class ItemRepository(private val db: AppDatabase) {

    private val attrSerializer = ListSerializer(AttributeValue.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    fun observeByCollection(collectionId: String): Flow<List<ItemDto>> =
        db.itemsQueries.selectByCollection(collectionId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDto() } }

    




    fun observeCounts(): Flow<Map<String, Int>> =
        db.itemsQueries.countByCollectionGrouped()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.associate { it.collectionId to it.cnt.toInt() } }

    fun getById(id: String): ItemDto? =
        db.itemsQueries.selectById(id).executeAsOneOrNull()?.toDto()

    fun create(
        collectionId: String,
        name: String,
        description: String,
        imageUrl: String?,
        pendingImagePath: String?,
        attributes: List<AttributeValue>,
        isFavorite: Boolean = false,
        forcedId: String? = null
    ): ItemDto {
        val now = currentTimeMillis()
        val id = forcedId ?: newUuid()
        db.itemsQueries.upsert(
            id = id,
            collectionId = collectionId,
            name = name,
            description = description,
            imageUrl = imageUrl,
            pendingImagePath = pendingImagePath,
            attributesJson = json.encodeToString(attrSerializer, attributes),
            createdAt = now,
            updatedAt = now,
            isDirty = 1L,
            isDeleted = 0L,
            isFavorite = if (isFavorite) 1L else 0L
        )
        return ItemDto(id, collectionId, name, description, imageUrl, attributes, now, now, isFavorite, pendingImagePath)
    }

    fun update(
        id: String,
        name: String,
        description: String,
        imageUrl: String?,
        pendingImagePath: String?,
        attributes: List<AttributeValue>,
        isFavorite: Boolean = false
    ) {
        val existing = db.itemsQueries.selectById(id).executeAsOneOrNull() ?: return
        val now = currentTimeMillis()

        
        
        
        
        val finalPending = if (imageUrl == null && pendingImagePath == null) {
            null
        } else {
            pendingImagePath ?: existing.pendingImagePath
        }

        db.itemsQueries.upsert(
            id = id,
            collectionId = existing.collectionId,
            name = name,
            description = description,
            imageUrl = imageUrl,
            pendingImagePath = finalPending,
            attributesJson = json.encodeToString(attrSerializer, attributes),
            createdAt = existing.createdAt,
            updatedAt = now,
            isDirty = 1L,
            isDeleted = 0L,
            isFavorite = if (isFavorite) 1L else 0L
        )
    }

    fun delete(id: String) {
        db.itemsQueries.markDeleted(currentTimeMillis(), id)
    }

    fun replaceFromServer(dto: ItemDto) {
        val existing = db.itemsQueries.selectById(dto.id).executeAsOneOrNull()

        
        
        val finalPending = if (dto.imageUrl == null) {
            null
        } else {
            existing?.pendingImagePath ?: dto.pendingImagePath
        }

        db.itemsQueries.upsert(
            id = dto.id,
            collectionId = dto.collectionId,
            name = dto.name,
            description = dto.description,
            imageUrl = dto.imageUrl,
            pendingImagePath = finalPending,
            attributesJson = json.encodeToString(attrSerializer, dto.attributes),
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            isDirty = 0L,
            isDeleted = 0L,
            isFavorite = if (dto.isFavorite) 1L else 0L
        )
    }

    fun toggleFavorite(id: String) {
        db.itemsQueries.toggleFavorite(currentTimeMillis(), id)
    }

    fun setUploadedImageUrl(id: String, url: String) = db.itemsQueries.setImageUrl(url, id)
    fun deleteHard(id: String) = db.itemsQueries.deleteHard(id)
    fun deleteByCollection(collectionId: String) = db.itemsQueries.deleteByCollection(collectionId)
    fun allDirty(): List<org.wut.items.collector.db.Items> =
        db.itemsQueries.selectAllDirty().executeAsList()

    
    fun allPendingImagePaths(): List<String> =
        db.itemsQueries.selectAllPendingImagePaths()
            .executeAsList()
            .mapNotNull { it }

    
    fun deleteCleanNotIn(collectionId: String, serverIds: Set<String>) {
        if (serverIds.isEmpty()) {
            db.itemsQueries.transaction {
                db.itemsQueries.selectByCollection(collectionId)
                    .executeAsList()
                    .filter { it.isDirty == 0L && it.isDeleted == 0L }
                    .forEach { db.itemsQueries.deleteHard(it.id) }
            }
        } else {
            db.itemsQueries.deleteCleanNotIn(collectionId, serverIds)
        }
    }

    private fun org.wut.items.collector.db.Items.toDto(): ItemDto {
        val attrs = if (attributesJson.isBlank()) emptyList()
        else json.decodeFromString(attrSerializer, attributesJson)
        return ItemDto(
            id = id,
            collectionId = collectionId,
            name = name,
            description = description,
            imageUrl = imageUrl,
            attributes = attrs,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isFavorite = isFavorite == 1L,
            pendingImagePath = pendingImagePath
        )
    }
}
