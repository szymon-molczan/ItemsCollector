package org.wut.items.collector.backup

import android.content.Context
import java.io.File





actual class BackupPaths(private val context: Context) {
    actual fun newBackupFile(): String {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val ts = System.currentTimeMillis()
        return File(dir, "items_collector_backup_$ts.zip").absolutePath
    }
}
