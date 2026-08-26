package org.wut.items.collector.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient










@Serializable
data class AttributeDef(
    val key: String,            
    val label: String,          
    val type: AttributeType,
    val required: Boolean = false,
    val options: List<String> = emptyList()  
)

@Serializable
enum class AttributeType {
    TEXT, NUMBER, DATE, BOOLEAN, SELECT
}





@Serializable
data class AttributeValue(
    val key: String,
    val value: String
)



@Serializable
data class CollectionDto(
    val id: String,
    val name: String,
    val description: String,
    val schema: List<AttributeDef>,
    val createdAt: Long,
    val updatedAt: Long,
    val bannerImageUrl: String? = null,
    val bannerAlignment: Float = 0.5f,
    @Transient
    val pendingBannerPath: String? = null
)

@Serializable
data class CreateCollectionRequest(
    val name: String,
    val description: String = "",
    val schema: List<AttributeDef> = emptyList(),
    





    val id: String? = null,
    val bannerImageUrl: String? = null,
    val bannerAlignment: Float = 0.5f,
    @Transient
    val pendingBannerPath: String? = null
)

@Serializable
data class UpdateCollectionRequest(
    val name: String,
    val description: String,
    val schema: List<AttributeDef>,
    val bannerImageUrl: String? = null,
    val bannerAlignment: Float = 0.5f,
    @Transient
    val pendingBannerPath: String? = null
)



@Serializable
data class ItemDto(
    val id: String,
    val collectionId: String,
    val name: String,
    val description: String,
    val imageUrl: String?,           
    val attributes: List<AttributeValue>,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
    




    @Transient
    val pendingImagePath: String? = null
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val description: String = "",
    val imageUrl: String? = null,
    val attributes: List<AttributeValue> = emptyList(),
    val isFavorite: Boolean = false,
    
    val id: String? = null
)

@Serializable
data class UpdateItemRequest(
    val name: String,
    val description: String,
    val imageUrl: String?,
    val attributes: List<AttributeValue>,
    val isFavorite: Boolean = false
)











@Serializable
data class ItemImageDto(
    val id: String,
    val itemId: String,
    val imageUrl: String?,
    val isPrimary: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    @Transient
    val pendingImagePath: String? = null
)

@Serializable
data class CreateItemImageRequest(
    val imageUrl: String,         
    val isPrimary: Boolean = false,
    val sortOrder: Int = 0,
    
    val id: String? = null
)



@Serializable
data class RegisterRequest(val email: String, val password: String, val displayName: String = "")

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val userId: String, val email: String, val displayName: String)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)



@Serializable
data class UploadResponse(val url: String)



@Serializable
data class ErrorResponse(val error: String)
