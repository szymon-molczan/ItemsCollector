package org.wut.items.collector.backup

import android.content.Context
import java.io.File
import java.util.UUID





actual class ImportPaths(private val context: Context) {
    actual fun newImageFile(): String {
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        return File(dir, "import_${UUID.randomUUID()}.jpg").absolutePath
    }
}
