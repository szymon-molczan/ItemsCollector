package org.wut.items.collector.db

import org.jetbrains.exposed.v1.core.Table




object Users : Table("users") {
    val id = varchar("id", 36)              
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 512)
    val passwordSalt = varchar("password_salt", 256)
    val displayName = varchar("display_name", 128)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}





object Collections : Table("collections") {
    val id = varchar("id", 36)
    val ownerId = varchar("owner_id", 36) references Users.id
    val name = varchar("name", 255)
    val description = text("description")
    val schemaJson = text("schema_json")     
    val bannerImageUrl = varchar("banner_image_url", 1024).nullable()
    val bannerAlignment = float("banner_alignment").default(0.5f)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}





object Items : Table("items") {
    val id = varchar("id", 36)
    val collectionId = varchar("collection_id", 36) references Collections.id
    val name = varchar("name", 255)
    val description = text("description")
    val imageUrl = varchar("image_url", 1024).nullable()
    val attributesJson = text("attributes_json")   
    val isFavorite = bool("is_favorite").default(false)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}









object ItemImages : Table("item_images") {
    val id = varchar("id", 36)
    val itemId = varchar("item_id", 36) references Items.id
    val imageUrl = varchar("image_url", 1024)
    val isPrimary = bool("is_primary").default(false)
    val sortOrder = integer("sort_order").default(0)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
