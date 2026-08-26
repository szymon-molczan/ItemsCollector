package org.wut.items.collector.backup

import java.io.File
import java.util.UUID




actual class ImportPaths {
    private val mediaDir: File by lazy {
        File(System.getProperty("user.home"), ".itemscollector/media").apply { mkdirs() }
    }

    actual fun newImageFile(): String {
        return File(mediaDir, "imp_${UUID.randomUUID()}.jpg").absolutePath
    }
}
