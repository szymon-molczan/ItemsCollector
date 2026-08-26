package org.wut.items.collector.media












expect class MediaPicker {
    


    val canTakePhoto: Boolean

    



    suspend fun pickFromGallery(): MediaResult?

    


    suspend fun pickMultipleFromGallery(): List<MediaResult>

    



    suspend fun optimizeForImport(media: MediaResult, options: MediaOptimizationOptions): MediaResult

    


    suspend fun takePhoto(): MediaResult?
}







data class MediaResult(
    val localPath: String,
    val displayName: String? = null
)

data class MediaOptimizationOptions(
    val maxDimension: Int = 2000,
    val jpegQuality: Int = 85
)
