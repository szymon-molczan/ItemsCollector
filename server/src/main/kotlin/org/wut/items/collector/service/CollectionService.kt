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
import org.wut.items.collector.model.AttributeDef
import org.wut.items.collector.model.CollectionDto
import org.wut.items.collector.model.CreateCollectionRequest
import org.wut.items.collector.model.UpdateCollectionRequest
import java.util.UUID

class CollectionService {

    private val attributeDefSerializer = ListSerializer(AttributeDef.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    fun list(ownerId: String): List<CollectionDto> = transaction {
        Collections.selectAll()
            .where { Collections.ownerId eq ownerId }
            .orderBy(Collections.updatedAt to SortOrder.DESC)
            .map { it.toDto() }
    }

    fun get(ownerId: String, id: String): CollectionDto? = transaction {
        Collections.selectAll()
            .where { (Collections.id eq id) and (Collections.ownerId eq ownerId) }
            .firstOrNull()
            ?.toDto()
    }

    








    fun create(ownerId: String, req: CreateCollectionRequest): CollectionDto = transaction {
        val now = System.currentTimeMillis()
        val id = req.id ?: UUID.randomUUID().toString()

        val existing = Collections.selectAll()
            .where { (Collections.id eq id) and (Collections.ownerId eq ownerId) }
            .firstOrNull()

        if (existing != null) {
            
            Collections.update({ (Collections.id eq id) and (Collections.ownerId eq ownerId) }) {
                it[Collections.name] = req.name
                it[Collections.description] = req.description
                it[Collections.schemaJson] = json.encodeToString(attributeDefSerializer, req.schema)
                it[Collections.bannerImageUrl] = req.bannerImageUrl
                it[Collections.bannerAlignment] = req.bannerAlignment
                it[Collections.updatedAt] = now
            }
            CollectionDto(
                id, req.name, req.description, req.schema, existing[Collections.createdAt], now,
                req.bannerImageUrl, req.bannerAlignment
            )
        } else {
            Collections.insert {
                it[Collections.id] = id
                it[Collections.ownerId] = ownerId
                it[Collections.name] = req.name
                it[Collections.description] = req.description
                it[Collections.schemaJson] = json.encodeToString(attributeDefSerializer, req.schema)
                it[Collections.bannerImageUrl] = req.bannerImageUrl
                it[Collections.bannerAlignment] = req.bannerAlignment
                it[Collections.createdAt] = now
                it[Collections.updatedAt] = now
            }
            CollectionDto(id, req.name, req.description, req.schema, now, now, req.bannerImageUrl, req.bannerAlignment)
        }
    }

    fun update(ownerId: String, id: String, req: UpdateCollectionRequest): CollectionDto? = transaction {
        val now = System.currentTimeMillis()
        val updated = Collections.update({ (Collections.id eq id) and (Collections.ownerId eq ownerId) }) {
            it[Collections.name] = req.name
            it[Collections.description] = req.description
            it[Collections.schemaJson] = json.encodeToString(attributeDefSerializer, req.schema)
            it[Collections.bannerImageUrl] = req.bannerImageUrl
            it[Collections.bannerAlignment] = req.bannerAlignment
            it[Collections.updatedAt] = now
        }
        if (updated == 0) null else get(ownerId, id)
    }

    fun delete(ownerId: String, id: String): Boolean = transaction {
        
        Items.deleteWhere { Items.collectionId eq id }
        Collections.deleteWhere { (Collections.id eq id) and (Collections.ownerId eq ownerId) } > 0
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toDto(): CollectionDto {
        val schemaText = this[Collections.schemaJson]
        val schema = if (schemaText.isBlank()) emptyList()
        else json.decodeFromString(attributeDefSerializer, schemaText)
        return CollectionDto(
            id = this[Collections.id],
            name = this[Collections.name],
            description = this[Collections.description],
            schema = schema,
            createdAt = this[Collections.createdAt],
            updatedAt = this[Collections.updatedAt],
            bannerImageUrl = this[Collections.bannerImageUrl],
            bannerAlignment = this[Collections.bannerAlignment]
        )
    }
}
