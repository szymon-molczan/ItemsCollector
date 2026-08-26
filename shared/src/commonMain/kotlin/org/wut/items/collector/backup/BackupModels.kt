package org.wut.items.collector.backup

import kotlinx.serialization.Serializable
import org.wut.items.collector.model.AttributeDef
import org.wut.items.collector.model.AttributeValue

















@Serializable
data class BackupFile(
    val version: Int = BackupConstants.CURRENT_VERSION,
    val exportedAt: Long,
    val exportedBy: String,        
    val collections: List<BackupCollection>,
    val items: List<BackupItem>
)

@Serializable
data class BackupCollection(
    val id: String,
    val name: String,
    val description: String,
    val schema: List<AttributeDef>,
    val bannerFileName: String? = null,
    val bannerAlignment: Float = 0.5f,
    val createdAt: Long,
    val updatedAt: Long
)








@Serializable
data class BackupItem(
    val id: String,
    val collectionId: String,
    val name: String,
    val description: String,
    val attributes: List<AttributeValue>,
    val isFavorite: Boolean = false,
    




    val imageFileName: String? = null,
    



    val images: List<BackupImage> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)






@Serializable
data class BackupImage(
    val id: String,
    val fileName: String,
    val isPrimary: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long
)


object BackupConstants {
    const val DATA_JSON = "data.json"
    const val IMAGES_DIR = "images"
    
    const val CURRENT_VERSION = 2
    const val MIN_SUPPORTED_VERSION = 1
}
