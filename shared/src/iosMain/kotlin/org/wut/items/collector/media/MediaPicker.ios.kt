package org.wut.items.collector.media





actual class MediaPicker {
    actual val canTakePhoto: Boolean = false

    actual suspend fun pickFromGallery(): MediaResult? = null
    actual suspend fun pickMultipleFromGallery(): List<MediaResult> = emptyList()
    actual suspend fun optimizeForImport(media: MediaResult, options: MediaOptimizationOptions): MediaResult = media
    actual suspend fun takePhoto(): MediaResult? = null
}
