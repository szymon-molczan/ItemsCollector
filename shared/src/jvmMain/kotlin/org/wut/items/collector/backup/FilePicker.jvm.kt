package org.wut.items.collector.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.UUID





actual class FilePicker {
    private val importsDir: File by lazy {
        File(System.getProperty("user.home"), ".itemscollector/imports").apply { mkdirs() }
    }

    actual suspend fun pickZip(): String? = withContext(Dispatchers.IO) {
        val dialog = FileDialog(null as Frame?, "Wybierz plik ZIP", FileDialog.LOAD)
        dialog.setFilenameFilter { _, name -> name.lowercase().endsWith(".zip") }
        dialog.file = "*.zip"
        dialog.isVisible = true

        val dir = dialog.directory ?: return@withContext null
        val file = dialog.file ?: return@withContext null
        val source = File(dir, file)
        if (!source.exists()) return@withContext null

        val dest = File(importsDir, "import_${UUID.randomUUID()}.zip")
        source.copyTo(dest, overwrite = true)
        dest.absolutePath
    }
}
