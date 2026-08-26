package org.wut.items.collector.service

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.wut.items.collector.db.Collections
import org.wut.items.collector.db.Items
import org.wut.items.collector.model.AttributeValue
import org.wut.items.collector.model.CreateItemRequest
import org.wut.items.collector.model.ItemDto
import org.wut.items.collector.model.UpdateItemRequest
import java.util.UUID

class ItemService {

    private val attributeValueSerializer = ListSerializer(AttributeValue.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    
    private fun guardOwnership(ownerId: String, collectionId: String): String? = transaction {
        Collections.selectAll()
            .where { (Collections.id eq collectionId) and (Collections.ownerId eq ownerId) }
            .firstOrNull()?.get(Collections.id)
    }

    fun list(ownerId: String, collectionId: String): List<ItemDto>? {
        guardOwnership(ownerId, collectionId) ?: return null
        return transaction {
            Items.selectAll()
                .where { Items.collectionId eq collectionId }
                .orderBy(Items.updatedAt to SortOrder.DESC)
                .map { it.toDto() }
        }
    }

    fun get(ownerId: String, collectionId: String, id: String): ItemDto? {
        guardOwnership(ownerId, collectionId) ?: return null
        return transaction {
            Items.selectAll()
                .where { (Items.id eq id) and (Items.collectionId eq collectionId) }
                .firstOrNull()?.toDto()
        }
    }

    


    fun create(ownerId: String, collectionId: String, req: CreateItemRequest): ItemDto? {
        guardOwnership(ownerId, collectionId) ?: return null
        return transaction {
            val now = System.currentTimeMillis()
            val id = req.id ?: UUID.randomUUID().toString()

            val existing = Items.selectAll()
                .where { (Items.id eq id) and (Items.collectionId eq collectionId) }
                .firstOrNull()

            if (existing != null) {
                Items.update({ (Items.id eq id) and (Items.collectionId eq collectionId) }) {
                    it[Items.name] = req.name
                    it[Items.description] = req.description
                    it[Items.imageUrl] = req.imageUrl
                    it[Items.attributesJson] = json.encodeToString(attributeValueSerializer, req.attributes)
                    it[Items.isFavorite] = req.isFavorite
                    it[Items.updatedAt] = now
                }
                ItemDto(id, collectionId, req.name, req.description, req.imageUrl, req.attributes, existing[Items.createdAt], now, req.isFavorite)
            } else {
                Items.insert {
                    it[Items.id] = id
                    it[Items.collectionId] = collectionId
                    it[Items.name] = req.name
                    it[Items.description] = req.description
                    it[Items.imageUrl] = req.imageUrl
                    it[Items.attributesJson] = json.encodeToString(attributeValueSerializer, req.attributes)
                    it[Items.isFavorite] = req.isFavorite
                    it[Items.createdAt] = now
                    it[Items.updatedAt] = now
                }
                ItemDto(id, collectionId, req.name, req.description, req.imageUrl, req.attributes, now, now, req.isFavorite)
            }
        }
    }

    fun update(ownerId: String, collectionId: String, id: String, req: UpdateItemRequest): ItemDto? {
        guardOwnership(ownerId, collectionId) ?: return null
        return transaction {
            val now = System.currentTimeMillis()
            val updated = Items.update({ (Items.id eq id) and (Items.collectionId eq collectionId) }) {
                it[Items.name] = req.name
                it[Items.description] = req.description
                it[Items.imageUrl] = req.imageUrl
                it[Items.attributesJson] = json.encodeToString(attributeValueSerializer, req.attributes)
                it[Items.isFavorite] = req.isFavorite
                it[Items.updatedAt] = now
            }
            if (updated == 0) null else get(ownerId, collectionId, id)
        }
    }

    fun delete(ownerId: String, collectionId: String, id: String): Boolean {
        guardOwnership(ownerId, collectionId) ?: return false
        return transaction {
            Items.deleteWhere { (Items.id eq id) and (Items.collectionId eq collectionId) } > 0
        }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toDto(): ItemDto {
        val attrsText = this[Items.attributesJson]
        val attrs = if (attrsText.isBlank()) emptyList()
        else json.decodeFromString(attributeValueSerializer, attrsText)
        return ItemDto(
            id = this[Items.id],
            collectionId = this[Items.collectionId],
            name = this[Items.name],
            description = this[Items.description],
            imageUrl = this[Items.imageUrl],
            attributes = attrs,
            createdAt = this[Items.createdAt],
            updatedAt = this[Items.updatedAt],
            isFavorite = this[Items.isFavorite]
        )
    }
}
