package org.wut.items.collector.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.FileImageOutputStream
import kotlin.math.max
import java.util.UUID









actual class MediaPicker {
    actual val canTakePhoto: Boolean = false

    private val mediaDir: File by lazy {
        File(System.getProperty("user.home"), ".itemscollector/media").apply { mkdirs() }
    }

    actual suspend fun pickFromGallery(): MediaResult? = withContext(Dispatchers.IO) {
        val dialog = imageFileDialog("Wybierz obraz")
        dialog.isVisible = true

        val dir = dialog.directory ?: return@withContext null
        val file = dialog.file ?: return@withContext null
        copyToMedia(File(dir, file))
    }

    actual suspend fun pickMultipleFromGallery(): List<MediaResult> = withContext(Dispatchers.IO) {
        val dialog = imageFileDialog("Wybierz obrazy")
        dialog.isMultipleMode = true
        dialog.isVisible = true

        dialog.files?.toList().orEmpty()
            .mapNotNull { copyToMedia(it) }
    }

    actual suspend fun optimizeForImport(
        media: MediaResult,
        options: MediaOptimizationOptions
    ): MediaResult = withContext(Dispatchers.IO) {
        val source = File(media.localPath)
        if (!source.exists()) return@withContext media

        val original = runCatching { ImageIO.read(source) }.getOrNull()
            ?: return@withContext media

        val maxDimension = options.maxDimension.coerceAtLeast(512)
        val maxSide = max(original.width, original.height)
        val target = if (maxSide > maxDimension) {
            val scale = maxDimension.toDouble() / maxSide.toDouble()
            scaleToRgb(
                original,
                (original.width * scale).toInt().coerceAtLeast(1),
                (original.height * scale).toInt().coerceAtLeast(1)
            )
        } else {
            toRgb(original)
        }

        val out = File(mediaDir, "opt_${UUID.randomUUID()}.jpg")
        val success = writeJpeg(target, out, options.jpegQuality.coerceIn(50, 95) / 100f)
        if (success && out.exists() && out.length() > 0L) {
            source.delete()
            MediaResult(out.absolutePath, media.displayName)
        } else {
            out.delete()
            media
        }
    }

    actual suspend fun takePhoto(): MediaResult? {
        return null
    }

    private fun imageFileDialog(title: String): FileDialog {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.setFilenameFilter { _, name ->
            val lower = name.lowercase()
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp") ||
                lower.endsWith(".bmp") || lower.endsWith(".gif")
        }
        
        dialog.file = "*.jpg;*.jpeg;*.png;*.webp;*.bmp;*.gif"
        return dialog
    }

    private fun copyToMedia(source: File): MediaResult? {
        if (!source.exists()) return null

        val ext = source.extension.ifBlank { "jpg" }
        val dest = File(mediaDir, "img_${UUID.randomUUID()}.$ext")
        source.copyTo(dest, overwrite = true)
        return MediaResult(dest.absolutePath, source.name)
    }

    private fun scaleToRgb(src: BufferedImage, width: Int, height: Int): BufferedImage {
        val out = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.drawImage(src, 0, 0, width, height, java.awt.Color.WHITE, null)
        } finally {
            g.dispose()
        }
        return out
    }

    private fun toRgb(src: BufferedImage): BufferedImage {
        if (src.type == BufferedImage.TYPE_INT_RGB) return src
        val out = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        try {
            g.drawImage(src, 0, 0, java.awt.Color.WHITE, null)
        } finally {
            g.dispose()
        }
        return out
    }

    private fun writeJpeg(image: BufferedImage, file: File, quality: Float): Boolean {
        val writer = ImageIO.getImageWritersByFormatName("jpg").asSequence().firstOrNull()
            ?: return false
        return try {
            FileImageOutputStream(file).use { output ->
                writer.output = output
                val params = writer.defaultWriteParam
                if (params.canWriteCompressed()) {
                    params.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    params.compressionQuality = quality
                }
                writer.write(null, IIOImage(image, null, null), params)
            }
            true
        } catch (_: Throwable) {
            false
        } finally {
            writer.dispose()
        }
    }
}
