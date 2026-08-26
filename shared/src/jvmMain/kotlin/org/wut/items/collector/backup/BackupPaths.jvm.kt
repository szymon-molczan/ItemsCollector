package org.wut.items.collector.backup

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale




actual class BackupPaths {
    private val exportsDir: File by lazy {
        File(System.getProperty("user.home"), ".itemscollector/exports").apply { mkdirs() }
    }

    actual fun newBackupFile(): String {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        return File(exportsDir, "backup_$ts.zip").absolutePath
    }
}
