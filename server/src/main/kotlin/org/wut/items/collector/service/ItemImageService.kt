package org.wut.items.collector.service

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.wut.items.collector.db.Collections
import org.wut.items.collector.db.ItemImages
import org.wut.items.collector.db.Items
import org.wut.items.collector.model.CreateItemImageRequest
import org.wut.items.collector.model.ItemImageDto
import java.util.UUID












class ItemImageService {

    
    private fun guardOwnership(ownerId: String, collectionId: String, itemId: String): Boolean = transaction {
        val itemRow = Items.selectAll()
            .where { (Items.id eq itemId) and (Items.collectionId eq collectionId) }
            .firstOrNull() ?: return@transaction false
        Collections.selectAll()
            .where { (Collections.id eq collectionId) and (Collections.ownerId eq ownerId) }
            .firstOrNull() != null
    }

    fun list(ownerId: String, collectionId: String, itemId: String): List<ItemImageDto>? {
        if (!guardOwnership(ownerId, collectionId, itemId)) return null
        return transaction {
            ItemImages.selectAll()
                .where { ItemImages.itemId eq itemId }
                .orderBy(
                    ItemImages.isPrimary to SortOrder.DESC,
                    ItemImages.sortOrder to SortOrder.ASC,
                    ItemImages.createdAt to SortOrder.ASC
                )
                .map { it.toDto() }
        }
    }

    









    fun create(
        ownerId: String,
        collectionId: String,
        itemId: String,
        req: CreateItemImageRequest
    ): ItemImageDto? {
        if (!guardOwnership(ownerId, collectionId, itemId)) return null
        return transaction {
            val now = System.currentTimeMillis()
            val id = req.id ?: UUID.randomUUID().toString()

            val existingForItem = ItemImages.selectAll()
                .where { ItemImages.itemId eq itemId }
                .toList()
            val isFirst = existingForItem.none { it[ItemImages.id] != id }
            val effectivePrimary = isFirst || req.isPrimary

            
            if (effectivePrimary) {
                ItemImages.update({ (ItemImages.itemId eq itemId) and (ItemImages.id neq id) }) {
                    it[ItemImages.isPrimary] = false
                    it[ItemImages.updatedAt] = now
                }
            }

            val existingRow = existingForItem.firstOrNull { it[ItemImages.id] == id }
            if (existingRow != null) {
                ItemImages.update({ ItemImages.id eq id }) {
                    it[ItemImages.imageUrl] = req.imageUrl
                    it[ItemImages.isPrimary] = effectivePrimary
                    it[ItemImages.sortOrder] = req.sortOrder
                    it[ItemImages.updatedAt] = now
                }
                ItemImageDto(
                    id = id,
                    itemId = itemId,
                    imageUrl = req.imageUrl,
                    isPrimary = effectivePrimary,
                    sortOrder = req.sortOrder,
                    createdAt = existingRow[ItemImages.createdAt],
                    updatedAt = now
                )
            } else {
                ItemImages.insert {
                    it[ItemImages.id] = id
                    it[ItemImages.itemId] = itemId
                    it[ItemImages.imageUrl] = req.imageUrl
                    it[ItemImages.isPrimary] = effectivePrimary
                    it[ItemImages.sortOrder] = req.sortOrder
                    it[ItemImages.createdAt] = now
                    it[ItemImages.updatedAt] = now
                }
                ItemImageDto(
                    id = id,
                    itemId = itemId,
                    imageUrl = req.imageUrl,
                    isPrimary = effectivePrimary,
                    sortOrder = req.sortOrder,
                    createdAt = now,
                    updatedAt = now
                )
            }
        }
    }

    fun delete(ownerId: String, collectionId: String, itemId: String, id: String): Boolean {
        if (!guardOwnership(ownerId, collectionId, itemId)) return false
        return transaction {
            val row = ItemImages.selectAll()
                .where { (ItemImages.id eq id) and (ItemImages.itemId eq itemId) }
                .firstOrNull() ?: return@transaction false
            val wasPrimary = row[ItemImages.isPrimary]

            ItemImages.deleteWhere { (ItemImages.id eq id) and (ItemImages.itemId eq itemId) }

            
            if (wasPrimary) {
                val next = ItemImages.selectAll()
                    .where { ItemImages.itemId eq itemId }
                    .orderBy(ItemImages.sortOrder to SortOrder.ASC, ItemImages.createdAt to SortOrder.ASC)
                    .firstOrNull()
                if (next != null) {
                    val now = System.currentTimeMillis()
                    ItemImages.update({ ItemImages.id eq next[ItemImages.id] }) {
                        it[ItemImages.isPrimary] = true
                        it[ItemImages.updatedAt] = now
                    }
                }
            }
            true
        }
    }

    fun setPrimary(ownerId: String, collectionId: String, itemId: String, id: String): ItemImageDto? {
        if (!guardOwnership(ownerId, collectionId, itemId)) return null
        return transaction {
            val now = System.currentTimeMillis()
            val updated = ItemImages.update({ (ItemImages.id eq id) and (ItemImages.itemId eq itemId) }) {
                it[ItemImages.isPrimary] = true
                it[ItemImages.updatedAt] = now
            }
            if (updated == 0) return@transaction null
            
            ItemImages.update({ (ItemImages.itemId eq itemId) and (ItemImages.id neq id) }) {
                it[ItemImages.isPrimary] = false
                it[ItemImages.updatedAt] = now
            }
            ItemImages.selectAll()
                .where { ItemImages.id eq id }
                .first()
                .toDto()
        }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toDto(): ItemImageDto = ItemImageDto(
        id = this[ItemImages.id],
        itemId = this[ItemImages.itemId],
        imageUrl = this[ItemImages.imageUrl],
        isPrimary = this[ItemImages.isPrimary],
        sortOrder = this[ItemImages.sortOrder],
        createdAt = this[ItemImages.createdAt],
        updatedAt = this[ItemImages.updatedAt]
    )
}
