package org.wut.items.collector.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import java.util.UUID













actual class MediaPicker(
    private val activity: ComponentActivity
) {
    actual val canTakePhoto: Boolean = true

    private val context: Context = activity.applicationContext

    private var pickGalleryLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var pickMultipleGalleryLauncher: ActivityResultLauncher<Array<String>>? = null
    private var takePictureLauncher: ActivityResultLauncher<Uri>? = null

    private var pendingGallery: CompletableDeferred<MediaResult?>? = null
    private var pendingMultipleGallery: CompletableDeferred<List<Uri>?>? = null
    private var pendingCamera: CompletableDeferred<MediaResult?>? = null
    private var pendingCameraUri: Uri? = null
    private var pendingCameraFile: File? = null

    


    fun register() {
        pickGalleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            val deferred = pendingGallery
            pendingGallery = null
            if (uri == null) {
                deferred?.complete(null)
                return@registerForActivityResult
            }
            
            val copied = copyUriToFiles(uri)
            deferred?.complete(copied?.let { MediaResult(it.absolutePath, cleanDisplayName(uri)) })
        }

        pickMultipleGalleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            val deferred = pendingMultipleGallery
            pendingMultipleGallery = null
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            deferred?.complete(uris)
        }

        takePictureLauncher = activity.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            val deferred = pendingCamera
            val file = pendingCameraFile
            pendingCamera = null
            pendingCameraUri = null
            pendingCameraFile = null
            if (success && file != null && file.exists() && file.length() > 0) {
                deferred?.complete(MediaResult(file.absolutePath))
            } else {
                file?.delete()
                deferred?.complete(null)
            }
        }
    }

    actual suspend fun pickFromGallery(): MediaResult? {
        val launcher = pickGalleryLauncher
            ?: error("MediaPicker.register() musi byc wywolane w MainActivity.onCreate()")
        val deferred = CompletableDeferred<MediaResult?>()
        pendingGallery = deferred
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
        return deferred.await()
    }

    actual suspend fun pickMultipleFromGallery(): List<MediaResult> {
        val launcher = pickMultipleGalleryLauncher
            ?: error("MediaPicker.register() musi byc wywolane w MainActivity.onCreate()")
        val deferred = CompletableDeferred<List<Uri>?>()
        pendingMultipleGallery = deferred
        launcher.launch(arrayOf("image/*"))
        val uris = deferred.await().orEmpty()
        if (uris.isEmpty()) return emptyList()

        return withContext(Dispatchers.IO) {
            uris.mapNotNull { uri ->
                val copied = copyUriToFiles(uri)
                copied?.let { MediaResult(it.absolutePath, cleanDisplayName(uri)) }
            }
        }
    }

    actual suspend fun optimizeForImport(
        media: MediaResult,
        options: MediaOptimizationOptions
    ): MediaResult = withContext(Dispatchers.IO) {
        val source = File(media.localPath)
        if (!source.exists()) return@withContext media

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext media

        var sample = 1
        val maxDimension = options.maxDimension.coerceAtLeast(512)
        while (bounds.outWidth / sample > maxDimension * 2 || bounds.outHeight / sample > maxDimension * 2) {
            sample *= 2
        }

        val decoded = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        ) ?: return@withContext media

        val maxSide = max(decoded.width, decoded.height)
        val finalBitmap = if (maxSide > maxDimension) {
            val scale = maxDimension.toFloat() / maxSide.toFloat()
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }

        val out = File(source.parentFile ?: File(context.filesDir, "media"), "opt_${UUID.randomUUID()}.jpg")
        val success = FileOutputStream(out).use { output ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, options.jpegQuality.coerceIn(50, 95), output)
        }

        if (finalBitmap !== decoded) finalBitmap.recycle()
        decoded.recycle()

        if (success && out.exists() && out.length() > 0L) {
            source.delete()
            MediaResult(out.absolutePath, media.displayName)
        } else {
            out.delete()
            media
        }
    }

    actual suspend fun takePhoto(): MediaResult? {
        val launcher = takePictureLauncher
            ?: error("MediaPicker.register() musi byc wywolane w MainActivity.onCreate()")
        
        val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
        val file = File(mediaDir, "cam_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val deferred = CompletableDeferred<MediaResult?>()
        pendingCamera = deferred
        pendingCameraUri = uri
        pendingCameraFile = file
        launcher.launch(uri)
        return deferred.await()
    }

    private fun displayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
    }

    private fun cleanDisplayName(uri: Uri): String? {
        val name = displayName(uri)?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return name.takeUnless { it.matches(Regex("\\d{8,}")) }
    }

    private fun copyUriToFiles(uri: Uri): File? {
        return try {
            val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
            val ext = guessExtension(uri) ?: "jpg"
            val out = File(mediaDir, "img_${UUID.randomUUID()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output ->
                    input.copyTo(output)
                }
            }
            if (out.exists() && out.length() > 0) out else null
        } catch (t: Throwable) {
            null
        }
    }

    private fun guessExtension(uri: Uri): String? {
        val mime = context.contentResolver.getType(uri) ?: return null
        return when {
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> null
        }
    }
}
