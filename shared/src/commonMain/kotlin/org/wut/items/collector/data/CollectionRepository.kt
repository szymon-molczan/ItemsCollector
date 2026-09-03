package org.wut.items.collector.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.wut.items.collector.db.AppDatabase
import org.wut.items.collector.model.AttributeDef
import org.wut.items.collector.model.AttributeValue
import org.wut.items.collector.model.CollectionDto
import org.wut.items.collector.util.currentTimeMillis
import org.wut.items.collector.util.newUuid






class CollectionRepository(private val db: AppDatabase) {

    private val attrSerializer = ListSerializer(AttributeDef.serializer())
    private val itemAttrSerializer = ListSerializer(AttributeValue.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    fun observeAll(): Flow<List<CollectionDto>> =
        db.collectionsQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDto() } }

    fun observeById(id: String): Flow<CollectionDto?> =
        db.collectionsQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDto() }

    fun getById(id: String): CollectionDto? =
        db.collectionsQueries.selectById(id).executeAsOneOrNull()?.toDto()

    fun create(name: String, description: String, schema: List<AttributeDef>, bannerImageUrl: String? = null, bannerAlignment: Float = 0.5f, pendingBannerPath: String? = null): CollectionDto {
        val now = nowMs()
        val id = newUuid()
        db.collectionsQueries.upsert(
            id = id,
            name = name,
            description = description,
            schemaJson = json.encodeToString(attrSerializer, schema),
            createdAt = now,
            updatedAt = now,
            isDirty = 1L,
            isDeleted = 0L,
            bannerImageUrl = bannerImageUrl,
            bannerAlignment = bannerAlignment.toDouble(),
            pendingBannerPath = pendingBannerPath
        )
        return CollectionDto(id, name, description, schema, now, now, bannerImageUrl, bannerAlignment, pendingBannerPath)
    }

    fun update(id: String, name: String, description: String, schema: List<AttributeDef>, bannerImageUrl: String? = null, bannerAlignment: Float = 0.5f, pendingBannerPath: String? = null) {
        val existing = db.collectionsQueries.selectById(id).executeAsOneOrNull() ?: return
        val now = nowMs()
        val previousSchema = if (existing.schemaJson.isBlank()) emptyList()
        else json.decodeFromString(attrSerializer, existing.schemaJson)
        val removedAttributeKeys = previousSchema.map { it.key }.toSet() - schema.map { it.key }.toSet()
        
        
        
        
        val finalPending = if (bannerImageUrl == null && pendingBannerPath == null) {
            null 
        } else {
            pendingBannerPath ?: existing.pendingBannerPath
        }

        db.collectionsQueries.transaction {
            db.collectionsQueries.upsert(
                id = id,
                name = name,
                description = description,
                schemaJson = json.encodeToString(attrSerializer, schema),
                createdAt = existing.createdAt,
                updatedAt = now,
                isDirty = 1L,
                isDeleted = 0L,
                bannerImageUrl = bannerImageUrl,
                bannerAlignment = bannerAlignment.toDouble(),
                pendingBannerPath = finalPending
            )

            if (removedAttributeKeys.isNotEmpty()) {
                removeDeletedAttributeValues(id, removedAttributeKeys, now)
            }
        }
    }

    fun delete(id: String) {
        db.collectionsQueries.markDeleted(nowMs(), id)
    }

    
    fun replaceFromServer(dto: CollectionDto) {
        val existing = db.collectionsQueries.selectById(dto.id).executeAsOneOrNull()
        
        
        
        
        val finalPending = if (dto.bannerImageUrl == null) {
            null
        } else {
            existing?.pendingBannerPath ?: dto.pendingBannerPath
        }

        db.collectionsQueries.upsert(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            schemaJson = json.encodeToString(attrSerializer, dto.schema),
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            isDirty = 0L,
            isDeleted = 0L,
            bannerImageUrl = dto.bannerImageUrl,
            bannerAlignment = dto.bannerAlignment.toDouble(),
            pendingBannerPath = finalPending
        )
    }

    fun deleteHard(id: String) = db.collectionsQueries.deleteHard(id)

    
    fun deleteCleanNotIn(serverIds: Set<String>) {
        
        
        if (serverIds.isEmpty()) {
            
            db.collectionsQueries.transaction {
                db.collectionsQueries.selectAllIncludingDirty()
                    .executeAsList()
                    .filter { it.isDirty == 0L && it.isDeleted == 0L }
                    .forEach { db.collectionsQueries.deleteHard(it.id) }
            }
        } else {
            db.collectionsQueries.deleteCleanNotIn(serverIds)
        }
    }

    fun allIncludingDirty(): List<org.wut.items.collector.db.Collections> =
        db.collectionsQueries.selectAllIncludingDirty().executeAsList()

    fun markClean(id: String) = db.collectionsQueries.markClean(id)

    fun allPendingBannerPaths(): List<String> =
        db.collectionsQueries.selectAllPendingBannerPaths()
            .executeAsList()
            .mapNotNull { it }

    private fun removeDeletedAttributeValues(collectionId: String, removedKeys: Set<String>, now: Long) {
        db.itemsQueries.selectByCollection(collectionId)
            .executeAsList()
            .forEach { item ->
                val attributes = if (item.attributesJson.isBlank()) emptyList()
                else json.decodeFromString(itemAttrSerializer, item.attributesJson)
                val remainingAttributes = attributes.filterNot { it.key in removedKeys }

                if (remainingAttributes.size == attributes.size) return@forEach

                db.itemsQueries.upsert(
                    id = item.id,
                    collectionId = item.collectionId,
                    name = item.name,
                    description = item.description,
                    imageUrl = item.imageUrl,
                    pendingImagePath = item.pendingImagePath,
                    attributesJson = json.encodeToString(itemAttrSerializer, remainingAttributes),
                    createdAt = item.createdAt,
                    updatedAt = now,
                    isDirty = 1L,
                    isDeleted = item.isDeleted,
                    isFavorite = item.isFavorite
                )
            }
    }

    private fun org.wut.items.collector.db.Collections.toDto(): CollectionDto {
        val schema = if (schemaJson.isBlank()) emptyList()
        else json.decodeFromString(attrSerializer, schemaJson)
        return CollectionDto(id, name, description, schema, createdAt, updatedAt, bannerImageUrl, bannerAlignment.toFloat(), pendingBannerPath)
    }

    private fun nowMs(): Long = currentTimeMillis()
}
